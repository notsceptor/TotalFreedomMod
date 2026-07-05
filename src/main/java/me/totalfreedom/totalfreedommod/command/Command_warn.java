package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.warn")
@CommandParameters(description = "Warns a player.", usage = "/<command> <player> <reason>")
public class Command_warn extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<player:Player> <reason..>")
    public boolean warnPlayer(CommandContext ctx, Player player, String reason)
    {
        if (!ctx.isSenderConsole() && ctx.getPlayerSender().equals(player))
        {
            msg(ctx.getSender(), "Please, don't try to warn yourself.", NamedTextColor.RED);
            return true;
        }
        else if (plugin.al.isAdmin(player))
        {
            msg(ctx.getSender(), "You can't warn admins.", NamedTextColor.RED);
            return true;
        }

        FUtil.adminAction(ctx.getSender().getName(), "Warning " + player.getName(), true);
        FUtil.bcastMsg(Component.text("  Reason: ", NamedTextColor.RED)
                .append(Component.text(reason, NamedTextColor.YELLOW)));

        plugin.pl.getPlayer(player).incrementWarnings();

        msg(player, Component.text("[WARNING] You have received a warning: ", NamedTextColor.RED)
                .append(FUtil.colorizeWithLinks(reason).colorIfAbsent(NamedTextColor.YELLOW)));

        msg (ctx.getSender(), player.getName() + " has been successfully warned.", NamedTextColor.GREEN);


        if (plugin.db.isReady())
        {
            plugin.db.sendActionMessage(ctx.getSender().getName(), player.getName(), reason,
                    ConfigEntry.DISCORD_PLAYER_WARN_MESSAGE);
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
