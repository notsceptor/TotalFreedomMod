package me.totalfreedom.totalfreedommod.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;

@Command(name = "banlist", description = "Shows all banned players and IP addresses. Superadmins may optionally use 'purge' to clear the list.", usage = "/banlist [purge]")
@Permission(permission = "tfm.admin.banlist")
public class Command_banlist extends FCommand
{
    @Callback
    public void showBanList(CommandSender sender)
    {
        List<String> playerNames = new ArrayList<>();
        plugin.bm.getUsernameBans()
            .stream()
            .filter(Ban::hasUsername)
            .map(Ban::getUsername)
            .forEach(playerNames::add);

        TreeSet<String> ipOnly = new TreeSet<>();
        plugin.bm.getIpBans()
            .stream()
            .filter(b -> !b.hasUsername())
            .flatMap(b -> b.getIps().stream())
            .forEach(ip -> ipOnly.add(FUtil.sanitizeIp(sender, ip)));
        
        List<String> permbanNames = new ArrayList<>(plugin.pm.getPermbannedNames());
        permbanNames.sort(String.CASE_INSENSITIVE_ORDER);

        if (playerNames.isEmpty() && ipOnly.isEmpty() && permbanNames.isEmpty())
        {
            msg(sender, "No bans on record.", NamedTextColor.GRAY);
            return;
        }

        if (!playerNames.isEmpty())
        {
            StringBuilder sb = new StringBuilder("<red>Player bans: <white>");
            sb.append(playerNames.stream().collect(Collectors.joining("<gray>, <white>")));
            msg(sender, sb.toString());
        }
        if (!ipOnly.isEmpty())
        {
            StringBuilder sb = new StringBuilder("<red>IP bans: <white>");
            sb.append(ipOnly.stream().collect(Collectors.joining("<gray>, <white>")));
            msg(sender, sb.toString());
        }
    }   

    @Callback
    @Subcommand("purge")
    @Permission(level = Rank.SENIOR_ADMIN, permission = "tfm.admin.banlist")
    public void purgeBans(CommandSender sender)
    {
        // Ok so apparently plugin.bm.purge() purges the banlist then returns an int to count how many bans were purged. 
        adminAction(sender, "<red>Purging the ban list.");
        msg(sender, "Purged %i player bans.", plugin.bm.purge());
    }
}
