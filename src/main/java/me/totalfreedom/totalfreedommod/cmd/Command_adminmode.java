package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Command_adminmode extends FCommand 
{
    @Callback
    public void query(CommandSender sender)
    {
        msg(sender, Component.text("Admins-only mode is currently ", NamedTextColor.GRAY)
                .append(ConfigEntry.ADMIN_ONLY_MODE.getBoolean() ?
                        Component.text("enabled", NamedTextColor.RED) :
                        Component.text("disabled", NamedTextColor.GREEN))
                .append(Component.text(".")));
    }
    
    @Callback
    public void setMode(CommandSender sender, Boolean value) 
    {
        FUtil.adminAction(sender.getName(), value ? "Closing the server to non-admins." : "Opening the server to all players.", true);
        ConfigEntry.ADMIN_ONLY_MODE.setBoolean(value);

        if (value)
        {
            server.getOnlinePlayers().stream().filter(player -> !plugin.al.isAdmin(player)).forEach(player -> 
                player.kick(Component.text("The server is now closed to non-admins.", NamedTextColor.RED)));
        }
    }
}
