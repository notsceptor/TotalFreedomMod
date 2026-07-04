package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.player.creative")
@CommandParameters(description = "Quickly change your own gamemode to creative, or define someone's username to change theirs.", usage = "/<command> <-a | [partialname]>", aliases = "gmc")
public class Command_creative extends FreedomCommand
{

    @CommandDispatchTarget(switches = "a")
    public boolean setSelfGamemode(CommandContext ctx, boolean all)
    {
        if (ctx.isSenderConsole())
        {
            msg(ctx.getSender(), "When used from the console, you must define a target player.");
            return true;
        }

        if (all)
        {
            checkRank(Rank.SUPER_ADMIN);

            for (Player targetPlayer : server.getOnlinePlayers())
            {
                targetPlayer.setGameMode(GameMode.CREATIVE);
            }

            FUtil.adminAction(ctx.getSender().getName(), "Changing everyone's gamemode to creative", false);
            return true;

        }
        else
        {
            ctx.getPlayerSender().setGameMode(GameMode.CREATIVE);
        }
        msg("Gamemode set to creative.");
        return true;
    }

    @CommandDispatchTarget(pattern = "<player:Player>")
    public boolean setOtherGamemode(CommandContext ctx, Player player)
    {
        checkRank(Rank.SUPER_ADMIN);

        msg("Setting " + player.getName() + " to game mode creative");
        msg(player, ctx.getSender().getName() + " set your game mode to creative");
        player.setGameMode(GameMode.CREATIVE);

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
