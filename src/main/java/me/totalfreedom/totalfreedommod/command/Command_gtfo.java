package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Makes someone GTFO (deop and ip ban by username).", usage = "/<command> <partialname>")
public class Command_gtfo extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {

        if (args.length == 0)
        {
            return false;
        }

        final Player player = getPlayer(args[0]);

        if (player == null)
        {
            msg(FreedomCommand.PLAYER_NOT_FOUND, NamedTextColor.RED);
            return true;
        }

        String reason = null;
        if (args.length >= 2)
        {
            reason = StringUtils.join(ArrayUtils.subarray(args, 1, args.length), " ");
        }

        FUtil.bcastMsg(player.getName() + " has been a VERY naughty, naughty boy.", NamedTextColor.RED);

        // Undo WorldEdits
        try
        {
            plugin.web.undo(player, 15);
        }
        catch (NoClassDefFoundError ex)
        {
        }

        // Rollback
        plugin.rb.rollback(player.getName());

        // Deop
        player.setOp(false);

        // Gamemode suvival
        player.setGameMode(GameMode.SURVIVAL);

        // Clear inventory
        player.getInventory().clear();

        // Strike with lightning
        final Location targetPos = player.getLocation();
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                final Location strike_pos = new Location(targetPos.getWorld(), targetPos.getBlockX() + x, targetPos.getBlockY(), targetPos.getBlockZ() + z);
                targetPos.getWorld().strikeLightning(strike_pos);
            }
        }

        String ip = FUtil.getFuzzyIp(player.getAddress().getAddress().getHostAddress());

        // Broadcast
        Component bcast = Component.text("Banning: " + player.getName() + ", IP: " + ip, NamedTextColor.RED);
        if (reason != null)
        {
            bcast = bcast.append(Component.text(" - Reason: ", NamedTextColor.RED))
                    .append(Component.text(reason, NamedTextColor.YELLOW));
        }
        FUtil.bcastMsg(bcast);

        // Ban player
        plugin.bm.addBan(Ban.forPlayerFuzzy(player, sender, null, reason));

        // Kick player
        player.kick(Component.text("GTFO", NamedTextColor.RED));

        return true;
    }
}
