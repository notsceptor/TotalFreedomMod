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

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length < 2)
        {
            return false;
        }

        Player player = getPlayer(args[0]);

        if (player == null)
        {
            msg(PLAYER_NOT_FOUND);
            return true;
        }

        if (sender instanceof Player)
        {
            if (player.equals(playerSender))
            {
                msg("Please, don't try to warn yourself.", NamedTextColor.RED);
                return true;
            }
        }

        if (plugin.al.isAdmin(player))
        {
            msg("You can not warn admins", NamedTextColor.RED);
            return true;
        }

        String warnReason = StringUtils.join(ArrayUtils.subarray(args, 1, args.length), " ");

        FUtil.adminAction(sender.getName(), "Warned " + player.getName(), true);
        if (warnReason != null)
        {
            FUtil.bcastMsg("  Reason: " + warnReason, NamedTextColor.YELLOW);
        }

        plugin.db.sendActionMessage(sender.getName(), player.getName(), warnReason, ConfigEntry.DISCORD_PLAYER_WARN_MESSAGE);

        msg(player, Component.text("[WARNING] You received a warning: ", NamedTextColor.RED)
                .append(FUtil.colorizeWithLinks(warnReason, NamedTextColor.RED)));
        msg("You have successfully warned " + player.getName(), NamedTextColor.GREEN);

        plugin.pl.getPlayer(player).incrementWarnings();

        return true;
    }
}
