package me.totalfreedom.totalfreedommod.blocking.item;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Interaction between ItemPacketListener and packetevents.  This is done for some benefits in interacting
 * with player equipment/inventory packets before they reach the client (to protect against crash items).
 */
public class EquipmentPacketGuard extends FreedomService
{

    private static final long RETRY_INTERVAL_TICKS = 40L;
    private static final int MAX_ATTEMPTS = 15;

    private Object registeredListener;
    private PacketSpamLimiter spamLimiter;
    private volatile boolean stopped;

    public EquipmentPacketGuard(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        stopped = false;

        Snapshot snapshot = Snapshot.read();
        if (!snapshot.anyHookEnabled())
        {
            return;
        }

        attemptRegister(snapshot, 0);
    }

    private void attemptRegister(Snapshot snapshot, int attempt)
    {
        if (stopped || registeredListener != null)
        {
            return;
        }

        Plugin pe = server.getPluginManager().getPlugin("packetevents");
        if (pe == null)
        {
            pe = server.getPluginManager().getPlugin("PacketEvents");
        }

        if (pe != null && pe.isEnabled())
        {
            try
            {
                register(snapshot);
            }
            catch (Throwable t)
            {
                FLog.severe("[EquipmentPacketGuard] Failed to register PacketEvents listener: " + t.getMessage());
                FLog.severe(t);
                registeredListener = null;
                spamLimiter = null;
            }
            return;
        }

        if (attempt >= MAX_ATTEMPTS)
        {
            FLog.warning("[EquipmentPacketGuard] PacketEvents not present after waiting; outbound "
                    + "cursed-item filtering and inbound packet rate limiting are disabled.");
            return;
        }

        server.getScheduler().runTaskLater(plugin, () -> attemptRegister(snapshot, attempt + 1), RETRY_INTERVAL_TICKS);
    }

    private void register(Snapshot snapshot)
    {
        if (snapshot.rateLimit)
        {
            spamLimiter = new PacketSpamLimiter(snapshot.maxInteractions, snapshot.maxCommands, snapshot.maxMovement);
        }

        registeredListener = PacketEvents.getAPI().getEventManager()
                .registerListener(new ItemPacketListener(plugin, snapshot.itemGuard, spamLimiter,
                        snapshot.signGuard, snapshot.signChunkGuard, snapshot.blockAllSignPackets,
                        snapshot.gameRuleGuard));

        FLog.info("[EquipmentPacketGuard] PacketEvents hooks active"
                + (snapshot.itemGuard ? " [itemGuard]" : "")
                + (snapshot.rateLimit ? " [rateLimit]" : "")
                + (snapshot.signGuard ? " [signGuard]" : "")
                + (snapshot.signChunkGuard ? " [signChunkGuard]" : "")
                + (snapshot.blockAllSignPackets ? " [blockAllSignPackets]" : "")
                + (snapshot.gameRuleGuard ? " [gameRuleGuard]" : "")
                + ".");
    }

    private record Snapshot(
            boolean itemGuard,
            boolean rateLimit,
            boolean signGuard,
            boolean signChunkGuard,
            boolean blockAllSignPackets,
            boolean gameRuleGuard,
            int maxInteractions,
            int maxCommands,
            int maxMovement)
    {
        private static Snapshot read()
        {
            return new Snapshot(
                    Boolean.TRUE.equals(ConfigEntry.CRASH_ITEMS_PACKET_GUARD.getBoolean()),
                    Boolean.TRUE.equals(ConfigEntry.CRASH_ITEMS_PACKET_RATE_LIMIT.getBoolean()),
                    Boolean.TRUE.equals(ConfigEntry.CRASH_SIGNS_PACKET_GUARD.getBoolean()),
                    Boolean.TRUE.equals(ConfigEntry.CRASH_SIGNS_CHUNK_GUARD.getBoolean()),
                    Boolean.FALSE.equals(ConfigEntry.ALLOW_SIGN_PLACE.getBoolean()),
                    Boolean.TRUE.equals(ConfigEntry.CRASH_GAMERULES_PACKET_GUARD.getBoolean()),
                    ConfigEntry.CRASH_ITEMS_MAX_INTERACTIONS_PER_SECOND.getInteger(),
                    ConfigEntry.CRASH_ITEMS_MAX_COMMANDS_PER_SECOND.getInteger(),
                    ConfigEntry.CRASH_ITEMS_MAX_MOVEMENT_PER_SECOND.getInteger());
        }

        private boolean anyHookEnabled()
        {
            return itemGuard || rateLimit || signGuard || signChunkGuard || blockAllSignPackets || gameRuleGuard;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        if (spamLimiter != null)
        {
            spamLimiter.forget(event.getPlayer().getUniqueId());
        }
    }

    @Override
    protected void onStop()
    {
        stopped = true;

        if (spamLimiter != null)
        {
            spamLimiter.clear();
            spamLimiter = null;
        }

        if (registeredListener == null)
        {
            return;
        }
        try
        {
            PacketEvents.getAPI().getEventManager().unregisterListener((PacketListenerCommon) registeredListener);
        }
        catch (Throwable t)
        {
            FLog.warning("[EquipmentPacketGuard] Failed to unregister PacketEvents listener: " + t.getMessage());
        }
        finally
        {
            registeredListener = null;
        }
    }
}
