package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.gamemode")
@CommandParameters(description = "Quickly change your own gamemode to adventure, or define someone's username to change theirs.", usage = "/<command> <-a | [partialname]>", aliases = "gma")
public class Command_adventure extends FreedomCommand
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
                targetPlayer.setGameMode(GameMode.ADVENTURE);
            }

            FUtil.adminAction(ctx.getSender().getName(), "Changing everyone's gamemode to adventure", false);
            return true;

        }
        else
        {
            ctx.getPlayerSender().setGameMode(GameMode.ADVENTURE);
        }
        msg("Gamemode set to adventure.");
        return true;
    }

    @CommandDispatchTarget(pattern = "<player:Player>")
    public boolean setOtherGamemode(CommandContext ctx, Player player)
    {
        checkRank(Rank.SUPER_ADMIN);

        msg("Setting " + player.getName() + " to game mode adventure");
        msg(player, ctx.getSender().getName() + " set your game mode to adventure");
        player.setGameMode(GameMode.ADVENTURE);

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
