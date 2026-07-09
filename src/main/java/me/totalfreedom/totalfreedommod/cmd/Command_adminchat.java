package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Greedy;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "adminchat",
    description = "AdminChat - Talk privately with other admins. Using the command by itself will toggle AdminChat on and off for all messages.",
    usage = "/<command> [message...]",
    aliases = {"o", "ac"})
@Permission(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.adminchat")
public class Command_adminchat extends FCommand
{

    @Callback
    public void toggle(CommandSender sender)
    {
        if (isConsole(sender))
        {
            msg(sender, "Only in-game players can toggle AdminChat.");
            return;
        }

        FPlayer userinfo = plugin.pl.getPlayer((Player) sender);
        userinfo.setAdminChat(!userinfo.inAdminChat());
        msg(sender, "Toggled Admin Chat " + (userinfo.inAdminChat() ? "on" : "off") + ".");
    }

    @Callback
    public void sendMessage(CommandSender sender, @Greedy String message)
    {
        plugin.cm.adminChat(sender, message);
    }
}
