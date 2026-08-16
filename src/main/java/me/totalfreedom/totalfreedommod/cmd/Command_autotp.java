package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.LoginProcess;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.annotation.*;

@Command(name = "autotp", description = "Toggle whether or not a player is automatically teleported when they join.", usage = "/autotp <player>")
@Permission(permission = "tfm.admin.autotp")
public class Command_autotp extends FCommand
{
    @Callback
    public void autotp(CommandSender sender, OfflinePlayer target)
    {
        final boolean enabled = !plugin().services().require(LoginProcess.class).TELEPORT_ON_JOIN.removeIf(entry -> entry.equals(target.getUniqueId()));
        if (enabled)
        {
            plugin().services().require(LoginProcess.class).TELEPORT_ON_JOIN.add(target.getUniqueId());
        }

        msg(sender, "<gold><player> <aqua>will <enabled:now:no longer> be automatically teleported when they join.",
                Placeholder.unparsed("player", target.getName() != null ? target.getName() : target.getUniqueId().toString()),
                Formatter.booleanChoice("enabled", enabled));
    }
}
