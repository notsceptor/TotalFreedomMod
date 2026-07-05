package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetAddress;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Unbans an IP address.", usage = "/<command> <ip>")
public class Command_unbanip extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<ip:IP>")
    public boolean unbanIP(CommandContext ctx, InetAddress ip)
    {
        if (!plugin.bm.isIpBanned(ip.getHostAddress()))
        {
            msg(ctx.getSender(), "That IP address is not banned.");
            return true;
        }

        FUtil.adminAction(ctx.getSender().getName(), "Unbanning IP " + FUtil.sanitizeIp(ctx.getSender(), ip.getHostAddress()), true);
        plugin.bm.unbanIp(ip.getHostAddress());

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
