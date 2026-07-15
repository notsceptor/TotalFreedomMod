package me.totalfreedom.totalfreedommod.cmd;

import java.net.InetAddress;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@Command(name = "unbanip", description = "Unbans an IP address.", usage = "/unbanip <ip>")
@Permission(permission = "tfm.admin.ban", level = Rank.SUPER_ADMIN)
public class Command_unbanip extends FCommand
{
    @Callback
    public void unbanip(CommandSender sender, InetAddress ip)
    {
        if (!plugin().bm.isIpBanned(ip.getHostAddress()))
        {
            msg(sender, "That IP address is not banned.");
            return;
        }

        adminAction(sender, "<red>Unbanning IP <ip>", Placeholder.unparsed("ip", FUtil.sanitizeIp(sender, ip.getHostAddress())));
        plugin().bm.unbanIp(ip.getHostAddress());
    }
}
