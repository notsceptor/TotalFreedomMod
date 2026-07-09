package me.totalfreedom.totalfreedommod.cmd;

import java.util.List;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

@Command(name = "admininfo", description = "Information on how to apply for admin.", usage = "/admininfo", aliases={"si", "ai", "staffinfo"})
@Permission(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.player.admininfo")
public class Command_admininfo extends FCommand 
{
    
    @Callback
    public void info(CommandSender sender)
    {
        List<String> adminInfo = ConfigEntry.ADMIN_INFO.getStringList();
        if (adminInfo.isEmpty()) 
        {
            msg(sender, "The admin information section of the config.yml file has not been configured.", NamedTextColor.RED);
            return;
        }
        
        msg(sender, Component.join(JoinConfiguration.newlines(), adminInfo.stream().map(FUtil::colorizeWithLinks).toList()));
    }
}
