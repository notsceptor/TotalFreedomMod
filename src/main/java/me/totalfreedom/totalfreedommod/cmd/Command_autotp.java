package me.totalfreedom.totalfreedommod.cmd;

import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;

@Command(name = "autotp", description = "Toggle whether or not a player is automatically teleported when they join.", usage = "/autotp <player>")
@Permission(level = Rank.SUPER_ADMIN, permission = "tfm.admin.autotp")
public class Command_autotp extends FCommand
{
    @Callback
    public void autotp(CommandSender sender, Player target)
    {
        final boolean enabled = plugin.lp.TELEPORT_ON_JOIN.removeIf(entry -> entry.equalsIgnoreCase(target.getName()))
                || !plugin.lp.TELEPORT_ON_JOIN.add(target.getName());

        msg(sender, "<gold><player> <aqua><enabled:will no longer:will now> be automatically teleported when they join.",
                Placeholder.unparsed("player", target.getName()),
                Formatter.booleanChoice("enabled", enabled));
    }
}