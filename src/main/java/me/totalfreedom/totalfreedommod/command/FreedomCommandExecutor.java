package me.totalfreedom.totalfreedommod.command;

import java.util.Arrays;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

public class FreedomCommandExecutor implements CommandExecutor
{

    private final TotalFreedomMod plugin;
    private final CommandHandler<?> handler;
    private final String name;
    private final AbstractCommandBase<?> commandBase;

    public FreedomCommandExecutor(TotalFreedomMod plugin, CommandHandler<?> handler, String name, AbstractCommandBase<?> command)
    {
        this.plugin = plugin;
        this.handler = handler;
        this.name = name;
        this.commandBase = command;
    }

    protected FreedomCommand getCommand()
    {
        return commandBase instanceof FreedomCommand ? (FreedomCommand) commandBase : null;
    }

    public void setupCommand(PluginCommand pluginCommand)
    {
        final FreedomCommand command = getCommand();
        if (command == null)
        {
            return;
        }

        final CommandParameters params = command.getParams();
        if (params == null)
        {
            return;
        }

        String aliasString = params.aliases();

        if (aliasString.length() > 0)
        {
            pluginCommand.setAliases(Arrays.asList(params.aliases().split(",")));
        }
        pluginCommand.setDescription(params.description());
        pluginCommand.setUsage(params.usage());

        // Check if permisions are correctly set up
        CommandPermissions perms = command.getPerms();
        if (perms != null)
        {
            if (perms.level().isConsole())
            {
                FLog.warning("[Command] " + pluginCommand.getName() + " - permission is set to a console rank, "
                        + "should be set to player variant with 'source = SourceType.ONLY_CONSOLE'");
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!hasPermission(sender, true))
        {
            return true;
        }

        try
        {
            return commandBase.runCommand(sender, command, label, args);
        }
        catch (Exception ex)
        {
            // If this is ever ran, TFM failed :
            FLog.severe("Unhandled command exception: " + command.getName());
            FLog.severe(ex);
            sender.sendMessage(ChatColor.RED + "Unhandled Command Error: " + command.getName());
            return true;
        }
    }

    public boolean hasPermission(CommandSender sender, boolean sendMsg)
    {
        final FreedomCommand command = getCommand();
        if (command == null)
        {
            return true;
        }

        final CommandPermissions perms = command.getPerms();
        if (perms == null)
        {
            return true;
        }

        // Block host console
        if (FUtil.isFromHostConsole(sender.getName())
                && perms.blockHostConsole())
        {
            if (sendMsg)
            {
                sender.sendMessage(handler.getPermissionMessage());
            }
            return false;
        }

        final Player player = sender instanceof Player ? (Player) sender : null;

        // Only console
        if (perms.source() == SourceType.ONLY_CONSOLE
                && player != null)
        {
            if (sendMsg)
            {
                sender.sendMessage(handler.getOnlyConsoleMessage());
            }
            return false;
        }

        // Only in game
        if (perms.source() == SourceType.ONLY_IN_GAME
                && player == null)
        {
            if (sendMsg)
            {
                sender.sendMessage(handler.getOnlyPlayerMessage());
            }
            return false;
        }

        // Check TFM permission if specified (custom rank system)
        String tfmPermission = perms.permission();
        if (tfmPermission != null && !tfmPermission.isEmpty())
        {
            boolean result = plugin.rm.hasPermission(sender, tfmPermission);
            if (!result && sendMsg)
            {
                sender.sendMessage(handler.getPermissionMessage());
            }
            return result;
        }

        // Fallback to legacy rank level check
        // Player permissions
        if (player != null)
        {
            Rank rank = plugin.rm.getRank(player);
            boolean result = rank.isAtLeast(perms.level());
            if (!result && sendMsg)
            {
                sender.sendMessage(handler.getPermissionMessage());
            }
            return result;
        }

        // Console permissions
        Rank rank = plugin.rm.getRank(sender);
        boolean result = rank.isAtLeast(perms.level());
        if (!result && sendMsg)
        {
            sender.sendMessage(handler.getPermissionMessage());
        }
        return result;
    }

    public static class FreedomExecutorFactory implements CommandHandler.CommandExecutorFactory
    {

        private final TotalFreedomMod plugin;

        public FreedomExecutorFactory(TotalFreedomMod plugin)
        {
            this.plugin = plugin;
        }

        @Override
        public CommandExecutor newExecutor(CommandHandler<?> handler, String name, AbstractCommandBase<?> command)
        {
            return new FreedomCommandExecutor(plugin, handler, name, command);
        }

    }

}
