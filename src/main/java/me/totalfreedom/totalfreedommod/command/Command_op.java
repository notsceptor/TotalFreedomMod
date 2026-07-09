package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.player.op")
@CommandParameters(description = "Makes a player operator", usage = "/<command> [player]")
public class Command_op extends FreedomCommand
{
    @CommandDispatchTarget
    public boolean opSelf(CommandContext ctx)
    {
        if (ctx.isSenderConsole() || !plugin.al.isAdmin(ctx.getPlayerSender()))
        {
            return false;
        }

        return opPlayer(ctx, ctx.getPlayerSender());
    }

    @CommandDispatchTarget(pattern = "<player:Player>")
    public boolean opPlayer(CommandContext ctx, Player player)
    {
        FUtil.adminAction(sender.getName(), "Opping " + player.getName(), false);
        player.setOp(true);
        msg(player, YOU_ARE_OP);
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
