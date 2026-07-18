package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.rank.Displayable;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "rank", description = "Shows ranks", usage = "/<command> [player]")
@Permission(level = Rank.NON_OP, permission = "tfm.player.rank")
public class Command_rank extends FCommand
{
    @Callback
    public void querySelfOrAll(CommandSender sender)
    {
        if (!(sender instanceof Player playerSender))
        {
            server().getOnlinePlayers().forEach(player -> queryPlayer(sender, player));
            return;
        }

        queryPlayer(sender, playerSender);
    }

    @Callback
    public void queryPlayer(CommandSender sender, Player player)
    {
        final Displayable display = plugin().rm.getDisplay(player);
        final Rank rank = plugin().rm.getRank(player);

        Component result = Component.text(player.getName() + " is ", NamedTextColor.AQUA)
                                    .append(display.getColoredLoginMessage());

        if (rank != display)
        {
            result = result.append(Component.text(" (", NamedTextColor.AQUA))
                           .append(rank.getColoredName())
                           .append(Component.text(")", NamedTextColor.AQUA));
        }

        msg(sender, "<result>", MessageUtils.component("result", result));
    }
}
