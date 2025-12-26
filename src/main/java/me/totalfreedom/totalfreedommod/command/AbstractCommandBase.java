package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Base class for commands.
 * Provides common functionality for command execution.
 */
public abstract class AbstractCommandBase<T extends TotalFreedomMod>
{

    protected T plugin;
    protected org.bukkit.Server server;
    protected CommandSender sender;
    protected Command command;
    protected String label;
    protected String[] args;
    protected Player playerSender;
    protected CommandHandler<T> handler;

    /**
     * Sets the variables for command execution.
     * Called before runCommand() is invoked.
     */
    protected void setVariables(CommandSender sender, Command command, String label, String[] args)
    {
        this.sender = sender;
        this.command = command;
        this.label = label;
        this.args = args;
        this.playerSender = sender instanceof Player ? (Player) sender : null;
    }

    /**
     * Sets the command handler.
     * Called during command registration.
     */
    protected void setHandler(CommandHandler<T> handler)
    {
        this.handler = handler;
    }

    /**
     * Sets the plugin instance.
     * Called during command registration.
     */
    protected void setPlugin(T plugin)
    {
        this.plugin = plugin;
        this.server = plugin != null ? plugin.getServer() : null;
    }

    /**
     * Gets the command handler.
     */
    protected CommandHandler<T> getHandler()
    {
        return handler;
    }

    /**
     * Checks if the sender is the console.
     */
    protected boolean isConsole()
    {
        return !(sender instanceof Player);
    }

    /**
     * Executes the command.
     * This is called by the command executor.
     *
     * @param sender The command sender
     * @param command The command
     * @param label The command label used
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    public abstract boolean runCommand(CommandSender sender, Command command, String label, String[] args);
}

