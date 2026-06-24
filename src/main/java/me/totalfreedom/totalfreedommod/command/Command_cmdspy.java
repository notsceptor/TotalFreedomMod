package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.admin.cmdspy")
@CommandParameters(description = "Spy on commands", usage = "/<command>", aliases = "commandspy")
public class Command_cmdspy extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {

        FPlayer playerdata = plugin.pl.getPlayer(playerSender);
        playerdata.setCommandSpy(!playerdata.cmdspyEnabled());

        final me.totalfreedom.totalfreedommod.player.PlayerData data = plugin.pl.getData(playerSender);
        data.setCommandSpy(playerdata.cmdspyEnabled());
        plugin.pl.saveAsync();

        msg("CommandSpy " + (playerdata.cmdspyEnabled() ? "enabled." : "disabled."));

        return true;
    }
}
