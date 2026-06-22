package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Temporarily bans a player for five minutes.", usage = "/<command> <player> [reason] [-rb]", aliases = "noob")
public class Command_tban extends FreedomCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length < 1)
        {
            return false;
        }

        final Player player = getPlayer(args[0]);

        if (player == null)
        {
            msg(FreedomCommand.PLAYER_NOT_FOUND);
            return true;
        }

        boolean rollback = args.length > 1 && args[args.length - 1].equalsIgnoreCase("-rb");
        int reasonEnd = rollback ? args.length - 1 : args.length;

        String reason = reasonEnd > 1
                ? StringUtils.join(args, " ", 1, reasonEnd)
                : "You have been temporarily banned for 5 minutes.";

        final Location targetPos = player.getLocation();
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                final Location strikePos = new Location(targetPos.getWorld(), targetPos.getBlockX() + x, targetPos.getBlockY(), targetPos.getBlockZ() + z);
                targetPos.getWorld().strikeLightning(strikePos);
            }
        }

        FUtil.adminAction(sender.getName(), "Tempbanning: " + player.getName() + " for 5 minutes.", true);
        plugin.bm.addBan(Ban.forPlayer(player, sender, FUtil.parseDateOffset("5m"), reason));

        if (rollback)
        {
            plugin.cpb.rollback(player.getName());
        }

        player.kick(Component.text("You have been temporarily banned for five minutes. Please read totalfreedom.me for more info.", NamedTextColor.RED));

        return true;
    }
}
