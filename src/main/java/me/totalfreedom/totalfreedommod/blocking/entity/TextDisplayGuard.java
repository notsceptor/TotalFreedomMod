package me.totalfreedom.totalfreedommod.blocking.entity;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import java.util.List;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.ComponentScanner;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Removes certain text display entities that can be used to crash clients.
 */
public class TextDisplayGuard extends FreedomService
{

    private static final long LOG_INTERVAL_TICKS = 100L;
    private static final float MAX_VIEW_RANGE = 64.0f;
    private static final int MAX_TEXT_LENGTH = 512;
    private static final float MAX_INTERACTION_SIZE = 16.0f;

    private int sweepTaskId = -1;

    private long lastSummaryTick = 0L;
    private long detectionsSinceLastSummary = 0L;
    private String sampleContext = null;

    public TextDisplayGuard(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        schedulePeriodicSweep();
        server.getScheduler().runTaskLater(plugin, this::sweepAllWorlds, 1L);
        long ticks = ConfigEntry.CRASH_ENTITIES_SWEEP_TICKS.getInteger();
        FLog.info("[TextDisplayGuard] active"
                + " [max_text_length=" + MAX_TEXT_LENGTH + "]"
                + " [max_view_range=" + MAX_VIEW_RANGE + "]"
                + " [max_interaction_size=" + MAX_INTERACTION_SIZE + "]"
                + " [periodic sweep every " + ticks + "t]");
    }

    @Override
    protected void onStop()
    {
        if (sweepTaskId != -1)
        {
            server.getScheduler().cancelTask(sweepTaskId);
            sweepTaskId = -1;
        }
    }

    private boolean enabled()
    {
        return Boolean.TRUE.equals(ConfigEntry.CRASH_ENTITIES_PREVENT.getBoolean());
    }

    private int maxComponentNodes()
    {
        int v = ConfigEntry.CRASH_ENTITIES_MAX_COMPONENT_NODES.getInteger();
        return v > 0 ? v : 1024;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event)
    {
        if (!enabled())
        {
            return;
        }
        Entity entity = event.getEntity();
        if (!isCursed(entity))
        {
            return;
        }
        event.setCancelled(true);
        recordDetection("spawn-cancel on " + entity.getType() + "@" + locShort(entity));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityAddToWorld(EntityAddToWorldEvent event)
    {
        if (!enabled())
        {
            return;
        }
        removeEntity(event.getEntity(), "addtoworld");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event)
    {
        if (!enabled())
        {
            return;
        }
        Chunk chunk = event.getChunk();
        Entity[] entities;
        try
        {
            entities = chunk.getEntities();
        }
        catch (Throwable t)
        {
            return;
        }
        for (Entity entity : entities)
        {
            removeEntity(entity, "chunkload");
        }
    }

    private void schedulePeriodicSweep()
    {
        long interval = ConfigEntry.CRASH_ENTITIES_SWEEP_TICKS.getInteger();
        if (interval <= 0)
        {
            return;
        }
        sweepTaskId = server.getScheduler()
                .runTaskTimer(plugin, this::sweepAllWorlds, interval, interval)
                .getTaskId();
    }

    private void sweepAllWorlds()
    {
        if (!enabled())
        {
            return;
        }
        for (World world : Bukkit.getWorlds())
        {
            List<Entity> entities;
            try
            {
                entities = world.getEntities();
            }
            catch (Throwable t)
            {
                continue;
            }
            for (Entity entity : entities)
            {
                removeEntity(entity, "periodic-sweep");
            }
        }
    }

    private boolean isCursed(Entity entity)
    {
        if (entity instanceof TextDisplay display)
        {
            return isCursedTextDisplay(display);
        }
        if (entity instanceof Interaction interaction)
        {
            return isOversizedInteraction(interaction);
        }
        return false;
    }

    private boolean isCursedTextDisplay(TextDisplay display)
    {
        Component text;
        try
        {
            text = display.text();
        }
        catch (Throwable t)
        {
            return false;
        }
        if (text != null)
        {
            int len = ComponentScanner.safePlainTextLength(text, maxComponentNodes());
            if (len < 0 || len > MAX_TEXT_LENGTH)
            {
                return true;
            }
        }
        try
        {
            return display.getViewRange() > MAX_VIEW_RANGE;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    private boolean isOversizedInteraction(Interaction interaction)
    {
        try
        {
            return interaction.getInteractionWidth() > MAX_INTERACTION_SIZE
                    || interaction.getInteractionHeight() > MAX_INTERACTION_SIZE;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    private void removeEntity(Entity entity, String context)
    {
        if (!isCursed(entity))
        {
            return;
        }
        try
        {
            entity.remove();
        }
        catch (Throwable ignored)
        {
            return;
        }
        recordDetection(context + " on " + entity.getType() + "@" + locShort(entity));
    }

    private static String locShort(Entity entity)
    {
        try
        {
            return entity.getWorld().getName()
                    + ":" + Math.round(entity.getLocation().getX())
                    + "," + Math.round(entity.getLocation().getY())
                    + "," + Math.round(entity.getLocation().getZ());
        }
        catch (Throwable t)
        {
            return "?";
        }
    }

    private void recordDetection(String context)
    {
        detectionsSinceLastSummary++;
        if (sampleContext == null)
        {
            sampleContext = context;
        }

        long nowTick = server.getCurrentTick();
        if (lastSummaryTick == 0L || nowTick - lastSummaryTick >= LOG_INTERVAL_TICKS)
        {
            FLog.warning("[EntityValidator] Removed " + detectionsSinceLastSummary
                    + " bad text display(s). Sample: " + sampleContext);

            lastSummaryTick = nowTick;
            detectionsSinceLastSummary = 0L;
            sampleContext = null;
        }
    }
}
