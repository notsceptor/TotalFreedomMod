package me.totalfreedom.totalfreedommod.command;

import java.util.Comparator;
import java.util.List;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.NON_OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.player.radar")
@CommandParameters(description = "Shows nearby people sorted by distance.", usage = "/<command> [limit]")
public class Command_radar extends FreedomCommand
{
    @CommandDispatchTarget
    public boolean showNearbyPlayers(CommandContext ctx)
    {
        return showNearbyPlayersInRange(ctx, 5);
    }

    @CommandDispatchTarget(pattern = "<limit:Integer>")
    public boolean showNearbyPlayersInRange(CommandContext ctx, Integer limit)
    {
        limit = Math.clamp(limit, 1, 64);

        final Location center = ctx.getPlayerSender().getLocation();
        final List<Player> nearbyPlayers = center.getWorld().getPlayers().stream()
                .filter(player -> !player.equals(ctx.getPlayerSender()))
                .sorted(Comparator.comparingDouble(player -> player.getLocation().distance(center)))
                .limit(limit)
                .toList();

        if (nearbyPlayers.isEmpty())
        {
            msg(ctx.getSender(), Component.text("You are the only player in this world. (", NamedTextColor.YELLOW)
                    .append(Component.text("Forever alone...", NamedTextColor.GREEN))
                    .append(Component.text(")", NamedTextColor.YELLOW))); //lol
            return true;
        }

        msg(ctx.getSender(), "People nearby in " + center.getWorld().getName() + ":", NamedTextColor.YELLOW);
        nearbyPlayers.forEach(player -> msg(ctx.getSender(),
                player.getName() + " - " + Math.round(player.getLocation().distance(center)), NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
