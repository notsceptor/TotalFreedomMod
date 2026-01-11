package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.server.lavaplace")
@CommandParameters(description = "Enable/disable lava placement.", usage = "/<command> <on | off>")
public class Command_lavaplace extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length != 1)
        {
            return false;
        }

        boolean lavaPlace = !args[0].equalsIgnoreCase("off");
        ConfigEntry.ALLOW_LAVA_PLACE.setBoolean(lavaPlace);

        msg("Lava placement is now " + (lavaPlace ? "enabled" : "disabled") + ".");

        return true;
    }
}

