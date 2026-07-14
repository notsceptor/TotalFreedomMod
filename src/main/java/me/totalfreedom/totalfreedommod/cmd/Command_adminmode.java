package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;

@Command(name = "adminmode", usage = "/adminmode [on | off]")
@Permission(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_CONSOLE, permission = "tfm.admin.adminmode")
public class Command_adminmode extends FCommand 
{
    @Callback
    public void query(CommandSender sender)
    {
        msg(sender, "<gray>Admins only mode is currently <status:\"<red>enabled\":\"<green>disabled\">.",
                Formatter.booleanChoice("status", ConfigEntry.ADMIN_ONLY_MODE.getBoolean()));
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
