package me.totalfreedom.totalfreedommod.blocking.entity;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import java.util.List;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Art;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Hard cap on Slime#setSize and the SCALE attribute.
 * Prevents tick stalls in Entity#checkInsideBlocks.
 */
public class EntitySizeGuard extends FreedomService
{

    private static final long LOG_INTERVAL_TICKS = 100L;

    private int sweepTaskId = -1;

    private long lastSummaryTick = 0L;
    private long numDetectionSinceLast = 0L;
    private long maxObservedSize = 0L;
    private String dominantReason = null;
    private String sampleContext = null;

    public EntitySizeGuard(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        schedulePeriodicSweep();
        FLog.info("[EntitySizeGuard] active"
                + " [max scale=" + describeCap(ConfigEntry.CRASH_ENTITIES_MAX_SCALE.getDouble()) + "]"
                + " [max slime size=" + describeCap(ConfigEntry.CRASH_ENTITIES_MAX_SLIME_SIZE.getInteger()) + "]"
                + " [max painting size=" + describeCap(ConfigEntry.CRASH_ENTITIES_MAX_PAINTING_BLOCKS.getInteger()) + "]"
                + " [periodic sweep every " + ConfigEntry.CRASH_ENTITIES_SCALE_SWEEP_TICKS.getInteger() + "t]");
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

    private static String describeCap(double value)
    {
        return value > 0 ? String.valueOf(value) : "disabled";
    }

    private static String describeCap(int value)
    {
        return value > 0 ? String.valueOf(value) : "disabled";
    }

    private boolean clampEntity(Entity entity, String context)
    {
        if (entity == null)
        {
            return false;
        }
        boolean changed = false;

        int maxSize = ConfigEntry.CRASH_ENTITIES_MAX_SLIME_SIZE.getInteger();
        if (maxSize > 0 && entity instanceof Slime slime)
        {
            try
            {
                int size = slime.getSize();
                if (size > maxSize)
                {
                    slime.setSize(maxSize);
                    recordDetection(size, "slime-size", context + " on " + entity.getType() + "@" + locShort(entity));
                    changed = true;
                }
            }
            catch (Throwable ignored)
            {
            }
        }

        double maxScale = ConfigEntry.CRASH_ENTITIES_MAX_SCALE.getDouble();
        if (maxScale > 0.0 && entity instanceof LivingEntity le)
        {
            try
            {
                AttributeInstance ai = le.getAttribute(Attribute.SCALE);
                if (ai != null && ai.getBaseValue() > maxScale)
                {
                    long observed = Math.round(ai.getBaseValue() * 100);
                    ai.setBaseValue(maxScale);
                    recordDetection(observed, "scale-attr", context + " on " + entity.getType() + "@" + locShort(entity));
                    changed = true;
                }
            }
            catch (Throwable ignored)
            {
            }
        }

        if (entity instanceof Painting painting && clampPainting(painting, context))
        {
            changed = true;
        }

        return changed;
    }

    private boolean clampPainting(Painting painting, String context)
    {
        int maxBlocks = ConfigEntry.CRASH_ENTITIES_MAX_PAINTING_BLOCKS.getInteger();
        if (maxBlocks <= 0)
        {
            return false;
        }

        boolean corrupt = false;
        long observed = -1L;
        try
        {
            Art art = painting.getArt();
            if (art == null)
            {
                corrupt = true;
            }
            else
            {
                int w = art.getBlockWidth();
                int h = art.getBlockHeight();
                observed = Math.max(w, h);
                if (w <= 0 || h <= 0 || w > maxBlocks || h > maxBlocks)
                {
                    corrupt = true;
                }
            }
        }
        catch (Throwable t)
        {
            // getArt() could not deserialize a corrupted/unregistered variant.
            corrupt = true;
        }

        if (!corrupt)
        {
            return false;
        }

        try
        {
            painting.remove();
        }
        catch (Throwable ignored)
        {
        }
        recordDetection(observed, "painting-variant", context + " on " + painting.getType() + "@" + locShort(painting));
        return true;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityAddToWorld(EntityAddToWorldEvent event)
    {
        clampEntity(event.getEntity(), "addtoworld");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event)
    {
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
            clampEntity(entity, "chunkload");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityTransform(EntityTransformEvent event)
    {
        for (Entity transformed : event.getTransformedEntities())
        {
            clampEntity(transformed, "transform");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event)
    {
        clampEntity(event.getEntity(), "spawn");
    }

    private void schedulePeriodicSweep()
    {
        long interval = ConfigEntry.CRASH_ENTITIES_SCALE_SWEEP_TICKS.getInteger();
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
                clampEntity(entity, "periodic-sweep");
            }
        }
    }

    private void recordDetection(long observed, String reason, String context)
    {
        numDetectionSinceLast++;
        if (observed > maxObservedSize)
        {
            maxObservedSize = observed;
        }
        if (dominantReason == null)
        {
            dominantReason = reason;
            sampleContext = context;
        }

        long nowTick = server.getCurrentTick();
        if (lastSummaryTick == 0L || nowTick - lastSummaryTick >= LOG_INTERVAL_TICKS)
        {
            String summary = "[EntitySizeGuard] Detected " + numDetectionSinceLast
                    + " oversized entity/entities. Reason: " + dominantReason
                    + " | max observed: " + maxObservedSize
                    + " | sample: " + sampleContext;
            FLog.warning(summary);
            broadcastToAdmins(summary);

            lastSummaryTick = nowTick;
            numDetectionSinceLast = 0L;
            dominantReason = null;
            maxObservedSize = 0L;
            sampleContext = null;
        }
    }

    private void broadcastToAdmins(String message)
    {
        Component component = Component.text(message, NamedTextColor.RED);
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (plugin.al.isAdmin(p))
            {
                p.sendMessage(component);
            }
        }
    }
}
