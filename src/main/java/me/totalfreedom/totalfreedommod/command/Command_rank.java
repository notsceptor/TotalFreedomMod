package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Displayable;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.NON_OP, source = SourceType.BOTH, permission = "tfm.player.rank")
@CommandParameters(description = "Shows ranks", usage = "/<command> [player]")
public class Command_rank extends FreedomCommand
{
    @CommandDispatchTarget
    public boolean querySelfOrAll(CommandContext ctx)
    {
        if (ctx.isSenderConsole())
        {
            server.getOnlinePlayers().forEach(player -> queryPlayer(ctx, player));
            return true;
        }

        return queryPlayer(ctx, ctx.getPlayerSender());
    }

    @CommandDispatchTarget(pattern = "<player:Player>")
    public boolean queryPlayer(CommandContext ctx, Player player)
    {
        Displayable display = plugin.rm.getDisplay(player);
        Rank rank = plugin.rm.getRank(player);

        Component result = Component.text(player.getName() + " is ", NamedTextColor.AQUA)
                .append(display.getColoredLoginMessage());

        if (rank != display)
        {
            result = result.append(Component.text(" (", NamedTextColor.AQUA))
                    .append(rank.getColoredName())
                    .append(Component.text(")", NamedTextColor.AQUA));
        }

        msg(ctx.getSender(), result);
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
