package me.totalfreedom.totalfreedommod.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;

@Command(name = "banlist", description = "Shows all banned players and IP addresses. Senior Admins may optionally use 'purge' to clear the list.", usage = "/banlist [purge]")
@Permission(permission = "tfm.admin.banlist")
public class Command_banlist extends FCommand
{
    @Callback
    public void showBanList(CommandSender sender)
    {
        List<String> playerNames = new ArrayList<>();
        plugin().bm.getUsernameBans()
            .stream()
            .filter(Ban::hasUsername)
            .map(Ban::getUsername)
            .forEach(playerNames::add);

        TreeSet<String> ipOnly = new TreeSet<>();
        plugin().bm.getIpBans()
            .stream()
            .filter(b -> !b.hasUsername())
            .flatMap(b -> b.getIps().stream())
            .forEach(ip -> ipOnly.add(FUtil.sanitizeIp(sender, ip)));
        
        List<String> permbanNames = new ArrayList<>(plugin().pm.getPermbannedNames());
        permbanNames.sort(String.CASE_INSENSITIVE_ORDER);

        if (playerNames.isEmpty() && ipOnly.isEmpty() && permbanNames.isEmpty())
        {
            msg(sender, "<gray>No bans on record.");
            return;
        }

        if (!playerNames.isEmpty())
        {
            msg(sender, "<red>Player bans: <white><list>",
                    Formatter.joining("list", playerNames.stream()
                            .map(name -> MessageUtils.parse(name + "<gray>, <white>"))
                            .toList()));
        }

        if (!ipOnly.isEmpty())
        {
            msg(sender, "<red>IP bans: <white><list>",
                    Formatter.joining("list", ipOnly.stream()
                            .map(ip -> MessageUtils.parse(ip + "<gray>, <white>"))
                            .toList()));
        }
    }   

    @Callback
    @Subcommand("purge")
    @Permission(level = Rank.SENIOR_ADMIN, permission = "tfm.admin.banlist")
    public void purgeBans(CommandSender sender)
    {
        // Ok so apparently plugin().bm.purge() purges the banlist then returns an int to count how many bans were purged. 
        adminAction(sender, "<red>Purging the ban list");
        msg(sender, "<gray>Purged <count> player bans.", Formatter.number("count", plugin().bm.purge()));
    }
}
