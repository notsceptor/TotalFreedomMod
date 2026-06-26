package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Temporarily bans a player for five minutes.", usage = "/<command> [-s] [-rb] <player> [reason]", aliases = "noob")
public class Command_tban extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<playerName> <reason..>", switches = "s,rb")
    public boolean tban(CommandContext ctx, String playerName, String reason, boolean silent, boolean rollback)
    {
        final Player player = getPlayer(playerName);

        if (player == null)
        {
            msg(FreedomCommand.PLAYER_NOT_FOUND);
            return true;
        }

        final Location targetPos = player.getLocation();
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                final Location strikePos = new Location(targetPos.getWorld(), targetPos.getBlockX() + x, targetPos.getBlockY(), targetPos.getBlockZ() + z);
                targetPos.getWorld().strikeLightning(strikePos);
            }
        }

        if (!silent)
            FUtil.adminAction(sender.getName(), "Tempbanning: " + player.getName() + " for 5 minutes.", true);
        plugin.bm.addBan(Ban.forPlayer(player, sender, FUtil.parseDateOffset("5m"), reason));

        if (rollback)
        {
            plugin.cpb.rollback(player.getName());
        }

        final PlayerData data = plugin.pl.getData(player);
        assert data != null;
        data.setStrikes(data.getStrikes() + 1);

        player.kick(Component.text("You have been temporarily banned for five minutes. Please read totalfreedom.me for more info.", NamedTextColor.RED));

        return true;
    }

    @CommandDispatchTarget(pattern = "<playerName>", switches = "s,rb")
    public boolean tbanNoReason(CommandContext ctx, String playerName, boolean silent, boolean rollback)
    {
        return tban(ctx, playerName, "You have been temporarily banned for 5 minutes.", silent, rollback);
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
