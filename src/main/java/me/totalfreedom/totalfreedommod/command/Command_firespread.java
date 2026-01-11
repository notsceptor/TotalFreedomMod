package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.GameRuleHandler.GameRule;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.server.firespread")
@CommandParameters(description = "Enable/disable fire spread.", usage = "/<command> <on | off>")
public class Command_firespread extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length != 1)
        {
            return false;
        }

        boolean fireSpread = !args[0].equalsIgnoreCase("off");
        ConfigEntry.ALLOW_FIRE_SPREAD.setBoolean(fireSpread);
        plugin.gr.setGameRule(GameRule.DO_FIRE_TICK, fireSpread);

        msg("Fire spread is now " + (fireSpread ? "enabled" : "disabled") + ".");

        return true;
    }
}

