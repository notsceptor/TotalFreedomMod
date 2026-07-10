package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Command(name = "autotp", description = "Toggle whether or not a player is automatically teleported when they join.", usage = "/autotp <player>")
@Permission(level = Rank.SUPER_ADMIN, permission = "tfm.admin.autotp")
public class Command_autotp extends FCommand
{
    @Callback
    public void autotp(CommandSender sender, Player target)
    {
        final boolean enabled = plugin.lp.TELEPORT_ON_JOIN.removeIf(entry -> entry.equalsIgnoreCase(target.getName()))
                || !plugin.lp.TELEPORT_ON_JOIN.add(target.getName());

        msg(sender, Component.text(target.getName(), NamedTextColor.GOLD)
                .append(Component.text(
                        enabled
                                ? " will no longer be automatically teleported when they join."
                                : " will now be automatically teleported when they join.",
                        NamedTextColor.AQUA)));
    }
}