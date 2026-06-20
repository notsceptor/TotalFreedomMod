package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.setlimit")
@CommandParameters(description = "Sets the WorldEdit block modification limit globally or for a specific player.", usage = "/<command> [player] <limit>", aliases = "setl,swl")
public class Command_setlimit extends FreedomCommand
{

    private static final int DEFAULT_LIMIT = 2500;

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length > 2)
        {
            return false;
        }

        if (args.length <= 1)
        {
            final int limit;
            if (args.length == 0)
            {
                limit = DEFAULT_LIMIT;
            }
            else
            {
                final Integer parsed = parseLimit(args[0]);
                if (parsed == null)
                {
                    msg("Invalid limit: " + args[0] + ". Must be a non-negative whole number.", NamedTextColor.RED);
                    return true;
                }
                limit = parsed;
            }

            FUtil.adminAction(sender.getName(), "Setting everyone's WorldEdit block modification limit to " + limit, true);
            for (final Player player : server.getOnlinePlayers())
            {
                plugin.web.setLimit(player, limit);
            }
            return true;
        }

        final Player target = getPlayer(args[0]);
        if (target == null)
        {
            msg(PLAYER_NOT_FOUND, NamedTextColor.RED);
            return true;
        }

        final Integer limit = parseLimit(args[1]);
        if (limit == null)
        {
            msg("Invalid limit: " + args[1] + ". Must be a non-negative whole number.", NamedTextColor.RED);
            return true;
        }

        FUtil.adminAction(sender.getName(), "Setting " + target.getName() + "'s WorldEdit block modification limit to " + limit, true);
        plugin.web.setLimit(target, limit);
        msg(target, "Your WorldEdit block modification limit has been set to " + limit + ".", NamedTextColor.GREEN);
        return true;
    }

    private Integer parseLimit(String input)
    {
        try
        {
            final int value = Integer.parseInt(input);
            return value < 0 ? null : value;
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }
}
