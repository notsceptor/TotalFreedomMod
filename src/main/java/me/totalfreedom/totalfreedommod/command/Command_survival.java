package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.player.survival")
@CommandParameters(description = "Quickly change your own gamemode to survival, or define someone's username to change theirs.", usage = "/<command> <[partialname] | -a>", aliases = "gms")
public class Command_survival extends FreedomCommand
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
                targetPlayer.setGameMode(GameMode.SURVIVAL);
            }

            FUtil.adminAction(ctx.getSender().getName(), "Changing everyone's gamemode to survival", false);
            return true;

        }
        else
        {
            ctx.getPlayerSender().setGameMode(GameMode.SURVIVAL);
        }
        msg("Gamemode set to survival.");
        return true;
    }

    @CommandDispatchTarget(pattern = "<player:Player>")
    public boolean setOtherGamemode(CommandContext ctx, Player player)
    {
        checkRank(Rank.SUPER_ADMIN);

        msg("Setting " + player.getName() + " to game mode survival");
        msg(player, ctx.getSender().getName() + " set your game mode to survival");
        player.setGameMode(GameMode.SURVIVAL);

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
