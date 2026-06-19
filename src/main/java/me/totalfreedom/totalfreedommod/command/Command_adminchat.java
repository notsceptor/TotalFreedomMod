package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.adminchat")
@CommandParameters(
        description = "AdminChat - Talk privately with other admins. Using <command> itself will toggle AdminChat on and off for all messages.",
        usage = "/<command> [message...]",
        aliases = "o,ac")
public class Command_adminchat extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "")
    public boolean toggle(CommandContext ctx)
    {
        if (ctx.isSenderConsole())
        {
            msg("Only in-game players can toggle AdminChat.");
            return true;
        }

        FPlayer userinfo = plugin.pl.getPlayer(ctx.getPlayerSender());
        userinfo.setAdminChat(!userinfo.inAdminChat());
        msg("Toggled Admin Chat " + (userinfo.inAdminChat() ? "on" : "off") + ".");
        return true;
    }

    @CommandDispatchTarget(pattern = "<message..>")
    public boolean sendMessage(CommandContext ctx, String message)
    {
        plugin.cm.adminChat(ctx.getSender(), message);
        return true;
    }
    
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
