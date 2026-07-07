package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.mute")
@CommandParameters(description = "Mutes a player with brute force.", usage = "/<command> <<player> [reason] | list | purge | all>", aliases = "mute")
public class Command_stfu extends FreedomCommand
{

    @CommandDispatchTarget(pattern = "list")
    public boolean list(CommandContext ctx)
    {
        final List<FPlayer> players = server.getOnlinePlayers().stream()
                .map(player -> plugin.pl.getPlayer(player))
                .filter(FPlayer::isMuted)
                .toList();

        if (players.isEmpty())
        {
            msg(ctx.getSender(), "No one on the server is currently muted.");
            return true;
        }

        msg(ctx.getSender(), Component.text("Muted players: ", NamedTextColor.GRAY)
                .append(Component.join(JoinConfiguration.commas(true),
                        players.stream().map(player -> ctx.isSenderConsole() ?
                                Component.text(player.getName(), NamedTextColor.WHITE) :
                                Component.text().colorIfAbsent(NamedTextColor.WHITE).append(
                                Objects.requireNonNull(server.getPlayer(player.getName())).displayName()
                                        .hoverEvent(HoverEvent.showText(Component.text(player.getName()))))).toList())));
        return true;
    }

    @CommandDispatchTarget(pattern = "purge")
    public boolean purge(CommandContext ctx)
    {
        FUtil.adminAction(ctx.getSender().getName(), "Unmuting all players", false);
        final List<FPlayer> players = server.getOnlinePlayers().stream()
                .map(player -> plugin.pl.getPlayer(player))
                .filter(FPlayer::isMuted)
                .peek(player -> player.setMuted(false))
                .toList();

        msg(ctx.getSender(), "Unmuted " + players.size() + " players.");
        return true;
    }

    @CommandDispatchTarget(pattern = "all")
    public boolean muteAll(CommandContext ctx)
    {
        FUtil.adminAction(ctx.getSender().getName(), "Muting all non-admins", true);
        final List<FPlayer> players = server.getOnlinePlayers().stream()
                .filter(player -> !plugin.al.isAdmin(player))
                .map(player -> plugin.pl.getPlayer(player))
                .peek(player -> player.setMuted(true))
                .toList();

        msg(ctx.getSender(), "Muted " + players.size() + " players.");
        return true;
    }

    @CommandDispatchTarget(pattern = "<player:Player>")
    public boolean mutePlayer(CommandContext ctx, Player player)
    {
        return mutePlayerWithReason(ctx, player, null);
    }

    @CommandDispatchTarget(pattern = "<player:Player> <reason..>")
    public boolean mutePlayerWithReason(CommandContext ctx, Player player, String reason)
    {
        final FPlayer fplayer = plugin.pl.getPlayer(player);

        if (fplayer.isMuted())
        {
            FUtil.adminAction(ctx.getSender().getName(), "Unmuting " + player.getName(), false);
            fplayer.setMuted(false);
            msg(player, "You have been unmuted.", NamedTextColor.GREEN);
        }
        else
        {
            FUtil.adminAction(ctx.getSender().getName(), Component.text("Muting ")
                    .append(Component.text(player.getName()))
                    .append(reason != null ?
                            Component.newline().append(Component.text("  Reason: ")
                                    .append(Component.text(reason, NamedTextColor.YELLOW))) :
                            Component.empty()), NamedTextColor.RED);

            fplayer.setMuted(true);
            msg(player, Component.text("You have been muted.", NamedTextColor.RED)
                    .append(reason != null ?
                            Component.text(" Reason: ")
                                    .append(FUtil.colorizeWithLinks(reason, NamedTextColor.YELLOW)) :
                            Component.empty()));
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
