package me.totalfreedom.totalfreedommod.command;

import lombok.Getter;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.pravian.aero.command.AbstractCommandBase;
import net.pravian.aero.util.Players;
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
    
    // Manual getters - Lombok @Getter not processing reliably
    public CommandParameters getParams()
    {
        return params;
    }
    
    public CommandPermissions getPerms()
    {
        return perms;
    }

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
        return Players.getPlayer(name);
    }

    protected void msg(final CommandSender sender, final String message, final ChatColor color)
    {
        if (sender == null || message == null)
        {
            return;
        }
        String coloredMessage = (color != null ? color : "") + message;
        String ampersandMessage = coloredMessage.replace('§', '&');
        Component component = me.totalfreedom.totalfreedommod.util.AdventureUtil.legacyToComponent(ampersandMessage);
        sender.sendMessage(component);
    }

    protected void msg(final String message, final ChatColor color)
    {
        msg(sender, message, color);
    }

    protected void msg(final CommandSender sender, final String message)
    {
        msg(sender, message, ChatColor.GRAY);
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
    
    protected void msg(final Player target, final String message, final ChatColor color)
    {
        if (target != null && message != null)
        {
            String coloredMessage = (color != null ? color : "") + message;
            target.sendMessage(me.totalfreedom.totalfreedommod.util.AdventureUtil.legacyToComponent(coloredMessage));
        }
    }
    
    protected void msg(final CommandSender sender, final Component component)
    {
        if (sender == null || component == null)
        {
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
            return (FreedomCommand) ((FreedomCommandExecutor) (((PluginCommand) command).getExecutor())).getCommandBase();
        }
        catch (Exception ex)
        {
            return null;
        }
    }
}
