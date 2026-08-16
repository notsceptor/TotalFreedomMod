package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.bridge.LibsDisguisesBridge;

import org.bukkit.command.CommandSender;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.annotation.*;

@Command(name = "undisguiseall", description = "Undisguise all players on the server", usage = "/undisguiseall", aliases = {"uall"})
@Permission(permission = "tfm.admin.undisguiseall")
public class Command_undisguiseall extends FCommand
{
    @Callback
    public void undisguiseall(CommandSender sender)
    {
        if (!plugin().bridges().require(LibsDisguisesBridge.class).isPluginEnabled())
        {
            msg(sender, "<gray>LibsDisguises is not enabled.");
            return;
        }

        if (!plugin().bridges().require(LibsDisguisesBridge.class).isDisguisesEnabled())
        {
            msg(sender, "<gray>Disguises are not enabled.");
            return;
        }

        adminAction(sender, "<red>Undisguising all non-admins");

        plugin().bridges().require(LibsDisguisesBridge.class).undisguiseAll(false);
    }
}
