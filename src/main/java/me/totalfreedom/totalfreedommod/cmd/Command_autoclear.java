package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;

@Command(name = "autoclear", description = "Toggle whether or not a player has their inventory automatically cleared when they join.", usage = "/autoclear <player>")
@Permission(permission = "tfm.admin.autoclear", level = Rank.SUPER_ADMIN)
public class Command_autoclear extends FCommand {

    @Callback
    public void autoclear(CommandSender sender, Player target)
    {
        final boolean enabled = plugin.lp.CLEAR_ON_JOIN.removeIf(entry -> entry.equalsIgnoreCase(target.getName()))
                || !plugin.lp.CLEAR_ON_JOIN.add(target.getName());

        msg(sender, "<gold>%s <aqua>%s have their inventory cleared when they join.", (enabled ? "will no longer" : "will now"));
    }
    
}
