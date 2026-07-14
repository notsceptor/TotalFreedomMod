package me.totalfreedom.totalfreedommod.cmd;

import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;

@Command(name = "autoclear", description = "Toggle whether or not a player has their inventory automatically cleared when they join.", usage = "/autoclear <player>")
@Permission(permission = "tfm.admin.autoclear", level = Rank.SUPER_ADMIN)
public class Command_autoclear extends FCommand {

    @Callback
    public void autoclear(CommandSender sender, OfflinePlayer target)
    {
        final boolean enabled = plugin.lp.CLEAR_ON_JOIN.removeIf(entry -> entry.equals(target.getUniqueId()));

        msg(sender, "<gold><player> <aqua> will <enabled:no longer:now> have their inventory cleared when they join.",
                Formatter.booleanChoice("enabled", enabled));
    }
    
}
