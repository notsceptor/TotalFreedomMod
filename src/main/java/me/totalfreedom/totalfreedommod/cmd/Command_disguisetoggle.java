package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;

@Command(name = "disguisetoggle", description = "Toggle the disguise plugin", usage = "/disguisetoggle", aliases = {"dtoggle"})
@Permission(permission = "tfm.admin.disguisetoggle", level = Rank.SUPER_ADMIN)
public class Command_disguisetoggle extends FCommand
{
    @Callback
    public void disguisetoggle(CommandSender sender)
    {
        if (!plugin.ldb.isPluginEnabled())
        {
            msg(sender, "<red>LibsDisguises is not enabled.");
            return;
        }

        adminAction(
                    sender, 
                    "<aqua><value:Enabling:Disabling> Disguises.", 
                    Formatter.booleanChoice("value", !plugin.ldb.isDisguisesEnabled())
                );

        if (plugin.ldb.isDisguisesEnabled())
        {
            plugin.ldb.undisguiseAll(true);
            plugin.ldb.setDisguisesEnabled(false);
        }
        else
        {
            plugin.ldb.setDisguisesEnabled(true);
        }

        msg(
            sender, 
            "Disguises are now <value:enabled:disabled>",
            Formatter.booleanChoice("value", plugin.ldb.isDisguisesEnabled())
        );
    }
}
