package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Temporarily bans a player for five minutes.", usage = "/<command> [-s] [-rb] <player> [reason]", aliases = "noob")
public class Command_tban extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<player> <reason..>", switches = "s,rb")
    public boolean tban(CommandContext ctx, String playerName, String reason, boolean silent, boolean rollback)
    {
        final Player player = getPlayer(playerName);
        final PlayerData data = BanCommandUtil.getData(plugin, playerName, player);
        final String name = BanCommandUtil.getCanonicalName(playerName, player, data);

        if (player != null)
        {
            final Location targetPos = player.getLocation();
            for (int x = -1; x <= 1; x++)
            {
                for (int z = -1; z <= 1; z++)
                {
                    final Location strikePos = new Location(targetPos.getWorld(), targetPos.getBlockX() + x, targetPos.getBlockY(), targetPos.getBlockZ() + z);
                    targetPos.getWorld().strikeLightning(strikePos);
                }
            }
        }

        if (!silent)
        {
            FUtil.adminAction(sender.getName(), "Tempbanning " + name + " for 5 minutes", true);

            if (reason != null)
            {
                FUtil.bcastMsg("  Reason: " + reason, NamedTextColor.YELLOW);
            }

            plugin.db.sendActionMessage(sender.getName(), name, reason, ConfigEntry.DISCORD_PLAYER_TBAN_MESSAGE);
        }

        final Ban ban = BanCommandUtil.createFullBan(name, BanCommandUtil.getIps(player, data), sender, FUtil.parseDateOffset("5m"), reason);
        plugin.bm.addBan(ban);

        if (rollback)
        {
            plugin.cpb.rollback(name);
        }

        if (data != null && plugin.al.getEntryByName(name) == null)
        {
            data.setStrikes(data.getStrikes() + 1);
            plugin.pl.saveData(data);
        }

        if (player != null)
        {
            player.kick(ban.bakeKickMessage());
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "<player>", switches = "s,rb")
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