package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.strike")
@CommandParameters(description = "Manages the strikes for a player.", usage = "/<command> <add | remove | clear> <player>", aliases = "strike")
public class Command_strikes extends FreedomCommand
{
    public boolean modify(final CommandContext ctx, final String playerName, int mod)
    {
        final Player player = Bukkit.getPlayer(playerName);
        if (player == null)
        {
            msg(FreedomCommand.PLAYER_NOT_FOUND);
            return true;
        }
        if (plugin.al.isAdmin(player))
        {
            msg("Strikes may not be modified for admins.");
            return true;
        }
        final PlayerData data = plugin.pl.getData(player);
        assert data != null;
        final int newValue = data.getStrikes() + mod;
        if (newValue < 0 || newValue > PlayerData.MAX_STRIKES)
        {
            msg(String.format("%s may only have between 0 and %s strikes.", player.getName(), PlayerData.MAX_STRIKES));
            return true;
        }
        data.setStrikes(newValue);
        msg(String.format("%s now has %s strike%s.",
            player.getName(),
            data.getStrikes(),
            data.getStrikes() != 1 ? "s" : ""));
        return true;
    }

    @CommandDispatchTarget(pattern = "add <playerName>")
    public boolean add(final CommandContext ctx, final String playerName)
    {
        return modify(ctx, playerName, 1);
    }

    @CommandDispatchTarget(pattern = "remove <playerName>")
    public boolean remove(final CommandContext ctx, final String playerName)
    {
        return modify(ctx, playerName, -1);
    }

    @CommandDispatchTarget(pattern = "clear <playerName>")
    public boolean clear(final CommandContext ctx, final String playerName)
    {
        final Player player = Bukkit.getPlayer(playerName);
        if (player == null)
        {
            msg(FreedomCommand.PLAYER_NOT_FOUND);
            return true;
        }
        if (plugin.al.isAdmin(player))
        {
            msg("Strikes may not be modified for admins.");
            return true;
        }
        final PlayerData data = plugin.pl.getData(player);
        assert data != null;
        data.setStrikes(0);
        msg(String.format("%s now has no strikes.", player.getName()));
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
