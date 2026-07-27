package me.totalfreedom.totalfreedommod.cmd;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;

import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "tempban", aliases = {"tban", "noob"},
    description = "Temporarily bans an online or previously known player.",
    usage = "/<command> [-s] [-rb] <player> [duration] [reason]")
@Permission(permission = "tfm.admin.ban", level = Rank.SUPER_ADMIN)
public class Command_tempban extends FCommand
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

    @Callback
    public void tempBanDefault(CommandSender sender, String name, @Switch("s") boolean silent, @Switch("rb") boolean rollback)
    {
        tempBan(sender, name, FUtil.parseDateOffset("5m"), null, silent, rollback);
    }

    @Callback
    public void tempBanWithDuration(CommandSender sender, String name, @Resolve("DateOffset") Date duration, @Switch("s") boolean silent, @Switch("rb") boolean rollback)
    {
        tempBan(sender, name, duration, null, silent, rollback);
    }

    @Callback
    public void tempBanWithDurationAndReason(CommandSender sender, String name, @Resolve("DateOffset") Date duration, @Greedy String reason, @Switch("s") boolean silent, @Switch("rb") boolean rollback)
    {
        tempBan(sender, name, duration, reason, silent, rollback);
    }

    /**
     * The target is a plain {@code String} rather than a {@code Player} so that offline and
     * previously-known names can be banned, which means it gets no suggestions of its own.
     */
    @Completer(value = "", position = 0)
    public List<String> completeTarget(CommandSender sender, String partial)
    {
        return NameCandidates.online(server(), partial);
    }

    private void tempBan(CommandSender sender, String name, Date expiry, String reason, boolean silent, boolean rollback)
    {
        final Player player = server().getPlayer(name);
        final PlayerData data = BanCommandUtil.getData(plugin(), name, player);
        final String canonicalName = BanCommandUtil.getCanonicalName(name, player, data);

        if (plugin().bm.getByUsername(canonicalName) != null)
        {
            msg(sender, "<gray><player> is already banned.", Placeholder.unparsed("player", canonicalName));
            return;
        }

        final Ban ban = (player == null && data == null)
                ? Ban.forPlayerName(canonicalName, sender, expiry, reason)
                : BanCommandUtil.createFullBan(canonicalName, BanCommandUtil.getIps(player, data), sender, expiry, reason);

        plugin().bm.addBan(ban);

        if (!silent)
        {
            adminAction(
                        sender, "<red>Temporarily banning <player> until <until><include_reason:\" - Reason: <yellow><reason>\":\"\">",
                        Placeholder.unparsed("player", canonicalName),
                        Placeholder.unparsed("until", DATE_FORMAT.format(expiry)),
                        Formatter.booleanChoice("include_reason", reason != null && !reason.isEmpty()),
                        Placeholder.unparsed("reason", reason != null ? reason : "")
                    );

            plugin().db.sendActionMessage(sender.getName(), canonicalName, reason, ConfigEntry.DISCORD_PLAYER_TBAN_MESSAGE);
        }

        if (rollback)
            plugin().cpb.rollback(canonicalName);

        if (data != null && plugin().al.getEntryByName(canonicalName) == null)
        {
            data.setStrikes(data.getStrikes() + 1);
            plugin().pl.saveData(data);
        }

        final List<String> ips = ban.getIps();
        server().getOnlinePlayers()
                .stream()
                .filter(suspect -> suspect.equals(player)
                        || (suspect.getAddress() != null && ips.contains(Objects.requireNonNull(suspect.getAddress()).getAddress().getHostAddress())))
                .forEach(target ->
                {
                    if (!silent)
                    {
                        smitePlayer(target);
                    }
                    target.kick(ban.bakeKickMessage());
                });
    }
}
