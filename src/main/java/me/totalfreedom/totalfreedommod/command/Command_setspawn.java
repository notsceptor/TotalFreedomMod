package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.world.setspawn")
@CommandParameters(description = "Set the server spawn to your current location.", usage = "/<command>")
public class Command_setspawn extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        Location location = playerSender.getLocation();
        plugin.sm.setSpawnLocation(location);
        msg("Server spawn set to: " + FUtil.formatLocation(location));
        return true;
    }
}
