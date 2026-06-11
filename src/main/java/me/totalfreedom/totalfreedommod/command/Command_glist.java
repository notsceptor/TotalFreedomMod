package me.totalfreedom.totalfreedommod.command;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.glist")
@CommandParameters(description = "Bans or unbans any player, even those who are not logged in anymore.", usage = "/<command> <purge | ban <username> [reason] | unban <username>>")
public class Command_glist extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length < 1)
        {
            return false;
        }

        if (args.length == 1)
        {
            if ("purge".equals(args[0]))
            {
                checkRank(Rank.SENIOR_ADMIN);
                plugin.pl.purgeAllData();
                msg("Purged playerbase.");

                return true;
            }

            return false;
        }

        if (args.length < 2)
        {
            return false;
        }

        final String target = args[1];

        if ("ban".equals(args[0]))
        {
            final String username;
            final List<String> ips = new ArrayList<>();

            final Player player = getPlayer(target);
            if (player == null)
            {
                final PlayerData entry = plugin.pl.getData(target);

                if (entry == null)
                {
                    msg("Can't find that user. If target is not logged in, make sure that you spelled the name exactly.");
                    return true;
                }

                username = entry.getUsername();
                ips.addAll(entry.getIps());
            }
            else
            {
                final PlayerData entry = plugin.pl.getData(player);
                username = player.getName();
                ips.addAll(entry.getIps());
            }

            FUtil.adminAction(sender.getName(), "Banning " + username + " and IPs: " + StringUtils.join(ips, ", "), true);

            final String reason = args.length > 2 ? StringUtils.join(args, " ", 2, args.length) : null;

            Ban ban = Ban.forPlayerName(username, sender, null, reason);
            for (String ip : ips)
            {
                ban.addIp(ip);
                ban.addIp(FUtil.getFuzzyIp(ip));
            }
            plugin.bm.addBan(ban);

            if (player != null)
            {
                player.kick(ban.bakeKickMessage());
            }
            return true;
        }

        if ("unban".equals(args[0]))
        {
            final Set<Ban> toRemove = new LinkedHashSet<>();

            final Ban byName = plugin.bm.getByUsername(target);
            if (byName != null)
            {
                toRemove.add(byName);
            }

            final Ban byIp = plugin.bm.getByIp(target);
            if (byIp != null)
            {
                toRemove.add(byIp);
            }

            final List<String> ips = new ArrayList<>();
            final Player player = getPlayer(target);
            final PlayerData entry = player != null ? plugin.pl.getData(player) : plugin.pl.getData(target);
            if (entry != null)
            {
                ips.addAll(entry.getIps());
            }
            for (Ban ban : new ArrayList<>(toRemove))
            {
                ips.addAll(ban.getIps());
            }

            for (String ip : ips)
            {
                Ban ban = plugin.bm.getByIp(ip);
                if (ban != null)
                {
                    toRemove.add(ban);
                }
                ban = plugin.bm.getByIp(FUtil.getFuzzyIp(ip));
                if (ban != null)
                {
                    toRemove.add(ban);
                }
            }

            if (toRemove.isEmpty())
            {
                msg("No ban on record for: " + target);
                return true;
            }

            final Set<String> removedIps = new LinkedHashSet<>();
            for (Ban ban : toRemove)
            {
                removedIps.addAll(ban.getIps());
                plugin.bm.removeBan(ban);
            }

            FUtil.adminAction(sender.getName(), "Unbanning " + target + " and IPs: " + StringUtils.join(removedIps, ", "), true);
            return true;
        }

        return false;
    }
}
