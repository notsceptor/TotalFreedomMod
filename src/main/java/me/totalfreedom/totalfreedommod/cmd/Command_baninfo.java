package me.totalfreedom.totalfreedommod.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.banning.PermBan;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Command(name = "", description = "")
public class Command_baninfo extends FCommand
{
    @Callback
    public void query(CommandSender sender, String target)
    {
        Optional<Ban> ban = Optional.ofNullable(plugin.bm.getByUsername(target));

        if (ban.isEmpty()) ban.map(c -> plugin.bm.getByIp(target));

        if (ban.isPresent()) 
        {
            printBan(sender, ban.get());
            return;
        }

        Optional<PermBan> permban = Optional.ofNullable(plugin.pm.getPermban(target));

        if (permban.isEmpty())
        {
            permban = plugin.pm.getPermbannedNames().stream()
                .map(plugin.pm::getPermban)    
                .filter(candidate -> {
                    return candidate != null && candidate.getIps() != null && candidate.getIps().contains(target);
                })
                .findFirst();
        }

        if (permban.isPresent())
        {
            printPermban(sender, permban.get());
        }

        if (isPartialIP(target))
        {
            String normalized = normalizePartialIP(target);
            int leading = countLeadingOctets(target);
            
            for (Ban candidate : plugin.bm.getAllBans())
            {
                if (candidate.isExpired()) continue;

                for (String ip : candidate.getIps())
                {
                    if (FUtil.fuzzyIpMatch(normalized, ip, leading))
                    {
                        printBan(sender, candidate);
                        return;
                    }
                }
            }

            for (String name : plugin.pm.getPermbannedNames())
            {
                PermBan candidate = plugin.pm.getPermban(name);
                
                if (candidate == null || candidate.getIps() == null) continue;

                for (String ip : candidate.getIps())
                {
                    if (FUtil.fuzzyIpMatch(normalized, ip, leading))
                    {
                        printPermban(sender, candidate);
                        return;
                    }
                }
            }
        }

        msg(sender, Component.text("No ban or permban found for: ").color(NamedTextColor.GRAY)
                .append(Component.text(target).color(NamedTextColor.WHITE)));
    }

    private static boolean isPartialIP(String s)
    {
        if (s == null || s.indexOf('.') < 0) return false;

        String[] parts = s.split("\\.", -1);
        if (parts.length > 4) return false;

        boolean hasWildcard = false;
        for (String part : parts)
        {
            if (part.isEmpty()) return false;

            if (part.equals("*"))
            {
                hasWildcard = true;
                continue;
            }

            for (int i = 0; i < part.length(); i++)
            {
                if (!Character.isDigit(part.charAt(i))) return false;
            }
        }

        return hasWildcard || parts.length < 4;
    }

    private static String normalizePartialIP(String s)
    {
        String[] parts = s.split("\\.", -1);
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < 4; i++)
        {
            if (i > 0) out.append('.');

            out.append(i < parts.length ? parts[i] : "*");
        }

        return out.toString();
    }

    private static int countLeadingOctets(String s)
    {
        String[] parts = s.split("\\.", -1);
        int n = 0;

        for (String part : parts)
        {
            if (part.equals("*")) break;

            n++;
        }

        return Math.min(n, 4);
    }

    private void printBan(CommandSender sender, Ban ban)
    {
        String header = ban.hasUsername() ? ban.getUsername() : "(IP-only)";

        msg(sender, Component.text("Ban: ").color(NamedTextColor.RED)
                .append(Component.text(header).color(NamedTextColor.WHITE)));

        msg(sender, Component.text("  Reason: ").color(NamedTextColor.GRAY)
                .append(FUtil.colorizeWithLinks(blankOr(ban.getReason(), "(none)"), NamedTextColor.WHITE)));

        msg(sender, Component.text("  Banned by: ").color(NamedTextColor.GRAY)
                .append(Component.text(blankOr(ban.getBy(), "(unknown)")).color(NamedTextColor.WHITE)));

        msg(sender, Component.text("  IPs: ").color(NamedTextColor.GRAY)
                .append(Component.text(formatIps(sender, ban.getIps())).color(NamedTextColor.WHITE)));

        if (ban.hasExpiry())
        {
            msg(sender, Component.text("  Expires: ").color(NamedTextColor.GRAY)
                    .append(Component.text(Ban.DATE_FORMAT.format(ban.getExpiryDate())).color(NamedTextColor.WHITE)));
        }
        else
        {
            msg(sender, Component.text("  Expires: ").color(NamedTextColor.GRAY)
                    .append(Component.text("Unknown (legacy ban)").color(NamedTextColor.YELLOW)));
        }
    }

    private void printPermban(CommandSender sender, PermBan permban)
    {
        String header = permban.getUsername() != null && !permban.getUsername().isEmpty()
                ? permban.getUsername()
                : "(IP-only)";

        msg(sender, Component.text("Permban: ").color(NamedTextColor.DARK_RED)
                .append(Component.text(header).color(NamedTextColor.WHITE)));

        msg(sender, Component.text("  Reason: ").color(NamedTextColor.GRAY)
                .append(FUtil.colorizeWithLinks(blankOr(permban.getReason(), "(none)"), NamedTextColor.WHITE)));

        msg(sender, Component.text("  IPs: ").color(NamedTextColor.GRAY)
                .append(Component.text(formatIps(sender, permban.getIps())).color(NamedTextColor.WHITE)));

        msg(sender, Component.text("  Expires: ").color(NamedTextColor.GRAY)
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
