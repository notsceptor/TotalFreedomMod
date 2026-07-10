package me.totalfreedom.totalfreedommod.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
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
            msg(sender, Component.text("Player bans: ", NamedTextColor.RED)
                    .append(Component.join(JoinConfiguration.commas(true),
                            playerNames.stream().map(Component::text).toList())
                            .color(NamedTextColor.WHITE)));
        }
        if (!ipOnly.isEmpty())
        {
            msg(sender, Component.text("IP bans: ", NamedTextColor.RED)
                    .append(Component.join(JoinConfiguration.commas(true),
                                    ipOnly.stream().map(Component::text).toList())
                            .color(NamedTextColor.WHITE)));
        }
    }   

    // So purging is effectively noop for intended function???
    @Callback
    @Subcommand("purge")
    @Permission(level = Rank.SENIOR_ADMIN, permission = "tfm.admin.banlist")
    public void purgeBans(CommandSender sender)
    {
        //TODO: Actually implement purging. That's not in scope for this particular branch, but it needs to be done.
        FUtil.adminAction(sender.getName(), "Purging the ban list.", true);
        msg(sender, Component.text("Purged " + plugin.bm.purge() + " player bans."));        
    }
}
