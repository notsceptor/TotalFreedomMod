package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Greedy;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "gcmd", description = "Send a command as someone else.", usage = "/gcmd <player> <command>")
@Permission(permission = "tfm.admin.gcmd")
public class Command_gcmd extends FCommand
{
    @Callback
    public void runAsOtherPlayer(CommandSender sender, Player player, @Greedy String command)
    {
        if (plugin().cb.isCommandBlocked(command, sender))
        {
            msg(sender, "<red>You cannot run blocked commands on another player.");
            return;
        }

        if (isProtectedAdmin(sender, player))
            return;

        msg(sender, "<gray>Sending command as <yellow><player><gray>: <white><command>",
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("command", command));

        try
        {
            if (server().getCommandMap().dispatch(player, command)) 
                msg(sender, "<green>Command sent.");
            else 
                msg(sender, "<red>Unknown error sending command.");
        }
        catch (Throwable ex)
        {
            msg(sender, "<red>Error sending command: <error>", Placeholder.unparsed("error", ex.getMessage()));
        }
    }
}