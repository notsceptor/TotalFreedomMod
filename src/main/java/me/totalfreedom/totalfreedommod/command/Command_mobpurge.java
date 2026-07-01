package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.server.mobpurge")
@CommandParameters(description = "Purge all mobs in all worlds.", usage = "/<command> [world] [chunkX chunkZ | batchSize]", aliases = "mp")
public class Command_mobpurge extends FreedomCommand
{

    public static final int DEFAULT_BATCH_SIZE = 200;

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length >= 1)
        {
            World world = Bukkit.getWorld(args[0]);
            if (world == null)
            {
                msg("World \"" + args[0] + "\" not found.", NamedTextColor.RED);
                return true;
            }
            int batchSize = DEFAULT_BATCH_SIZE;
            Integer chunkX = null;
            Integer chunkZ = null;
            if (args.length >= 3)
            {
                try
                {
                    int cx = Integer.parseInt(args[1]);
                    int cz = Integer.parseInt(args[2]);
                    chunkX = cx;
                    chunkZ = cz;
                    if (args.length >= 4)
                    {
                        batchSize = Math.max(10, Math.min(1000, Integer.parseInt(args[3])));
                    }
                }
                catch (NumberFormatException e)
                {
                    chunkX = null;
                    chunkZ = null;
                }
            }
            else if (args.length >= 2)
            {
                try
                {
                    batchSize = Math.max(10, Math.min(1000, Integer.parseInt(args[1])));
                }
                catch (NumberFormatException e)
                {
                    msg("Invalid batch size. Using " + DEFAULT_BATCH_SIZE + ".", NamedTextColor.GRAY);
                }
            }
            if (chunkX != null && chunkZ != null)
            {
                FUtil.adminAction(sender.getName(), "Starting mob purge in world " + world.getName() + " chunk (" + chunkX + "," + chunkZ + ") (" + batchSize + " per tick)", true);
                purgeMobsBatchedForChunk(plugin, world, chunkX, chunkZ, batchSize, sender);
                msg("Batched mob purge started for chunk (" + chunkX + "," + chunkZ + "). You will be notified when done.", NamedTextColor.GRAY);
                return true;
            }
            FUtil.adminAction(sender.getName(), "Starting mob purge in world " + world.getName() + " (" + batchSize + " per tick)", true);
            purgeMobsBatched(plugin, world, batchSize, sender);
            msg("Mob purge started. You will be notified when done.", NamedTextColor.GRAY);
            return true;
        }

        FUtil.adminAction(sender.getName(), "Purging all mobs", true);
        msg(purgeMobs() + " mobs removed.");
        return true;
    }

    /**
     * Purge mobs in a single world over multiple ticks to avoid server freezes.
     * If no entities are in loaded chunks, force-loads spawn area so purge can run.
     */
    private static void purgeMobsBatched(me.totalfreedom.totalfreedommod.TotalFreedomMod plugin, World world, int batchSize, CommandSender notify)
    {
        new BukkitRunnable()
        {
            int totalRemoved = 0;
            boolean spawnLoaded = false;

            @Override
            public void run()
            {
                if (!plugin.isEnabled())
                {
                    cancel();
                    return;
                }
                World w = Bukkit.getWorld(world.getName());
                if (w == null)
                {
                    cancel();
                    sendDone(notify, world.getName(), totalRemoved, false);
                    return;
                }
                List<Entity> toRemove = new ArrayList<>(batchSize);
                for (Entity ent : w.getLivingEntities())
                {
                    if (toRemove.size() >= batchSize)
                    {
                        break;
                    }
                    if (isPurgeableMob(ent))
                    {
                        toRemove.add(ent);
                    }
                }
                if (toRemove.isEmpty() && totalRemoved == 0 && !spawnLoaded)
                {
                    loadSpawnChunks(w);
                    spawnLoaded = true;
                    return;
                }
                for (Entity e : toRemove)
                {
                    e.remove();
                    totalRemoved++;
                }
                if (toRemove.isEmpty())
                {
                    cancel();
                    sendDone(notify, world.getName(), totalRemoved, totalRemoved == 0 && spawnLoaded);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Purge mobs only in a specific chunk. Loads that chunk first (may cause brief lag if chunk has many entities), then batched purge.
     * Use to clear a single bloated chunk without wiping the world.
     */
    private static void purgeMobsBatchedForChunk(me.totalfreedom.totalfreedommod.TotalFreedomMod plugin, World world, int chunkX, int chunkZ, int batchSize, CommandSender notify)
    {
        new BukkitRunnable()
        {
            int totalRemoved = 0;

            @Override
            public void run()
            {
                if (!plugin.isEnabled())
                {
                    cancel();
                    return;
                }
                World w = Bukkit.getWorld(world.getName());
                if (w == null)
                {
                    cancel();
                    sendChunkDone(notify, world.getName(), chunkX, chunkZ, totalRemoved);
                    return;
                }
                w.getChunkAt(chunkX, chunkZ);
                List<Entity> toRemove = new ArrayList<>(batchSize);
                for (Entity ent : w.getLivingEntities())
                {
                    if (toRemove.size() >= batchSize)
                    {
                        break;
                    }
                    if (!isPurgeableMob(ent))
                    {
                        continue;
                    }
                    int ex = ent.getLocation().getBlockX() >> 4;
                    int ez = ent.getLocation().getBlockZ() >> 4;
                    if (ex == chunkX && ez == chunkZ)
                    {
                        toRemove.add(ent);
                    }
                }
                for (Entity e : toRemove)
                {
                    e.remove();
                    totalRemoved++;
                }
                if (toRemove.isEmpty())
                {
                    cancel();
                    sendChunkDone(notify, world.getName(), chunkX, chunkZ, totalRemoved);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static void sendChunkDone(CommandSender notify, String worldName, int chunkX, int chunkZ, int total)
    {
        if (notify != null)
        {
            notify.sendMessage(Component.text("[MobPurge] World \"" + worldName + "\" chunk (" + chunkX + "," + chunkZ + "): " + total + " mobs removed.", NamedTextColor.GRAY));
        }
    }

    private static void loadSpawnChunks(World w)
    {
        int cx = w.getSpawnLocation().getBlockX() >> 4;
        int cz = w.getSpawnLocation().getBlockZ() >> 4;
        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dz = -1; dz <= 1; dz++)
            {
                w.getChunkAt(cx + dx, cz + dz);
            }
        }
    }

    private static void sendDone(CommandSender notify, String worldName, int total, boolean suggestReload)
    {
        if (notify != null)
        {
            notify.sendMessage(Component.text("[MobPurge] World \"" + worldName + "\": " + total + " mobs removed.", NamedTextColor.GRAY));
            if (suggestReload)
            {
                notify.sendMessage(Component.text("[MobPurge] No entities in loaded chunks. Run again after spawn area loaded, or have a player enter the world so more chunks load, then run again.", NamedTextColor.GRAY));
            }
        }
    }

    public static boolean isPurgeableMob(Entity ent)
    {
        return ent instanceof Creature || ent instanceof Ghast || ent instanceof Slime
                || ent instanceof EnderDragon || ent instanceof Ambient;
    }

    public static int purgeMobs()
    {
        int removed = 0;
        for (World world : Bukkit.getWorlds())
        {
            for (Entity ent : world.getLivingEntities())
            {
                if (isPurgeableMob(ent))
                {
                    ent.remove();
                    removed++;
                }
            }
        }

        return removed;
    }
}
