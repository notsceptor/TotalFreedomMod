package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH)
@CommandParameters(description = "Enable/disable fire placement.", usage = "/<command> <on | off>")
public class Command_fireplace extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length != 1)
        {
            return false;
        }

        boolean firePlace = !args[0].equalsIgnoreCase("off");
        ConfigEntry.ALLOW_FIRE_PLACE.setBoolean(firePlace);

        msg("Fire placement is now " + (firePlace ? "enabled" : "disabled") + ".");

        return true;
    }
}

