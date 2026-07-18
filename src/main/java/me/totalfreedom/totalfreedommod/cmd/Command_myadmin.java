package me.totalfreedom.totalfreedommod.cmd;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;

import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Permission(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.admin.myadmin")
@Command(name = "myadmin", description = "Manage my admin entry", usage = "/myadmin <clearips | clearip <ip> | setlogin <message> | clearlogin>")
public class Command_myadmin extends FCommand
{

    @Callback
    @Subcommand("clearip")
    public void removeIp(Player player, @Resolve("IP") InetAddress address)
    {
        if (player.getAddress().getAddress().equals(address))
        {
            msg(player, "You can't remove your current IP address.", NamedTextColor.RED);
            return;
        }

        final Admin entry = plugin().al.getAdmin(player);
        if (!entry.getIps().contains(address.getHostAddress()))
        {
            msg(player, "That IP address isn't in your entry.");
            return;
        }

        entry.removeIp(address.getHostAddress());
    }

    @Callback
    @Subcommand("clearips")
    public void clearIps(Player player)
    {
        final Admin entry = plugin().al.getAdmin(player);
        final List<String> ips = entry.getIps();
        int count = ips.size();

        ips.clear();
        ips.add(player.getAddress().getAddress().getHostName());

        msg(player, count - 1 + " IP address(es) were removed.");
    }

    @Callback
    @Subcommand("setlogin")
    public void setLoginMessage(Player player, @Greedy String message)
    {
        final Admin entry = plugin().al.getAdmin(player);
        //removed conversion method. we want to enforce <tags> because tags are peak.

        entry.setLoginMessage(message);

        msg(
            player,
            """ 
                Your login message is now:
                > <login>
            """,
            MessageUtils.component("login", plugin().rm.formatLoginMessage(player))
        );
        
        plugin().al.save();
        plugin().al.updateTables();
    }

    @Callback
    @Subcommand("clearlogin")
    public void clearLogin(Player player)
    {
        final Admin entry = plugin().al.getAdmin(player);

        entry.setLoginMessage(null); // this needs a .resetLoginMessage() call so that we AVOID nullability. Not my problem right now.
        msg(player, "Your login message has been removed.");
        plugin().al.save();
        plugin().al.updateTables();
    }
}
