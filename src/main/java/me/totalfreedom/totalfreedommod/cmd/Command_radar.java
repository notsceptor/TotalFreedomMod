package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Resolve;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;

@Command(name = "radar", description = "Shows nearby people sorted by distance.", usage = "/radar [limit]")
@Permission(permission = "tfm.player.radar", level = Rank.OP, source = SourceType.ONLY_IN_GAME)
public class Command_radar extends FCommand
{
    @Callback
    public void showNearbyPlayers(Player sender)
    {
        showNearbyPlayersInRange(sender, 5);
    }

    @Callback
    public void showNearbyPlayersInRange(Player sender, @Resolve("Integer") Integer limit)
    {
        limit = Math.clamp(limit, 1, 64);

        final Location center = sender.getLocation();
        final List<Player> nearbyPlayers = center.getWorld().getPlayers().stream()
                .filter(player -> !player.equals(sender))
                .sorted(Comparator.comparingDouble(player -> player.getLocation().distance(center)))
                .limit(limit)
                .toList();

        if (nearbyPlayers.isEmpty())
        {
            msg(sender,"<yellow>You are the only player in this world. (<green>Forever alone...<yellow>)"); //lol
            return;
        }

        msg(
                sender,
                "<yellow>People nearby in <world>:<newline><list:'<newline>'>",
                Placeholder.unparsed("world", center.getWorld().getName()),
                Formatter.joining(
                        "list",
                        nearbyPlayers.stream()
                                .map(player -> MessageUtils.parse("<player> - <distance> blocks",
                                        Placeholder.unparsed("player", player.getName()),
                                        Formatter.number("distance", Math.round(player.getLocation().distance(center)))))
                                .toList()
                ));
    }
}