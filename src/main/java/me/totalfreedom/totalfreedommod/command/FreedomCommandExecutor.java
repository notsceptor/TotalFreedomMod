package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.rank.CustomRank;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchContext;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchSession;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

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

    FreedomCommand getCommand()
    {
        return commandBase instanceof FreedomCommand ? (FreedomCommand) commandBase : null;
    }

    void executePaper(CommandSender sender, String label, String[] args)
    {
        if (!hasPermission(sender, true))
        {
            return;
        }
        Command command = plugin.getServer().getPluginCommand(label);
        if (command == null)
        {
            command = plugin.getServer().getPluginCommand(name);
        }
        if (command == null)
        {
            command = new FallbackCommand(name);
        }
        try
        {
            boolean handled = commandBase.runCommand(sender, command, label, args);
            if (!handled)
            {
                FreedomCommand freedomCommand = getCommand();
                if (freedomCommand != null && freedomCommand.getParams() != null)
                {
                    String usage = freedomCommand.getParams().usage();
                    if (usage != null && !usage.isEmpty())
                    {
                        sender.sendMessage(Component.text("Usage: " + usage.replace("<command>", label), NamedTextColor.RED));
                    }
                }
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Unhandled command exception: " + label);
            FLog.severe(ex);
            sender.sendMessage(Component.text("Unhandled Command Error: " + label, NamedTextColor.RED));
        }
    }

    List<String> tabComplete(CommandSender sender, String label, String[] args)
    {
        if (!hasPermission(sender, false))
        {
            return List.of();
        }

        try
        {
            return commandBase.tabComplete(sender, label, args);
        }
        catch (Exception ex)
        {
            FLog.warning("Unhandled tab-completion exception: " + label);
            FLog.warning(ex);
            return List.of();
        }
    }

    @Override
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
            FLog.severe("Unhandled command exception: " + command.getName());
            FLog.severe(ex);
            sender.sendMessage(Component.text("Unhandled Command Error: " + command.getName(), NamedTextColor.RED));
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

        RemoteDispatchSession dispatch = RemoteDispatchContext.getActiveSession();
        if (dispatch != null)
        {
            if (dispatch.isIdentified())
            {
                String permNode = switch (dispatch.getChannel())
                {
                    case SSH -> "tfm.manage.ssh";
                    case DISCORD -> "tfm.manage.discord";
                };
                String channelLabel = switch (dispatch.getChannel())
                {
                    case SSH -> "SSH";
                    case DISCORD -> "Discord";
                };
                if (!plugin.rm.hasPermission(sender, permNode))
                {
                    if (sendMsg)
                    {
                        sender.sendMessage(Component.text("You do not have permission to run commands via " + channelLabel + ".", NamedTextColor.RED));
                    }
                    return false;
                }
            }
        }
        else if (!(sender instanceof Player) && plugin.al.getEntryByName(sender.getName()) != null)
        {
            if (!plugin.rm.hasPermission(sender, "tfm.manage.telnet"))
            {
                if (sendMsg)
                {
                    sender.sendMessage(Component.text("You do not have permission to run commands via telnet.", NamedTextColor.RED));
                }
                return false;
            }
        }

        final Player player = sender instanceof Player ? (Player) sender : null;

        if (perms.source() == SourceType.ONLY_CONSOLE && player != null)
        {
            if (sendMsg)
            {
                sender.sendMessage(handler.getOnlyConsoleMessage());
            }
            return false;
        }

        if (perms.source() == SourceType.ONLY_IN_GAME && player == null)
        {
            if (sendMsg)
            {
                sender.sendMessage(handler.getOnlyPlayerMessage());
            }
            return false;
        }

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

        Rank rank = plugin.rm.getRank(sender);
        String boundRankId = plugin.csr.getRankIdForSender(sender.getName());
        CustomRank boundCustom = boundRankId != null ? plugin.rm.getCustomRank(boundRankId) : null;
        boolean result;
        if (boundCustom != null)
        {
            result = boundCustom.isAtLeast(perms.level());
        }
        else
        {
            result = rank.isAtLeast(perms.level());
        }
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

    private static final class FallbackCommand extends Command
    {
        private FallbackCommand(String name)
        {
            super(name);
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args)
        {
            return false;
        }
    }
}
