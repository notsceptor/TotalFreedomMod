package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Unbans an IP address.", usage = "/<command> <ip>")
public class Command_unbanip extends FreedomCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length != 1)
        {
            return false;
        }
        Ban ban = plugin.bm.getByIp(args[0]);
        if (ban == null)
        {
            msg("That IP address is not banned.");
            return true;
        }
        plugin.bm.removeBan(ban);
        FUtil.adminAction(sender.getName(), "Unbanning IP " + FUtil.sanitizeIp(sender, args[0]), true);
        return true;
    }
}
