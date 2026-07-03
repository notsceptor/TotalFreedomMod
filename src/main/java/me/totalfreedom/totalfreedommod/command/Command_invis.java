package me.totalfreedom.totalfreedommod.command;

import java.util.List;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.invis")
@CommandParameters(description = "Shows (and optionally clears) invisisible players", usage = "/<command> [clear]")
public class Command_invis extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "clear")
    public boolean clear(CommandContext ctx)
    {
        FUtil.adminAction(sender.getName(), "Clearing invisibility for all players", false);
        long count = server.getOnlinePlayers().stream()
                .filter(player -> player.hasPotionEffect(PotionEffectType.INVISIBILITY))
                .filter(player -> !plugin.al.isAdmin(player))
                .peek(player -> player.removePotionEffect(PotionEffectType.INVISIBILITY))
                .count();

        msg(ctx.getSender(), "Cleared invisibility effect from " + count + " players.");
        return true;
    }

    @CommandDispatchTarget
    public boolean list(CommandContext ctx)
    {
        final List<Component> players = server.getOnlinePlayers().stream()
                .filter(player -> player.hasPotionEffect(PotionEffectType.INVISIBILITY))
                .map(player -> player.displayName()
                        .colorIfAbsent(NamedTextColor.WHITE)
                        .hoverEvent(HoverEvent.showText(Component.text(player.getName())))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, ClickEvent.Payload.string("/tp " + player.getName()))))
                .toList();

        if (players.isEmpty())
        {
            msg(ctx.getSender(), "There are no invisible players.");
        }
        else
        {
            msg(ctx.getSender(), Component.text("Invisible players (")
                    .append(Component.text(players.size()))
                    .append(Component.text("): "))
                    .append(Component.join(JoinConfiguration.commas(true), players))
                    .color(NamedTextColor.GRAY));
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
