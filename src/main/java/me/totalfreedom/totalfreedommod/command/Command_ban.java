package me.totalfreedom.totalfreedommod.command;

import java.util.List;
import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Bans an online or previously known player and their known IP addresses.", usage = "/<command> [-s] [-nrb] <player> [reason]", aliases = "gtfo")
public class Command_ban extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<playerName> <reason..>", switches = "s,nrb")
    public boolean ban(CommandContext ctx, String playerName, String reason, boolean silent, boolean noRollback)
    {
        Player player = getPlayer(playerName);
        PlayerData data = BanCommandUtil.getData(plugin, playerName, player);

        if (player == null && data == null)
        {
            msg("Can't find that player. Use /banname to ban an arbitrary name.");
            return true;
        }

        String name = BanCommandUtil.getCanonicalName(playerName, player, data);

        if (plugin.bm.getByUsername(name) != null)
        {
            msg(name + " is already banned.");
            return true;
        }

        List<String> ips = BanCommandUtil.getIps(player, data);
        Ban ban = BanCommandUtil.createFullBan(name, ips, sender, null, reason);

        if (!silent && player != null)
            FUtil.bcastMsg(player.getName() + " has been a VERY naughty, naughty boy.", NamedTextColor.RED);

        plugin.bm.addBan(ban);
        if (!silent)
            FUtil.adminAction(sender.getName(), "Banning " + name, true);

        if (!noRollback)
        {
            plugin.cpb.rollback(name);
        }

        if (!plugin.al.isAdmin(player))
            data.setStrikes(0);

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

        return true;
    }

    @CommandDispatchTarget(pattern = "<playerName>", switches = "s,nrb")
    public boolean banNoReason(CommandContext ctx, String playerName, boolean silent, boolean noRollback)
    {
        return ban(ctx, playerName, null, silent, noRollback);
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
