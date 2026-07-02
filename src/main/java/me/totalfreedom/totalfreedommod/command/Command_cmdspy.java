package me.totalfreedom.totalfreedommod.command;

import java.util.List;
import java.util.Locale;
import me.totalfreedom.totalfreedommod.player.CommandSpyMode;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.admin.cmdspy")
@CommandParameters(description = "Spy on commands", usage = "/<command> [admins | ops | all]", aliases = "commandspy")
public class Command_cmdspy extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        final FPlayer playerData = plugin.pl.getPlayer(playerSender);
        final PlayerData data = plugin.pl.getData(playerSender);

        if (args.length == 0)
        {
            final CommandSpyMode mode = playerData.cmdspyEnabled() ? CommandSpyMode.OFF : CommandSpyMode.ALL;
            playerData.setCommandSpyMode(mode);
            data.setCommandSpyMode(mode);
            plugin.pl.saveAsync();
            msg(mode == CommandSpyMode.OFF ? "CommandSpy disabled." : "CommandSpy enabled for all.");
            return true;
        }

        if (args.length != 1)
        {
            return false;
        }

        final CommandSpyMode mode = switch (args[0].toLowerCase(Locale.ROOT))
        {
            case "admins" -> CommandSpyMode.ADMINS;
            case "ops" -> CommandSpyMode.OPS;
            case "all" -> CommandSpyMode.ALL;
            default -> null;
        };

        if (mode == null)
        {
            return false;
        }

        playerData.setCommandSpyMode(mode);
        data.setCommandSpyMode(mode);
        plugin.pl.saveAsync();
        msg("CommandSpy enabled for " + mode.getName() + ".");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, String[] args)
    {
        if (args.length != 1)
        {
            return List.of();
        }

        final String partial = args[0].toLowerCase(Locale.ROOT);
        return List.of("admins", "ops", "all").stream()
                .filter(mode -> mode.startsWith(partial))
                .toList();
    }
}
