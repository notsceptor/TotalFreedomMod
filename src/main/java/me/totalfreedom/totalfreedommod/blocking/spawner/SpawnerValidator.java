package me.totalfreedom.totalfreedommod.blocking.spawner;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.entity.TrialSpawnerSpawnEvent;

public class SpawnerValidator extends FreedomService
{

    private static final long LOG_INTERVAL_TICKS = 100L;

    private long lastSummaryTick = 0L;
    private long detectionsSinceLastSummary = 0L;
    private String sampleContext = null;

    public SpawnerValidator(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        FLog.info("[SpawnerValidator] active"
                + " [prevent=" + Boolean.TRUE.equals(ConfigEntry.CRASH_SPAWNERS_PREVENT.getBoolean()) + "]");
    }

    @Override
    protected void onStop()
    {
    }

    private boolean preventActive()
    {
        return Boolean.TRUE.equals(ConfigEntry.CRASH_SPAWNERS_PREVENT.getBoolean());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event)
    {
        blockHangingFromSpawner(event, "spawner-hanging", "spawner");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTrialSpawnerSpawn(TrialSpawnerSpawnEvent event)
    {
        blockHangingFromSpawner(event, "trial-spawner-hanging", "trial-spawner");
    }

    private void blockHangingFromSpawner(EntitySpawnEvent event, String reason, String label)
    {
        if (Boolean.TRUE.equals(ConfigEntry.DISABLE_SPAWNERS.getBoolean()))
        {
            return;
        }
        if (!preventActive())
        {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof Hanging)
        {
            event.setCancelled(true);
            recordDetection(reason, label + " on " + entity.getType() + "@" + locShort(entity));
        }
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

    private void recordDetection(String reason, String context)
    {
        detectionsSinceLastSummary++;
        if (sampleContext == null)
        {
            sampleContext = context;
        }

        long nowTick = server.getCurrentTick();
        if (lastSummaryTick == 0L || nowTick - lastSummaryTick >= LOG_INTERVAL_TICKS)
        {
            String summary = "[SpawnerValidator] Blocked " + detectionsSinceLastSummary
                    + " spawner placement(s). Reason: " + reason
                    + " | sample: " + sampleContext;
            FLog.warning(summary);
            broadcastToAdmins(summary);

            lastSummaryTick = nowTick;
            detectionsSinceLastSummary = 0L;
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
