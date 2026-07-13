package me.totalfreedom.totalfreedommod.command;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Temporarily bans an online or previously known player.", usage = "/<command> <player> [duration] [reason] [-rb]")
public class Command_tempban extends FreedomCommand
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

    @CommandDispatchTarget(pattern = "<playerName>", switches = "rb")
    public boolean tempBanPlayer(CommandContext ctx, String player, boolean rollback)
    {
        return tempBanPlayer(ctx, player, FUtil.parseDateOffset("30m"), null, rollback);
    }

    @CommandDispatchTarget(pattern = "<playerName> <duration:DateOffset>", switches = "rb")
    public boolean tempBanPlayer(CommandContext ctx, String player, Date offset, boolean rollback)
    {
        return tempBanPlayer(ctx, player, offset, null, rollback);
    }

    @CommandDispatchTarget(pattern = "<playerName> <duration:DateOffset> <reason..>", switches = "rb")
    public boolean tempBanPlayer(CommandContext ctx, String player, Date offset, String reason, boolean rollback)
    {
        final Player actualPlayer = //(Player) plugin.cl.getHandler().resolveArgument("Player", player, null);
        null;
        final PlayerData playerData = BanCommandUtil.getData(plugin, player, actualPlayer);
        final Ban ban;
        final String name;

        // If we can't find a player here, we'll just fumble a solution with names.
        if (actualPlayer == null && playerData == null)
        {
            ban = Ban.forPlayerName(player, ctx.getSender(), offset, reason);
            name = player;
        }
        else
        {
            name = BanCommandUtil.getCanonicalName(player, actualPlayer, playerData);
            List<String> ips = BanCommandUtil.getIps(actualPlayer, playerData);
            ban = BanCommandUtil.createFullBan(name, ips, ctx.getPlayerSender(), offset, reason);
        }

        FUtil.adminAction(ctx.getSender().getName(),
                "Temporarily banning " + name + " until " + DATE_FORMAT.format(offset), true);

        plugin.bm.addBan(ban);

        server.getOnlinePlayers().stream()
                .filter(suspect -> suspect.equals(actualPlayer) ||
                        ban.getIps().contains(Objects.requireNonNull(suspect.getAddress()).getAddress().getHostAddress()))
                .forEach(target ->
                {
                    Location loc = target.getLocation();
                    for (int x = -1; x <= 1; x++)
                    {
                        for (int z = -1; z <= 1; z++)
                        {
                            loc.getWorld().strikeLightning(new Location(loc.getWorld(),
                                    loc.getBlockX() + x, loc.getBlockY(), loc.getBlockZ() + z));
                        }
                    }
                    target.kick(ban.bakeKickMessage());
                });

        if (rollback)
        {
            plugin.cpb.rollback(name);
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
