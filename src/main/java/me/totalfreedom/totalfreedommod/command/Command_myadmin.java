package me.totalfreedom.totalfreedommod.command;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;

import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.admin.myadmin")
@CommandParameters(description = "Manage my admin entry", usage = "/<command> <clearips | clearip <ip> | setlogin <message> | clearlogin>")
public class Command_myadmin extends FreedomCommand
{

    @CommandDispatchTarget(pattern = "clearip <ip:IP>")
    public boolean removeIp(CommandContext ctx, InetAddress address)
    {
        if (Objects.requireNonNull(playerSender.getAddress()).getAddress().equals(address))
        {
            msg(ctx.getSender(), "You can't remove your current IP address.", NamedTextColor.RED);
            return true;
        }

        final Admin entry = plugin.al.getAdmin(ctx.getPlayerSender());
        if (!entry.getIps().contains(address.getHostAddress()))
        {
            msg(ctx.getSender(), "That IP address isn't in your entry.");
            return true;
        }

        entry.removeIp(address.getHostAddress());
        return true;
    }

    @CommandDispatchTarget(pattern = "clearips")
    public boolean clearIps(CommandContext ctx)
    {
        final Admin entry = plugin.al.getAdmin(ctx.getPlayerSender());
        final List<String> ips = entry.getIps();
        int count = ips.size();

        ips.clear();
        ips.add(Objects.requireNonNull(ctx.getPlayerSender().getAddress()).getAddress().getHostName());

        msg(ctx.getSender(), count - 1 + " IP address(es) were removed.");
        return true;
    }

    @CommandDispatchTarget(pattern = "setlogin <message..>")
    public boolean setLoginMessage(CommandContext ctx, String message)
    {
        final Admin entry = plugin.al.getAdmin(ctx.getPlayerSender());

        // Temporary legacy to MiniMessage placeholder conversion for login messages, remove if no longer needed
        if (message.contains("%name%") || message.contains("%rank%") || message.contains("%coloredrank%"))
        {
            message = message.replace("%name%", "<name>")
                    .replace("%rank%", "<rank>")
                    .replace("%coloredrank%", "<colored_rank>");

            msg(ctx.getSender(), Component.text("MiniMessage is now favored over legacy placeholder tags. Use ", NamedTextColor.GRAY)
                    .append(Component.text("<name>", NamedTextColor.AQUA))
                    .append(Component.text(", ", NamedTextColor.GRAY))
                    .append(Component.text("<rank>", NamedTextColor.AQUA))
                    .append(Component.text(" or ", NamedTextColor.GRAY))
                    .append(Component.text("<colored_rank>", NamedTextColor.AQUA))
                    .append(Component.text(" instead. We'll convert it for you, but this conversion may be removed in the future.", NamedTextColor.GRAY)));
        }

        entry.setLoginMessage(message);
        msg(ctx.getSender(), "Your login message is now: ");
        msg(ctx.getSender(), Component.text("> ").append(plugin.rm.formatLoginMessage(ctx.getPlayerSender())));
        plugin.al.save();
        plugin.al.updateTables();

        return true;
    }

    @CommandDispatchTarget(pattern = "clearlogin")
    public boolean clearLogin(CommandContext ctx)
    {
        final Admin entry = plugin.al.getAdmin(ctx.getPlayerSender());

        entry.setLoginMessage(null);
        msg(ctx.getSender(), "Your login message has been removed.");
        plugin.al.save();
        plugin.al.updateTables();

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
