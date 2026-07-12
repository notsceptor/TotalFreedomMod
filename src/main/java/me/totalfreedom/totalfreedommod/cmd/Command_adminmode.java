package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;

public class Command_adminmode extends FCommand 
{
    @Callback
    public void query(CommandSender sender)
    {
        msg(sender, String.format(
            "<gray>Admins only mode is currently %s<gray>.", 
            ConfigEntry.ADMIN_ONLY_MODE.getBoolean() ? "<red>enabled" : "<green>disabled"
        ));
    }
    
    @Callback
    public void setMode(CommandSender sender, Boolean value) 
    {
        adminAction(sender, value ? "<red>Closing the server to non-admins." : "<green>Opening the server to all players.");
        ConfigEntry.ADMIN_ONLY_MODE.setBoolean(value);

        if (value)
        {
            server.getOnlinePlayers().stream().filter(player -> !plugin.al.isAdmin(player)).forEach(player -> 
                    kickPlayer(player, "<red>The server is now closed to non-admins."
                ));        
        }
    }
}
