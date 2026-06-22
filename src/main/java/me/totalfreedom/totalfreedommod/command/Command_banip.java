package me.totalfreedom.totalfreedommod.command;

import java.util.List;
import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Bans an IP address or all known IP addresses for a player.", usage = "/<command> <player|ip> [reason]")
public class Command_banip extends FreedomCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length < 1)
        {
            return false;
        }
        String reason = args.length > 1 ? StringUtils.join(args, " ", 1, args.length) : null;
        Player player = getPlayer(args[0]);
        PlayerData data = BanCommandUtil.getData(plugin, args[0], player);
        List<String> ips = BanCommandUtil.getIps(player, data);
        if (ips.isEmpty())
        {
            ips = java.util.List.of(args[0]);
        }
        int added = 0;
        for (String ip : ips)
        {
            if (plugin.bm.getByIp(ip) == null)
            {
                Ban ban = Ban.forPlayerIp(ip, sender, null, reason);
                ban.addIp(FUtil.getFuzzyIp(ip));
                if (plugin.bm.addBan(ban))
                {
                    added++;
                }
            }
        }
        FUtil.adminAction(sender.getName(), "Banning " + added + " IP record(s) for " + args[0], true);
        if (player != null)
        {
            Ban ban = plugin.bm.getByIp(player.getAddress().getAddress().getHostAddress());
            if (ban != null)
            {
                player.kick(ban.bakeKickMessage());
            }
        }
        return true;
    }
}
