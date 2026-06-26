package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.kick")
@CommandParameters(description = "Kick a player.", usage = "/<command> [-s] <player> [reason]", aliases = "k")
public class Command_kick extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<playerName> <reason..>", switches = "s")
    public boolean kick(CommandContext ctx, String playerName, String reason, boolean silent)
    {
        Player player = getPlayer(playerName);
        if (player == null)
        {
            msg(PLAYER_NOT_FOUND);
            return true;
        }

        if (isAdmin(player))
        {
            msg("Admins can not be kicked", NamedTextColor.RED);
            return true;
        }

        Component kickMessage = Component.text("You have been kicked from the server.", NamedTextColor.RED)
                .append(Component.text("\nKicked by: ", NamedTextColor.RED))
                .append(Component.text(sender.getName(), NamedTextColor.GOLD));

        if (reason != null)
        {
            kickMessage = kickMessage
                    .append(Component.text("\nReason: ", NamedTextColor.RED))
                    .append(Component.text(reason, NamedTextColor.GOLD));
            if (!silent)
                FUtil.adminAction(sender.getName(), "Kicking " + player.getName() + " - Reason: " + reason, true);
        }
        else
        {
            if (!silent)
                FUtil.adminAction(sender.getName(), "Kicking " + player.getName(), true);
        }

        player.kick(kickMessage);
        return true;
    }

    @CommandDispatchTarget(pattern = "<playerName>", switches = "s")
    public boolean kickNoReason(CommandContext ctx, String playerName, boolean silent)
    {
        return kick(ctx, playerName, null, silent);
    }

    @Override
    protected boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }

}
