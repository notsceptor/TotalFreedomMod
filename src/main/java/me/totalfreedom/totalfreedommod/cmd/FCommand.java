package me.totalfreedom.totalfreedommod.cmd;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.totalfreedom.totalfreedommod.PluginProvider;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Base class for command declarations in the new command framework.
 * <p>
 * Subclasses declare their metadata with
 * {@link me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command @Command} /
 * {@link me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission @Permission}
 * and their handlers with
 * {@link me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback @Callback} +
 * {@link me.totalfreedom.totalfreedommod.cmd.internal.annotation.Subcommand @Subcommand}.
 * <p>
 * Unlike FreedomCommand, there is no per-invocation mutable state. Handlers receive the sender as
 * their first parameter, so every helper takes the sender explicitly.
 */
public abstract class FCommand
{

    public static final Component YOU_ARE_OP = Component.text("You are now op!", NamedTextColor.YELLOW);
    public static final Component YOU_ARE_NOT_OP = Component.text("You are no longer op!", NamedTextColor.YELLOW);
    public static final Component NOT_FROM_CONSOLE = Component.text("This command may not be used from the console.", NamedTextColor.GRAY);
    public static final Component PLAYER_NOT_FOUND = Component.text("Player not found!", NamedTextColor.GRAY);

    protected final TotalFreedomMod plugin = PluginProvider.get();
    protected final Server server = plugin.getServer();

    @Deprecated
    protected boolean isConsole(CommandSender sender)
    {
        return !(sender instanceof Player);
    }

    @Deprecated
    protected void checkConsole(CommandSender sender)
    {
        if (!isConsole(sender))
        {
            throw new CommandFailException("This command can only be used from the console.");
        }
    }

    @Deprecated
    protected void checkPlayer(CommandSender sender)
    {
        if (isConsole(sender))
        {
            throw new CommandFailException("This command can only be used by players.");
        }
    }

    @Deprecated
    protected void checkRank(CommandSender sender, Rank rank)
    {
        if (!plugin.rm.getRank(sender).isAtLeast(rank))
        {
            noPerms();
        }
    }

    protected void adminAction(CommandSender sender, String action, Object... refs)
    {
        FUtil.adminAction(sender, MessageUtils.parse(String.format(action, refs)));
    }

    protected void adminAction(CommandSender sender, String action, TagResolver... resolvers)
    {
        FUtil.adminAction(sender, MessageUtils.parse(action, resolvers));
    }

    protected ConfigEntry getConfigEntry(final String value)
    {
        return ConfigEntry.valueOf(value.toUpperCase(Locale.ROOT));
    }

    @Deprecated
    protected boolean noPerms()
    {
        throw new CommandFailException("You do not have permission to use this command.");
    }

    protected Player getPlayer(String name)
    {
        if (name == null || name.isEmpty())
        {
            throw new InvalidParameterException("String cannot be Null-or-Empty");
        }

        Player player = server.getPlayerExact(name);
        if (player != null)
        {
            return player;
        }

        name = name.toLowerCase();
        for (Player p : server.getOnlinePlayers())
        {
            if (p.getName().toLowerCase().equals(name))
            {
                return p;
            }
        }

        List<Player> matches = new ArrayList<>();
        for (Player p : server.getOnlinePlayers())
        {
            if (p.getName().toLowerCase().startsWith(name))
            {
                matches.add(p);
            }
        }

        if (matches.size() == 1)
        {
            return matches.get(0);
        }

        throw new CommandFailException("That player cannot be found!");
    }

    protected void msg(final CommandSender sender, final String message, Object... refs)
    {
        MessageUtils.send(sender, String.format(message, refs));
    }

    protected void msg(final CommandSender sender, final String message, TagResolver... refs)
    {
        MessageUtils.send(sender, message, refs);
    }

    protected void kickPlayer(final Player player, final String message)
    {
        player.kick(MessageUtils.parse(message));
    }

    protected void smitePlayer(final Player player)
    {
        final Location targetPos = player.getLocation();
        final World world = player.getWorld();
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                final Location strike_pos = new Location(world, targetPos.getBlockX() + x, targetPos.getBlockY(), targetPos.getBlockZ() + z);
                world.strikeLightning(strike_pos);
            }
        }
    }

    protected boolean isAdmin(CommandSender sender)
    {
        return plugin.al.isAdmin(sender);
    }

    protected Admin getAdmin(CommandSender sender)
    {
        return plugin.al.getAdmin(sender);
    }

    protected Admin getAdmin(Player player)
    {
        return plugin.al.getAdmin(player);
    }

    protected PlayerData getData(Player player)
    {
        return plugin.pl.getData(player);
    }
}
