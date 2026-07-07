package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.findip")
@CommandParameters(description = "Shows all IPs registered to a player.", usage = "/<command> [player]", aliases = "ips,ip")
public class Command_findip extends FreedomCommand
{
    @CommandDispatchTarget
    public boolean getSelfIps(CommandContext ctx)
    {
        if (ctx.isSenderConsole())
        {
            msg(ctx.getSender(), "When used from the console, you must define a target player.");
            return true;
        }

        return getPlayerIps(ctx, ctx.getPlayerSender());
    }

    @CommandDispatchTarget(pattern = "<player:Player>")
    public boolean getPlayerIps(CommandContext ctx, Player player)
    {
        msg(ctx.getSender(), Component.text(player.getName(), NamedTextColor.GRAY)
                .append(Component.text("'s IP(s): "))
                .append(Component.join(JoinConfiguration.commas(true),
                        plugin.pl.getData(player).getIps().stream()
                                .map(ip -> Component.text(ip, NamedTextColor.WHITE))
                                .toList())));
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
