package me.totalfreedom.totalfreedommod.command;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.totalfreedom.totalfreedommod.banning.PermBan;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_CONSOLE, permission = "tfm.admin.ban.perm")
@CommandParameters(description = "Manage permanently banned players and IPs.",
        usage = "/<command> <add <name> [ip...] | remove <name|ip> | reload>")
public class Command_permban extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length < 1)
        {
            return false;
        }

        switch (args[0].toLowerCase())
        {
            case "reload":
            {
                msg("Reloading permban list...", NamedTextColor.RED);
                plugin.pm.reload();
                msg("Reloaded permban list.");
                msg(plugin.pm.getPermbannedIps().size() + " IPs and "
                        + plugin.pm.getPermbannedNames().size() + " usernames loaded.");
                return true;
            }
            case "add":
            {
                return runAdd(sender, args);
            }
            case "remove":
            case "delete":
            {
                return runRemove(sender, args);
            }
            default:
            {
                return false;
            }
        }
    }

    private boolean runAdd(CommandSender sender, String[] args)
    {
        if (args.length < 2)
        {
            msg("Usage: /permban add <name> [ip...]", NamedTextColor.RED);
            return true;
        }

        final String input = args[1];
        final Player online = getPlayer(input);
        final PlayerData data = BanCommandUtil.getData(plugin, input, online);
        final String name = BanCommandUtil.getCanonicalName(input, online, data);

        final Set<String> ips = new LinkedHashSet<>(BanCommandUtil.getIps(online, data));
        for (int i = 2; i < args.length; i++)
        {
            final String ip = args[i];
            if (isValidIpOrRange(ip))
            {
                ips.add(ip);
            }
            else
            {
                msg("Ignoring invalid IP/range: " + ip, NamedTextColor.RED);
            }
        }

        final UUID uuid = online != null ? online.getUniqueId() : FUtil.usernameToUuid(name);

        PermBan permban = plugin.pm.getPermban(name);
        final boolean existed = permban != null;
        if (!existed)
        {
            permban = new PermBan(uuid, name, "Permbanned by " + sender.getName());
        }
        else if (uuid != null && !permban.hasUuid())
        {
            permban.setUuid(uuid);
        }

        final int before = permban.getIps().size();
        permban.addIps(new ArrayList<>(ips));
        final int addedIps = permban.getIps().size() - before;

        plugin.pm.addPermban(permban);

        FUtil.adminAction(sender.getName(),
                (existed ? "Updating permban for " : "Permbanning ") + name
                        + (addedIps > 0 ? " (" + addedIps + " IP record(s))" : ""), true);

        if (existed)
        {
            msg("Updated permban for " + name + "; added " + addedIps + " IP record(s) ("
                    + permban.getIps().size() + " total).");
        }
        else
        {
            msg("Permbanned " + name + " with " + permban.getIps().size() + " IP record(s).");
        }

        if (online != null)
        {
            online.kick(permbanKickMessage());
        }

        return true;
    }

    private boolean runRemove(CommandSender sender, String[] args)
    {
        if (args.length != 2)
        {
            msg("Usage: /permban remove <name|ip>", NamedTextColor.RED);
            return true;
        }

        final String target = args[1];

        if (isValidIpOrRange(target))
        {
            final List<String> removed = plugin.pm.removePermbansByIp(target);
            if (removed.isEmpty())
            {
                msg("No permbans matched the IP " + target + ".", NamedTextColor.RED);
            }
            else
            {
                FUtil.adminAction(sender.getName(), "Removing " + removed.size() + " permban(s) matching IP " + target, true);
                msg("Removed " + removed.size() + " permban(s) matching " + target + ": " + String.join(", ", removed) + ".");
            }
            return true;
        }

        if (plugin.pm.removePermban(target))
        {
            FUtil.adminAction(sender.getName(), "Removing the permban for " + target, true);
            msg("Removed the permban for " + target + ".");
        }
        else
        {
            msg(target + " is not permbanned.", NamedTextColor.RED);
        }

        return true;
    }

    private Component permbanKickMessage()
    {
        return Component.text("Your username is permanently banned from this server.\n"
                        + "Release procedures are available at\n", NamedTextColor.RED)
                .append(Component.text(ConfigEntry.SERVER_PERMBAN_URL.getString(), NamedTextColor.GOLD));
    }

    private static boolean isValidIpOrRange(String ip)
    {
        final String[] parts = ip.split("\\.");
        if (parts.length != 4)
        {
            return false;
        }
        for (String part : parts)
        {
            if (part.equals("*"))
            {
                continue;
            }
            try
            {
                final int octet = Integer.parseInt(part);
                if (octet < 0 || octet > 255)
                {
                    return false;
                }
            }
            catch (NumberFormatException ex)
            {
                return false;
            }
        }
        return true;
    }

}
