package me.totalfreedom.totalfreedommod.command;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.admin.banlist")
@CommandParameters(description = "Shows all banned players and IP addresses. Superadmins may optionally use 'purge' to clear the list.", usage = "/<command> [purge]")
public class Command_banlist extends FreedomCommand
{
    @CommandDispatchTarget
    public boolean showBanList(CommandContext ctx)
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
            msg(ctx.getSender(), "No bans on record.", NamedTextColor.GRAY);
            return true;
        }

        if (!playerNames.isEmpty())
        {
            msg(ctx.getSender(), Component.text("Player bans: ", NamedTextColor.RED)
                    .append(Component.join(JoinConfiguration.commas(true),
                            playerNames.stream().map(Component::text).toList())
                            .color(NamedTextColor.WHITE)));
        }
        if (!ipOnly.isEmpty())
        {
            msg(ctx.getSender(), Component.text("IP bans: ", NamedTextColor.RED)
                    .append(Component.join(JoinConfiguration.commas(true),
                                    ipOnly.stream().map(Component::text).toList())
                            .color(NamedTextColor.WHITE)));
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "purge")
    public boolean purgeBanList(CommandContext ctx)
    {
        checkRank(Rank.SENIOR_ADMIN);

        FUtil.adminAction(ctx.getSender().getName(), "Purging the ban list", true);
        msg(ctx.getSender(), Component.text("Purged " + plugin.bm.purge() + " player bans."));
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
