package me.totalfreedom.totalfreedommod.cmd;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Command(name = "banip", description = "Bans an IP address or all known IP addresses for a player.", usage = "/banip <player|ip> [reason]")
@Permission(level = Rank.SUPER_ADMIN, permission = "tfm.admin.ban")
public class Command_banip extends FCommand 
{
    
    // @Resolve("IPs") indicates to the command processor that the provided argument should be resolved via the InetAddressListResolver
    @Callback
    public void banIps(CommandSender sender, @Resolve("IPs") List<InetAddress> addressList)
    {
        banIpsWithReason(sender, addressList, null);
    }
    
    @Callback
    public void banIpsWithReason(CommandSender sender, @Resolve("IPs") List<InetAddress> addressList, @Greedy String reason)
    {
        FUtil.adminAction(sender.getName(), Component.text("Banning ")
                .append(Component.text(addressList.size()))
                .append(Component.text(" IP address(es)"))
                .append(reason != null ?
                        Component.newline().append(Component.text("  Reason: ")
                                .append(Component.text(reason, NamedTextColor.YELLOW))) :
                        Component.empty()), NamedTextColor.RED);

        addressList.stream()
                .filter(ip -> !plugin.bm.isIpBanned(ip.getHostAddress()))
                .forEach(ip -> 
                    {
                        final Ban ban = Ban.forPlayerIp(ip.getHostAddress(), sender, null, reason);
                        BanCommandUtil.addRangeIpIfEnabled(ban, ip.getHostAddress());

                        if (plugin.bm.addBan(ban))
                        {
                            server.getOnlinePlayers().stream()
                                .filter(player -> Objects.requireNonNull(player.getAddress()).getAddress().equals(ip))
                                .map(player -> (Player) player)
                                .forEach(player -> player.kick(ban.bakeKickMessage()));
                        }
                    });
    }
}
