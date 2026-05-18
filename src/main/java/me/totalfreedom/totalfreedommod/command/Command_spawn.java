package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.NON_OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.player.spawn")
@CommandParameters(description = "Teleport to the server spawn.", usage = "/<command>")
public class Command_spawn extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (plugin.sm.sendToSpawn(playerSender))
        {
            msg("Teleported to spawn.");
        }
        return true;
    }
}
