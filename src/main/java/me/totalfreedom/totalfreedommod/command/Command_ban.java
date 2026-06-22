package me.totalfreedom.totalfreedommod.command;

import java.util.List;
import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Bans an online or previously known player and their known IP addresses.", usage = "/<command> <player> [reason] [-nrb]", aliases = "gtfo")
public class Command_ban extends FreedomCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length < 1)
        {
            return false;
        }

        Player player = getPlayer(args[0]);
        PlayerData data = BanCommandUtil.getData(plugin, args[0], player);

        if (player == null && data == null)
        {
            msg("Can't find that player. Use /banname to ban an arbitrary name.");
            return true;
        }

        String name = BanCommandUtil.getCanonicalName(args[0], player, data);

        if (plugin.bm.getByUsername(name) != null)
        {
            msg(name + " is already banned.");
            return true;
        }

        boolean noRollback = args.length > 1 && args[args.length - 1].equalsIgnoreCase("-nrb");
        int reasonEnd = noRollback ? args.length - 1 : args.length;

        String reason = reasonEnd > 1
                ? StringUtils.join(args, " ", 1, reasonEnd)
                : null;

        List<String> ips = BanCommandUtil.getIps(player, data);
        Ban ban = BanCommandUtil.createFullBan(name, ips, sender, null, reason);

        if (player != null)
        {
            FUtil.bcastMsg(player.getName() + " has been a VERY naughty, naughty boy.", NamedTextColor.RED);
        }

        plugin.bm.addBan(ban);
        FUtil.adminAction(sender.getName(), "Banning " + name, true);

        if (!noRollback)
        {
            plugin.cpb.rollback(name);
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

            player.kick(ban.bakeKickMessage());
        }

        return true;
    }
}
