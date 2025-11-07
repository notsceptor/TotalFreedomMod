package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH)
@CommandParameters(description = "Enable/disable fluid spread.", usage = "/<command> <on | off>")
public class Command_fluidspread extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length != 1)
        {
            return false;
        }

        boolean fluidSpread = !args[0].equalsIgnoreCase("off");
        ConfigEntry.ALLOW_FLUID_SPREAD.setBoolean(fluidSpread);

        msg("Lava and water spread is now " + (fluidSpread ? "enabled" : "disabled") + ".");

        return true;
    }
}

