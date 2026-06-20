package me.totalfreedom.totalfreedommod.blocking.sign;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.ComponentScanner;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitTask;

public class SignValidator extends FreedomService
{

    private static final int LINES_PER_SIDE = 4;
    private static final int MAX_COMPONENT_NODES = 1024;

    private BukkitTask sweepTask;

    public SignValidator(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        sweepLoadedChunks();
        scheduleProactiveSweep();
    }

    @Override
    protected void onStop()
    {
        if (sweepTask != null)
        {
            sweepTask.cancel();
            sweepTask = null;
        }
    }

     * comes near or joins. Removing them at the source keeps the chunk data sent
     * to new joiners clean; the outbound packet guard covers the brief window
     * before the next sweep.
     */
    private void scheduleProactiveSweep()
    {
        if (!sweepActive())
        {
            return;
        }
        long ticks = ConfigEntry.CRASH_SIGNS_SWEEP_TICKS.getInteger();
        if (ticks <= 0)
        {
            return;
        }
        sweepTask = Bukkit.getScheduler().runTaskTimer(plugin, this::sweepAroundPlayers, ticks, ticks);
    }

    private void sweepAroundPlayers()
    {
        if (!sweepActive())
        {
            return;
        }
        int radius = Math.max(0, ConfigEntry.CRASH_SIGNS_SWEEP_RADIUS.getInteger());
        Set<String> visited = new HashSet<>();
        int removed = 0;
        for (Player player : Bukkit.getOnlinePlayers())
        {
            World world = player.getWorld();
            Chunk center = player.getLocation().getChunk();
            int baseX = center.getX();
            int baseZ = center.getZ();
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    int cx = baseX + dx;
                    int cz = baseZ + dz;
                    if (!world.isChunkLoaded(cx, cz))
                    {
                        continue;
                    }
                    if (!visited.add(world.getUID() + ":" + cx + ":" + cz))
                    {
                        continue;
                    }
                    removed += sweepChunk(world.getChunkAt(cx, cz), "periodic sweep");
                }
            }
        }
        if (removed > 0)
        {
            FLog.warning("[SignValidator] Periodic sweep removed " + removed + " cursed sign(s).");
        }
    }

    private void sweepLoadedChunks()
    {
        if (!sweepActive())
        {
            return;
        }
        int chunks = 0;
        int removed = 0;
        for (World world : Bukkit.getWorlds())
        {
            for (Chunk chunk : world.getLoadedChunks())
            {
                chunks++;
                removed += sweepChunk(chunk, "startup sweep");
            }
        }
        if (removed > 0)
        {
            FLog.warning("[SignValidator] Startup sweep removed " + removed
                    + " cursed sign(s) across " + chunks + " loaded chunk(s).");
        }
    }

    private boolean enabled()
    {
        return Boolean.TRUE.equals(ConfigEntry.CRASH_SIGNS_PREVENT.getBoolean());
    }

    private boolean signPlacementBlocked()
    {
        return Boolean.FALSE.equals(ConfigEntry.ALLOW_SIGN_PLACE.getBoolean());
    }

    private boolean sweepActive()
    {
        return enabled() || signPlacementBlocked();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event)
    {
        if (signPlacementBlocked())
        {
            event.setCancelled(true);
            removeSign(event.getBlock());
            return;
        }

        if (!enabled())
        {
            return;
        }
        boolean cursed = false;
        for (int i = 0; i < LINES_PER_SIDE; i++)
        {
            if (ComponentScanner.isUnsafe(event.line(i), MAX_COMPONENT_NODES))
            {
                cursed = true;
                break;
            }
        }
        if (cursed)
        {
            event.setCancelled(true);
            Block block = event.getBlock();
            FUtil.playerMsg(event.getPlayer(),
                    "One or more sign lines contained malicious component data; the sign was removed.",
                    NamedTextColor.RED);
            FLog.warning("[SignValidator] Removed cursed sign edit by " + event.getPlayer().getName()
                    + " at " + FUtil.formatLocation(block.getLocation()));
            Bukkit.getScheduler().runTask(plugin, () -> removeSign(block));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (!enabled())
        {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null)
        {
            return;
        }
        Sign sign = asSign(block);
        if (sign == null)
        {
            return;
        }
        if (scanSign(sign))
        {
            event.setCancelled(true);
            removeSign(block);
            FUtil.playerMsg(event.getPlayer(), "That sign was cursed; it has been removed.", NamedTextColor.RED);
            FLog.warning("[SignValidator] Cursed sign interacted with at "
                    + FUtil.formatLocation(block.getLocation()) + " — removed in place.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if (!enabled())
        {
            return;
        }
        Block placed = event.getBlockPlaced();
        Sign sign = asSign(placed);
        if (sign == null)
        {
            return;
        }
        if (scanSign(sign))
        {
            event.setCancelled(true);
            FUtil.playerMsg(event.getPlayer(), "That sign was cursed; it has been removed.", NamedTextColor.RED);
            FLog.warning("[SignValidator] Cursed sign placed by " + event.getPlayer().getName()
                    + " at " + FUtil.formatLocation(placed.getLocation()) + " — placement blocked and removed.");
            Bukkit.getScheduler().runTask(plugin, () -> removeSign(placed));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event)
    {
        if (!enabled())
        {
            return;
        }
        Block block = event.getBlock();
        Sign sign = asSign(block);
        if (sign == null)
        {
            return;
        }
        if (scanSign(sign))
        {
            removeSign(block);
            FLog.warning("[SignValidator] Cursed sign broken by " + event.getPlayer().getName()
                    + " at " + FUtil.formatLocation(block.getLocation()) + " — removed in place before the break.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event)
    {
        if (!sweepActive())
        {
            return;
        }
        if (!signPlacementBlocked() && !Boolean.TRUE.equals(ConfigEntry.CRASH_SIGNS_SCAN_CHUNK_LOAD.getBoolean()))
        {
            return;
        }
        if (event.isNewChunk())
        {
            return;
        }
        int removed = sweepChunk(event.getChunk(), "chunk load");
        if (removed > 0)
        {
            FLog.warning("[SignValidator] Chunk-load sweep removed " + removed + " cursed sign(s) at "
                    + event.getChunk().getX() + ", " + event.getChunk().getZ() + ".");
        }
    }

    private int sweepChunk(Chunk chunk, String context)
    {
        BlockState[] tileEntities;
        try
        {
            tileEntities = chunk.getTileEntities(false);
        }
        catch (Throwable t)
        {
            return 0;
        }
        int removed = 0;
        for (BlockState state : tileEntities)
        {
            if (!(state instanceof Sign sign))
            {
                continue;
            }
            if (signPlacementBlocked() || scanSign(sign))
            {
                Block block = sign.getBlock();
                Bukkit.getScheduler().runTask(plugin, () -> removeSign(block));
                removed++;
            }
        }
        return removed;
    }

    private Sign asSign(Block block)
    {
        BlockState state = block.getState();
        return state instanceof Sign sign ? sign : null;
    }

    private boolean scanSign(Sign sign)
    {
        return scanSide(sign.getSide(Side.FRONT)) || scanSide(sign.getSide(Side.BACK));
    }

    private boolean scanSide(SignSide side)
    {
        for (int i = 0; i < LINES_PER_SIDE; i++)
        {
            if (ComponentScanner.isUnsafe(side.line(i), MAX_COMPONENT_NODES))
            {
                return true;
            }
        }
        return false;
    }

    private void removeSign(Block block)
    {
        if (asSign(block) == null)
        {
            return;
        }
        block.setType(Material.AIR, false);
    }
}
