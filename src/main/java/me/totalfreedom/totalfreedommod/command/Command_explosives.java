package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.server.explosives")
@CommandParameters(description = "Enable/disable explosives and set effect radius.", usage = "/<command> <on | off> [radius]")
public class Command_explosives extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length == 0)
        {
            return false;
        }

        if (args.length == 2)
        {
            try
            {
                ConfigEntry.EXPLOSIVE_RADIUS.setDouble(Math.max(1.0, Math.min(30.0, Double.parseDouble(args[1]))));
            }
            catch (NumberFormatException nfex)
            {
                msg(nfex.getMessage());
                return true;
            }
        }

        boolean explosives = !args[0].equalsIgnoreCase("off");
        ConfigEntry.ALLOW_EXPLOSIONS.setBoolean(explosives);

        if (explosives)
        {
            msg("Explosives are now enabled, radius set to " + ConfigEntry.EXPLOSIVE_RADIUS.getDouble() + " blocks.");
        }
        else
        {
            msg("Explosives are now disabled.");
        }

        return true;
    }
}

