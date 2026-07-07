package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Bans the specified name.", usage = "/<command> <name> [reason]")
public class Command_banname extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<name>")
    public boolean banName(CommandContext ctx, String name)
    {
        return banNameWithReason(ctx, name, null);
    }

    @CommandDispatchTarget(pattern = "<name> <reason..>")
    public boolean banNameWithReason(CommandContext ctx, String name, String reason)
    {
        if (plugin.bm.getByUsername(name) != null)
        {
            msg(ctx.getSender(), name + " is already banned.");
            return true;
        }
        final Ban ban = Ban.forPlayerName(name, ctx.getSender(), null, reason);

        plugin.bm.addBan(ban);

        final Player player = server.getPlayer(name);
        if (player != null)
        {
            player.kick(ban.bakeKickMessage());
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
