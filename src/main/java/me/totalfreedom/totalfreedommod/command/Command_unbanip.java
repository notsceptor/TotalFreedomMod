package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.List;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Unbans an IP address.", usage = "/<command> <ip>")
public class Command_unbanip extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<ip:IPs>")
    public boolean unbanIP(CommandContext ctx, List<InetAddress> ips)
    {
        // So right now, this is coded to only take one IP address. In the future, this could probably be rewritten to
        //  accept multiple addresses.
        final String ip = ips.getFirst().getHostAddress();

        if (!plugin.bm.isIpBanned(ip))
        {
            msg(ctx.getSender(), "That IP address is not banned.");
            return true;
        }

        FUtil.adminAction(ctx.getSender().getName(), "Unbanning IP " + FUtil.sanitizeIp(ctx.getSender(), ip), true);
        plugin.bm.unbanIp(ip);

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
