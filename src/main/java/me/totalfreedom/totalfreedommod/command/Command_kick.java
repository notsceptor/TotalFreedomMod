package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.kick")
@CommandParameters(description = "Kick a player.", usage = "/<command> <player> [reason]", aliases = "k")
public class Command_kick extends FreedomCommand
{

    @Override
    protected boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length == 0)
        {
            return false;
        }

        Player player = getPlayer(args[0]);
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

        String reason = null;
        if (args.length > 1)
        {
            reason = StringUtils.join(args, " ", 1, args.length);
        }

        Component kickMessage = Component.text("You have been kicked from the server.", NamedTextColor.RED)
                .append(Component.text("\nKicked by: ", NamedTextColor.RED))
                .append(Component.text(sender.getName(), NamedTextColor.GOLD));

        if (reason != null)
        {
            kickMessage = kickMessage
                    .append(Component.text("\nReason: ", NamedTextColor.RED))
                    .append(Component.text(reason, NamedTextColor.GOLD));
            FUtil.adminAction(sender.getName(), "Kicking " + player.getName() + " - Reason: " + reason, true);
        }
        else
        {
            FUtil.adminAction(sender.getName(), "Kicking " + player.getName(), true);
        }

        player.kick(kickMessage);
        return true;
    }

}
