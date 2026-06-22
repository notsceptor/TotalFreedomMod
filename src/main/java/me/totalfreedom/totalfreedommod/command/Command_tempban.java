package me.totalfreedom.totalfreedommod.command;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Temporarily bans an online or previously known player.", usage = "/<command> <player> [duration] [reason] [-rb]")
public class Command_tempban extends FreedomCommand
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

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
            msg("Can't find that player. Use /banname for a name-only ban.");
            return true;
        }
        String name = BanCommandUtil.getCanonicalName(args[0], player, data);
        Date expires = FUtil.parseDateOffset("30m");
        if (args.length >= 2)
        {
            Date parsed = FUtil.parseDateOffset(args[1]);
            if (parsed != null)
            {
                expires = parsed;
            }
        }
        boolean rollback = args.length > 1 && args[args.length - 1].equalsIgnoreCase("-rb");
        int reasonEnd = rollback ? args.length - 1 : args.length;
        String reason = reasonEnd >= 3 ? StringUtils.join(args, " ", 2, reasonEnd) : "Banned by " + sender.getName();
        List<String> ips = BanCommandUtil.getIps(player, data);
        Ban ban = BanCommandUtil.createFullBan(name, ips, sender, expires, reason);
        plugin.bm.addBan(ban);
        FUtil.adminAction(sender.getName(), "Temporarily banned " + name + " until " + DATE_FORMAT.format(expires), true);
        if (rollback)
        {
            plugin.cpb.rollback(name);
        }
        if (player != null)
        {
            Location target = player.getLocation();
            for (int x = -1; x <= 1; x++)
            {
                for (int z = -1; z <= 1; z++)
                {
                    target.getWorld().strikeLightning(new Location(target.getWorld(), target.getBlockX() + x, target.getBlockY(), target.getBlockZ() + z));
                }
            }
            player.kick(Component.text("You have been temporarily banned until " + DATE_FORMAT.format(expires) + "."));
        }
        return true;
    }
}
