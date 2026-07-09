package me.totalfreedom.totalfreedommod.cmd;

import java.util.ArrayList;
import java.util.List;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.bukkit.Server;
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

    protected final TotalFreedomMod plugin = TotalFreedomMod.plugin();
    protected final Server server = plugin.getServer();

    protected boolean isConsole(CommandSender sender)
    {
        return !(sender instanceof Player);
    }

    protected void checkConsole(CommandSender sender)
    {
        if (!isConsole(sender))
        {
            throw new CommandFailException("This command can only be used from the console.");
        }
    }

    protected void checkPlayer(CommandSender sender)
    {
        if (isConsole(sender))
        {
            throw new CommandFailException("This command can only be used by players.");
        }
    }

    protected void checkRank(CommandSender sender, Rank rank)
    {
        if (!plugin.rm.getRank(sender).isAtLeast(rank))
        {
            noPerms();
        }
    }

    protected boolean noPerms()
    {
        throw new CommandFailException("You do not have permission to use this command.");
    }

    protected Player getPlayer(String name)
    {
        if (name == null || name.isEmpty())
        {
            return null;
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

        return null;
    }

    protected void msg(final CommandSender sender, final String message, final NamedTextColor color)
    {
        if (sender == null || message == null)
        {
            return;
        }
        msg(sender, FUtil.colorizeWithLinks(message, color));
    }

    protected void msg(final CommandSender sender, final String message)
    {
        msg(sender, message, NamedTextColor.GRAY);
    }

    protected void msg(final CommandSender sender, final Component component)
    {
        if (sender == null || component == null)
        {
            return;
        }

        if (!(sender instanceof Player))
        {
            sender.sendMessage(ANSIComponentSerializer.ansi().serialize(component));
            return;
        }

        sender.sendMessage(component);
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
