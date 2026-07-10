package me.totalfreedom.totalfreedommod.cmd;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;

@Command(name = "ban", description = "Bans an online or previously known player and their known IP addresses.", usage = "/<command> [-s] [-nrb] <player> [reason]", aliases = {"gtfo"})
@Permission(permission = "tfm.admin.ban", level = Rank.SUPER_ADMIN)
public class Command_ban extends FCommand
{

    @Callback
    public void ban(CommandSender sender, String playerName, @Switch("s") boolean silent, @Switch("nrb") boolean noRollback, @Greedy String reason)
    {
        doBan(sender, playerName, reason, silent, noRollback);
    }

    @Callback
    public void banNoReason(CommandSender sender, String playerName, @Switch("s") boolean silent, @Switch("nrb") boolean noRollback)
    {
        doBan(sender, playerName, null, silent, noRollback);
    }

    private void doBan(CommandSender sender, String playerName, String reason, boolean silent, boolean noRollback)
    {
        Player player = getPlayer(playerName);
        PlayerData data = BanCommandUtil.getData(plugin, playerName, player);

        if (player == null && data == null)
        {
            msg(sender, "Can't find that player. Use /banname to ban an arbitrary name.");
            return;
        }

        String name = BanCommandUtil.getCanonicalName(playerName, player, data);

        if (plugin.bm.getByUsername(name) != null)
        {
            msg(sender, name + " is already banned.");
            return;
        }

        List<String> ips = BanCommandUtil.getIps(player, data);
        Ban ban = BanCommandUtil.createFullBan(name, ips, sender, FUtil.parseDateOffset("24h"), reason);

        if (!silent && player != null)
        {
            FUtil.bcastMsg(player.getName() + " has been a VERY naughty, naughty boy.", NamedTextColor.RED);
        }

        plugin.bm.addBan(ban);
        if (!silent)
        {
            FUtil.adminAction(sender.getName(), "Banning " + name, true);

            if (reason != null)
            {
                FUtil.bcastMsg("  Reason: " + reason, NamedTextColor.YELLOW);
            }

            plugin.db.sendActionMessage(sender.getName(), name, reason, ConfigEntry.DISCORD_PLAYER_BAN_MESSAGE);
        }

        if (!noRollback)
        {
            plugin.cpb.rollback(name);
        }

        if (!plugin.al.isAdmin(player))
        {
            data.setStrikes(0);
        }

        if (player != null)
        {
            try
            {
                plugin.web.undo(player, 15);
            }
            catch (NoClassDefFoundError ignored)
            {
            }

            if (!silent)
            {
                player.setOp(false);
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();

                Location targetPos = player.getLocation();
                if (targetPos.getWorld() != null)
                {
                    for (int x = -1; x <= 1; x++)
                    {
                        for (int z = -1; z <= 1; z++)
                        {
                            Location strikePos = new Location(
                                    targetPos.getWorld(),
                                    targetPos.getBlockX() + x,
                                    targetPos.getBlockY(),
                                    targetPos.getBlockZ() + z);
                            targetPos.getWorld().strikeLightning(strikePos);
                        }
                    }
                }
            }

            player.kick(ban.bakeKickMessage());
        }
    }
}
