package me.totalfreedom.totalfreedommod.command;

import java.util.ArrayList;
import java.util.List;
import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.banning.PermBan;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.baninfo")
@CommandParameters(description = "Show info about a ban by username or the first match to a (partial) IP.", usage = "/<command> <name|ip>", aliases = "checkban")
public class Command_baninfo extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length < 1)
        {
            return false;
        }

        final String query = args[0];

        // Lookup by username.
        Ban ban = plugin.bm.getByUsername(query);

        // Lookup by IP.
        if (ban == null)
        {
            ban = plugin.bm.getByIp(query);
        }

        if (ban != null)
        {
            printBan(sender, ban);
            return true;
        }

        // Lookup permanent bans.
        PermBan permban = plugin.pm.getPermban(query);

        if (permban == null)
        {
            for (String name : plugin.pm.getPermbannedNames())
            {
                PermBan candidate = plugin.pm.getPermban(name);
                if (candidate != null && candidate.getIps() != null && candidate.getIps().contains(query))
                {
                    permban = candidate;
                    break;
                }
            }
        }

        if (permban != null)
        {
            printPermban(sender, permban);
            return true;
        }

        // Fallback only triggers if the argument looks like a partial IP.
        if (isPartialIp(query))
        {
            String normalized = normalizePartialIp(query);
            int leading = countLeadingOctets(query);

            for (Ban candidate : plugin.bm.getAllBans())
            {
                if (candidate.isExpired())
                {
                    continue;
                }

                for (String ip : candidate.getIps())
                {
                    if (FUtil.fuzzyIpMatch(normalized, ip, leading))
                    {
                        printBan(sender, candidate);
                        return true;
                    }
                }
            }

            for (String name : plugin.pm.getPermbannedNames())
            {
                PermBan candidate = plugin.pm.getPermban(name);
                if (candidate == null || candidate.getIps() == null)
                {
                    continue;
                }

                for (String ip : candidate.getIps())
                {
                    if (FUtil.fuzzyIpMatch(normalized, ip, leading))
                    {
                        printPermban(sender, candidate);
                        return true;
                    }
                }
            }
        }

        msg(Component.text("No ban or permban found for: ").color(NamedTextColor.GRAY)
                .append(Component.text(query).color(NamedTextColor.WHITE)));

        return true;
    }

    /**
     * True if {@code s} looks like a partial IPv4 pattern.
     * A complete 4-octet IP with no wildcards is excluded.
     */
    private static boolean isPartialIp(String s)
    {
        if (s == null || s.indexOf('.') < 0)
        {
            return false;
        }

        String[] parts = s.split("\\.", -1);
        if (parts.length > 4)
        {
            return false;
        }

        boolean hasWildcard = false;
        for (String part : parts)
        {
            if (part.isEmpty())
            {
                return false;
            }

            if (part.equals("*"))
            {
                hasWildcard = true;
                continue;
            }

            for (int i = 0; i < part.length(); i++)
            {
                if (!Character.isDigit(part.charAt(i)))
                {
                    return false;
                }
            }
        }

        return hasWildcard || parts.length < 4;
    }

    /**
     * Pads the input to exactly 4 dot-separated segments with "*" placeholders.
     */
    private static String normalizePartialIp(String s)
    {
        String[] parts = s.split("\\.", -1);
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < 4; i++)
        {
            if (i > 0)
            {
                out.append('.');
            }

            out.append(i < parts.length ? parts[i] : "*");
        }

        return out.toString();
    }

    /**
     * Number of leading non-wildcard octets in the partial pattern.
     */
    private static int countLeadingOctets(String s)
    {
        String[] parts = s.split("\\.", -1);
        int n = 0;

        for (String part : parts)
        {
            if (part.equals("*"))
            {
                break;
            }

            n++;
        }

        return Math.min(n, 4);
    }

    private void printBan(CommandSender sender, Ban ban)
    {
        String header = ban.hasUsername() ? ban.getUsername() : "(IP-only)";

        msg(Component.text("Ban: ").color(NamedTextColor.RED)
                .append(Component.text(header).color(NamedTextColor.WHITE)));

        msg(Component.text("  Reason: ").color(NamedTextColor.GRAY)
                .append(Component.text(blankOr(ban.getReason(), "(none)")).color(NamedTextColor.WHITE)));

        msg(Component.text("  Banned by: ").color(NamedTextColor.GRAY)
                .append(Component.text(blankOr(ban.getBy(), "(unknown)")).color(NamedTextColor.WHITE)));

        msg(Component.text("  IPs: ").color(NamedTextColor.GRAY)
                .append(Component.text(formatIps(sender, ban.getIps())).color(NamedTextColor.WHITE)));

        if (ban.hasExpiry())
        {
            msg(Component.text("  Expires: ").color(NamedTextColor.GRAY)
                    .append(Component.text(Ban.DATE_FORMAT.format(ban.getExpiryDate())).color(NamedTextColor.WHITE)));
        }
        else
        {
            msg(Component.text("  Expires: ").color(NamedTextColor.GRAY)
                    .append(Component.text("Unknown (legacy ban)").color(NamedTextColor.YELLOW)));
        }
    }

    private void printPermban(CommandSender sender, PermBan permban)
    {
        String header = permban.getUsername() != null && !permban.getUsername().isEmpty()
                ? permban.getUsername()
                : "(IP-only)";

        msg(Component.text("Permban: ").color(NamedTextColor.DARK_RED)
                .append(Component.text(header).color(NamedTextColor.WHITE)));

        msg(Component.text("  Reason: ").color(NamedTextColor.GRAY)
                .append(Component.text(blankOr(permban.getReason(), "(none)")).color(NamedTextColor.WHITE)));

        msg(Component.text("  IPs: ").color(NamedTextColor.GRAY)
                .append(Component.text(formatIps(sender, permban.getIps())).color(NamedTextColor.WHITE)));

        msg(Component.text("  Expires: ").color(NamedTextColor.GRAY)
                .append(Component.text("Permanent").color(NamedTextColor.RED)));
    }

    private String formatIps(CommandSender sender, List<String> ips)
    {
        if (ips == null || ips.isEmpty())
        {
            return "(none)";
        }

        List<String> masked = new ArrayList<>(ips.size());
        for (String ip : ips)
        {
            masked.add(FUtil.sanitizeIp(sender, ip));
        }

        return String.join(", ", masked);
    }

    private static String blankOr(String value, String fallback)
    {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
