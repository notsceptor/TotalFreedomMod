package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.NON_OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.player.spawn")
@CommandParameters(description = "Teleport to the server spawn.", usage = "/<command> [player]")
public class Command_spawn extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<player:Player>")
    public boolean sendPlayerToSpawn(final CommandContext ctx, final Player player)
    {
        if (!plugin.al.isAdmin(ctx.getSender()))
        {
            msg("No permission to send other players to spawn.");
            return true;
        }
        if (plugin.sm.sendToSpawn(player))
            msg(String.format("Sent %s to spawn.", player.getName()));
        return true;
    }

    @CommandDispatchTarget
    public boolean sendSelfToSpawn(final CommandContext ctx)
    {
        if (plugin.sm.sendToSpawn(ctx.getPlayerSender()))
            msg("Teleported to spawn.");
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
