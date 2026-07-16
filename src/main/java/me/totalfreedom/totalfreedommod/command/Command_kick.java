package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
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
    @CommandDispatchTarget(pattern = "<player:Player> <reason..>", switches = "s")
    public boolean kick(CommandContext ctx, Player player, String reason, boolean silent)
    {
        if (plugin.al.isAdmin(player))
        {
            msg(ctx.getSender(), "This command cannot be used on other admins.");
            return true;
        }

        Component kickMessage = Component.text("You have been kicked from the server.", NamedTextColor.RED)
                .append(Component.text("\nKicked by: ", NamedTextColor.RED))
                .append(Component.text(sender.getName(), NamedTextColor.GOLD));

        if (reason != null)
        {
            kickMessage = kickMessage
                    .append(Component.text("\nReason: ", NamedTextColor.RED))
                    .append(FUtil.colorizeWithLinks(reason, NamedTextColor.GOLD));
        }

        if (!silent)
        {
            FUtil.adminAction(sender.getName(), reason == null ? "Kicking " + player.getName()  : "Kicking " + player.getName() + " - Reason: " + reason, true);
            plugin.db.sendActionMessage(sender.getName(), player.getName(), reason, ConfigEntry.DISCORD_PLAYER_KICK_MESSAGE);
        }

        player.kick(kickMessage);
        return true;
    }

    @CommandDispatchTarget(pattern = "<player:Player>", switches = "s")
    public boolean kickNoReason(CommandContext ctx, Player player, boolean silent)
    {
        return kick(ctx, player, null, silent);
    }

    @Override
    protected boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }

}
