package me.totalfreedom.totalfreedommod.command;

import lombok.Getter;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import java.util.ArrayList;
import java.util.List;
import me.totalfreedom.totalfreedommod.command.AbstractCommandBase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

public abstract class FreedomCommand extends AbstractCommandBase<TotalFreedomMod>
{

    public static final String YOU_ARE_OP = ChatColor.YELLOW + "You are now op!";
    public static final String YOU_ARE_NOT_OP = ChatColor.YELLOW + "You are no longer op!";
    public static final String NOT_FROM_CONSOLE = "This command may not be used from the console.";
    public static final String PLAYER_NOT_FOUND = ChatColor.GRAY + "Player not found!";
    //
    @Getter
    private final CommandParameters params;
    @Getter
    private final CommandPermissions perms;

    public FreedomCommand()
    {
        this.params = getClass().getAnnotation(CommandParameters.class);
        if (params == null)
        {
            FLog.warning("Ignoring command usage for command " + getClass().getSimpleName() + ". Command is not annotated!");
        }

        this.perms = getClass().getAnnotation(CommandPermissions.class);
        if (perms == null)
        {
            FLog.warning("Ignoring permissions for command " + getClass().getSimpleName() + ". Command is not annotated!");
        }
    }

    @Override
    public final boolean runCommand(final CommandSender sender, final Command command, final String label, final String[] args)
    {
        setVariables(sender, command, label, args);

        try
        {
            return run(sender, playerSender, command, label, args, isConsole());
        }
        catch (CommandFailException ex)
        {
            msg(ex.getMessage());
            return true;
        }
        catch (Exception ex)
        {
            FLog.severe("Uncaught exception executing command: " + command.getName());
            FLog.severe(ex);
            msg("Command error: " + (ex.getMessage() == null ? "Unknown cause" : ex.getMessage()), ChatColor.RED);
            return true;
        }
    }

    protected abstract boolean run(final CommandSender sender, final Player playerSender, final Command cmd, final String commandLabel, final String[] args, final boolean senderIsConsole);

    protected void checkConsole()
    {
        if (!isConsole())
        {
            throw new CommandFailException(getHandler().getOnlyConsoleMessage());
        }
    }

    protected void checkPlayer()
    {
        if (isConsole())
        {
            throw new CommandFailException(getHandler().getOnlyPlayerMessage());
        }
    }

    protected void checkNotHostConsole()
    {
        if (isConsole() && FUtil.isFromHostConsole(sender.getName()))
        {
            throw new CommandFailException("This command can not be used from the host console.");
        }
    }

    protected void checkRank(Rank rank)
    {
        if (!plugin.rm.getRank(sender).isAtLeast(rank))
        {
            noPerms();
        }
    }

    protected boolean noPerms()
    {
        throw new CommandFailException(getHandler().getPermissionMessage());
    }

    protected boolean isConsole()
    {
        return !(sender instanceof Player);
    }

    protected Player getPlayer(String name)
    {
        if (name == null || name.isEmpty())
        {
            return null;
        }

        // Try exact match first
        Player player = server.getPlayerExact(name);
        if (player != null)
        {
            return player;
        }

        // Try case-insensitive match
        name = name.toLowerCase();
        for (Player p : server.getOnlinePlayers())
        {
            if (p.getName().toLowerCase().equals(name))
            {
                return p;
            }
        }

        // Try partial match
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

    /**
     * Builds a Component from a message string, handling embedded color codes and color parameter.
     * 
     * @param message The message string (may contain embedded color codes)
     * @param color The color parameter to apply (prepended if message has embedded colors)
     * @return Component with all colors applied
     */
    private Component buildComponent(String message, NamedTextColor color)
    {
        // Process message: prepend color parameter if message has embedded colors
        String processedMessage = message;
        if (message.contains("§") || message.contains("&"))
        {
            if (color != null)
            {
                // Prepend the color parameter code to the message so it applies to the beginning
                ChatColor chatColor = AdventureUtil.namedTextColorToChatColor(color);
                if (chatColor != null)
                {
                    processedMessage = chatColor + message;
                }
            }
        }
        
        // Build Component from processed message
        if (processedMessage.contains("§") || processedMessage.contains("&"))
        {
            // Message has embedded color codes, convert them to Component
            return AdventureUtil.legacyToComponent(processedMessage);
        }
        else if (color != null)
        {
            // No embedded colors, apply the color parameter
            return Component.text(message).color(color);
        }
        else
        {
            // No colors at all
            return Component.text(message);
        }
    }

    protected void msg(final CommandSender sender, final String message, final NamedTextColor color)
    {
        if (sender == null || message == null)
        {
            return;
        }
        
        // Build Component the same way for both console and players
        Component component = buildComponent(message, color);
        
        if (!(sender instanceof Player))
        {
            String ansiMessage = ANSIComponentSerializer.ansi().serialize(component);
            sender.sendMessage(ansiMessage);
            return;
        }
        
        sender.sendMessage(component);
    }

    protected void msg(final String message, final NamedTextColor color)
    {
        msg(sender, message, color);
    }

    @Deprecated
    protected void msg(final CommandSender sender, final String message, final ChatColor color)
    {
        if (sender == null || message == null)
        {
            return;
        }
        NamedTextColor namedColor = color != null ? AdventureUtil.chatColorToNamedTextColor(color) : null;
        msg(sender, message, namedColor);
    }

    @Deprecated
    protected void msg(final String message, final ChatColor color)
    {
        msg(sender, message, color);
    }

    protected void msg(final CommandSender sender, final String message)
    {
        msg(sender, message, NamedTextColor.GRAY);
    }

    protected void msg(final String message)
    {
        msg(sender, message);
    }
    
    protected void msg(final Player target, final String message)
    {
        if (target != null && message != null)
        {
            target.sendMessage(me.totalfreedom.totalfreedommod.util.AdventureUtil.legacyToComponent(message));
        }
    }
    
    protected void msg(final Player target, final String message, final NamedTextColor color)
    {
        if (target != null && message != null)
        {
            Component component = Component.text(message);
            if (color != null)
            {
                component = component.color(color);
            }
            target.sendMessage(component);
        }
    }

    @Deprecated
    protected void msg(final Player target, final String message, final ChatColor color)
    {
        if (target != null && message != null)
        {
            NamedTextColor namedColor = color != null ? me.totalfreedom.totalfreedommod.util.AdventureUtil.chatColorToNamedTextColor(color) : null;
            msg(target, message, namedColor);
        }
    }
    
    protected void msg(final CommandSender sender, final Component component)
    {
        if (sender == null || component == null)
        {
            return;
        }
        
        if (!(sender instanceof Player))
        {
            String ansiMessage = ANSIComponentSerializer.ansi().serialize(component);
            sender.sendMessage(ansiMessage);
            return;
        }
        
        sender.sendMessage(component);
    }

    protected void msg(final Component component)
    {
        msg(sender, component);
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

    public static FreedomCommand getFrom(Command command)
    {
        try
        {
            FreedomCommandExecutor executor = (FreedomCommandExecutor) (((PluginCommand) command).getExecutor());
            return executor != null ? executor.getCommand() : null;
        }
        catch (Exception ex)
        {
            return null;
        }
    }
}
