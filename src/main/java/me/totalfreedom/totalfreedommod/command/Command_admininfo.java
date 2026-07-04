package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.player.admininfo")
@CommandParameters(description = "Information on how to apply for admin.", usage = "/<command>", aliases = "si,ai,staffinfo")
public class Command_admininfo extends FreedomCommand
{

    @CommandDispatchTarget
    public boolean adminInformation(CommandContext ctx)
    {
        List<String> adminInfo = ConfigEntry.ADMIN_INFO.getStringList();

        if (adminInfo.isEmpty())
        {
            msg("The admin information section of the config.yml file has not been configured.", NamedTextColor.RED);
        }
        else
        {
            msg(FUtil.colorize(StringUtils.join(adminInfo, "\n")));
        }
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}