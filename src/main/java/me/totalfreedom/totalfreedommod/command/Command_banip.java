package me.totalfreedom.totalfreedommod.command;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Bans an IP address or all known IP addresses for a player.", usage = "/<command> <player|ip> [reason]")
public class Command_banip extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<ip:IPs:allowPlayers,all>")
    public boolean banIPs(CommandContext ctx, List<InetAddress> ips)
    {
        return banIPsWithReason(ctx, ips, null);
    }

    @CommandDispatchTarget(pattern = "<ip:IPs:allowPlayers,all> <reason..>")
    public boolean banIPsWithReason(CommandContext ctx, List<InetAddress> ips, String reason)
    {
        FUtil.adminAction(ctx.getSender().getName(), Component.text("Banning ")
                .append(Component.text(ips.size()))
                .append(Component.text(" IP address(es)"))
                .append(reason != null ?
                        Component.newline().append(Component.text("  Reason: ")
                                .append(Component.text(reason, NamedTextColor.YELLOW))) :
                        Component.empty()), NamedTextColor.RED);

        ips.stream()
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

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return true;
    }
}
