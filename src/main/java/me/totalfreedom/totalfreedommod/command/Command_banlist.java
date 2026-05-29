package me.totalfreedom.totalfreedommod.command;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.admin.banlist")
@CommandParameters(description = "Shows all banned players and IP addresses. Superadmins may optionally use 'purge' to clear the list.", usage = "/<command> [purge]")
public class Command_banlist extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length > 0)
        {
            if (args[0].equalsIgnoreCase("purge"))
            {
                checkRank(Rank.SENIOR_ADMIN);

                FUtil.adminAction(sender.getName(), "Purging the ban list", true);
                int amount = plugin.bm.purge();
                sender.sendMessage(Component.text("Purged " + amount + " player bans.", NamedTextColor.GRAY));

                return true;
            }

            return false;
        }

        getBanList(sender);
        return true;
    }

    private void getBanList(CommandSender sender)
    {
        // Player bans: Bans that have a username.
        List<String> playerNames = new ArrayList<>();
        for (Ban ban : plugin.bm.getUsernameBans())
        {
            if (ban.hasUsername())
            {
                playerNames.add(ban.getUsername());
            }
        }
        playerNames.sort(String.CASE_INSENSITIVE_ORDER);

        // IP-only bans: Bans with at least one IP and no username.
        TreeSet<String> ipOnly = new TreeSet<>();
        for (Ban ban : plugin.bm.getIpBans())
        {
            if (!ban.hasUsername())
            {
                for (String ip : ban.getIps())
                {
                    ipOnly.add(FUtil.sanitizeIp(sender, ip));
                }
            }
        }

        // Permbans: usernames from PermbanList.
        List<String> permbanNames = new ArrayList<>(plugin.pm.getPermbannedNames());
        permbanNames.sort(String.CASE_INSENSITIVE_ORDER);

        if (playerNames.isEmpty() && ipOnly.isEmpty() && permbanNames.isEmpty())
        {
            msg("No bans on record.", NamedTextColor.GRAY);
            return;
        }

        if (!playerNames.isEmpty())
        {
            msg(buildSection("Player bans: ", NamedTextColor.RED, playerNames));
        }
        if (!ipOnly.isEmpty())
        {
            msg(buildSection("IP bans: ", NamedTextColor.GOLD, new ArrayList<>(ipOnly)));
        }
        if (!permbanNames.isEmpty())
        {
            msg(buildSection("Permbans: ", NamedTextColor.DARK_RED, permbanNames));
        }
    }

    private Component buildSection(String header, NamedTextColor headerColor, List<String> entries)
    {
        Component line = Component.text(header).color(headerColor);
        for (int i = 0; i < entries.size(); i++)
        {
            if (i > 0)
            {
                line = line.append(Component.text(", ").color(NamedTextColor.WHITE));
            }
            line = line.append(Component.text(entries.get(i)).color(NamedTextColor.WHITE));
        }
        return line;
    }
}
