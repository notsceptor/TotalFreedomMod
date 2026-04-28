package me.totalfreedom.totalfreedommod.blocking.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;

public class CommandBlocker extends FreedomService
{

    private final Pattern flagPattern = Pattern.compile("(:([0-9]){5,})");
    //
    private final Map<String, CommandBlockerEntry> entryList = Maps.newHashMap();
    private final List<String> unknownCommands = Lists.newArrayList();
    private List<String> serverCommandBlockedSubstrings = Lists.newArrayList();
    private long lastServerCommandBlockWarningTick = 0L;
    private long blockedServerCommandsSinceLastWarning = 0L;

    public CommandBlocker(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        load();
    }

    @Override
    protected void onStop()
    {
        entryList.clear();
    }

    public void load()
    {
        entryList.clear();
        unknownCommands.clear();
        loadServerCommandBlockerConfig();

        final CommandMap commandMap = getCommandMap();
        if (commandMap == null)
        {
            FLog.severe("Error loading commandMap.");
            return;
        }

        @SuppressWarnings("unchecked")
        List<String> blockedCommands = (List<String>) ConfigEntry.BLOCKED_COMMANDS.getList();
        for (String rawEntry : blockedCommands)
        {
            final String[] parts = rawEntry.split(":");
            if (parts.length < 3 || parts.length > 4)
            {
                FLog.warning("Invalid command blocker entry: " + rawEntry);
                continue;
            }

            final CommandBlockerRank rank = CommandBlockerRank.fromToken(parts[0]);
            final CommandBlockerAction action = CommandBlockerAction.fromToken(parts[1]);
            String commandName = parts[2].toLowerCase().substring(1);
            final String message = (parts.length > 3 ? parts[3] : null);

            if (rank == null || action == null || commandName == null || commandName.isEmpty())
            {
                FLog.warning("Invalid command blocker entry: " + rawEntry);
                continue;
            }

            final String[] commandParts = commandName.split(" ");
            String subCommand = null;
            if (commandParts.length > 1)
            {
                commandName = commandParts[0];
                subCommand = StringUtils.join(commandParts, " ", 1, commandParts.length).trim().toLowerCase();
            }

            final Command command = commandMap.getCommand(commandName);

            // Obtain command from alias
            if (command == null)
            {
                unknownCommands.add(commandName);
            }
            else
            {
                commandName = command.getName().toLowerCase();
            }

            String entryKey = subCommand != null ? commandName + " " + subCommand : commandName;

            if (entryList.containsKey(entryKey))
            {
                FLog.warning("Not blocking: /" + entryKey + " - Duplicate entry exists!");
                continue;
            }

            final CommandBlockerEntry blockedCommandEntry = new CommandBlockerEntry(rank, action, commandName, subCommand, message);
            entryList.put(entryKey, blockedCommandEntry);

            if (command != null)
            {
                for (String alias : command.getAliases())
                {
                    String aliasKey = subCommand != null ? alias.toLowerCase() + " " + subCommand : alias.toLowerCase();
                    entryList.put(aliasKey, blockedCommandEntry);
                }
            }
        }

        FLog.info("Loaded " + blockedCommands.size() + " blocked commands (" + (blockedCommands.size() - unknownCommands.size()) + " known).");
    }

    private void loadServerCommandBlockerConfig()
    {
        serverCommandBlockedSubstrings = Lists.newArrayList();

        @SuppressWarnings("unchecked")
        List<String> blockedSubstrings = (List<String>) ConfigEntry.BLOCK_SERVER_COMMANDS_BLOCKED_SUBSTRINGS.getList();
        if (blockedSubstrings != null)
        {
            for (String token : blockedSubstrings)
            {
                if (token == null)
                {
                    continue;
                }
                final String trimmed = token.trim();
                if (!trimmed.isEmpty())
                {
                    serverCommandBlockedSubstrings.add(trimmed.toLowerCase());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        // Blocked commands
        if (isCommandBlocked(event.getMessage(), event.getPlayer(), true))
        {
            // CommandBlocker handles messages and broadcasts
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event)
    {
        if (!Boolean.TRUE.equals(ConfigEntry.BLOCK_SERVER_COMMANDS_ENABLED.getBoolean()))
        {
            return;
        }

        final CommandSender sender = event.getSender();
        if (sender instanceof Player)
        {
            return;
        }
        if (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)
        {
            return;
        }

        final boolean blockAtNamedSenders = Boolean.TRUE.equals(ConfigEntry.BLOCK_SERVER_COMMANDS_BLOCK_AT_NAMED_SENDERS.getBoolean());
        final boolean blockCommandBlockHolders = Boolean.TRUE.equals(ConfigEntry.BLOCK_SERVER_COMMANDS_BLOCK_COMMAND_BLOCK_HOLDERS.getBoolean());

        final boolean isAtNamedSender = "@".equals(sender.getName());
        final boolean isCommandBlockHolder = sender instanceof BlockCommandSender || sender instanceof CommandMinecart;

        final boolean shouldApply =
                (blockAtNamedSenders && isAtNamedSender)
                        || (blockCommandBlockHolders && isCommandBlockHolder);

        if (!shouldApply)
        {
            return;
        }

        final String rawCommand = event.getCommand();
        if (rawCommand == null || rawCommand.isEmpty())
        {
            return;
        }

        if (!matchesServerCommandBlocklist(rawCommand))
        {
            return;
        }

        event.setCancelled(true);

        if (Boolean.TRUE.equals(ConfigEntry.BLOCK_SERVER_COMMANDS_LOG_THROTTLED_WARNINGS.getBoolean()))
        {
            blockedServerCommandsSinceLastWarning++;

            final long intervalTicks = Math.max(1, ConfigEntry.BLOCK_SERVER_COMMANDS_LOG_INTERVAL_TICKS.getInteger());
            final long nowTick = server.getCurrentTick();
            if (lastServerCommandBlockWarningTick == 0L || nowTick - lastServerCommandBlockWarningTick >= intervalTicks)
            {
                FLog.warning("[TFM] Blocked " + blockedServerCommandsSinceLastWarning
                        + " server-side command(s) from " + sender.getClass().getSimpleName()
                        + " (\"" + sender.getName() + "\"). Last: " + rawCommand);

                lastServerCommandBlockWarningTick = nowTick;
                blockedServerCommandsSinceLastWarning = 0L;
            }
        }
    }

    private boolean matchesServerCommandBlocklist(String command)
    {
        if (serverCommandBlockedSubstrings.isEmpty())
        {
            return false;
        }

        String normalized = command.trim();
        if (normalized.startsWith("/"))
        {
            normalized = normalized.substring(1);
        }
        normalized = normalized.toLowerCase();

        for (String token : serverCommandBlockedSubstrings)
        {
            if (normalized.contains(token))
            {
                return true;
            }
        }

        return false;
    }

    public boolean isCommandBlocked(String command, CommandSender sender)
    {
        return isCommandBlocked(command, sender, false);
    }

    public boolean isCommandBlocked(String command, CommandSender sender, boolean doAction)
    {
        if (command == null || command.isEmpty())
        {
            return false;
        }

        // Format
        command = command.toLowerCase().trim();
        command = command.startsWith("/") ? command.substring(1) : command;

        // Check for plugin specific commands
        final String[] commandParts = command.split(" ");
        if (commandParts[0].contains(":"))
        {
            if (doAction)
            {
                FUtil.playerMsg(sender, "Plugin specific commands are disabled.");
            }
            return true;
        }

        for (String part : commandParts)
        {
            Matcher matcher = flagPattern.matcher(part);
            if (!matcher.matches())
            {
                continue;
            }
            if (doAction)
            {
                FUtil.playerMsg(sender, "That command contains an illegal number: " + matcher.group(1));
            }
            return true;
        }

        // Obtain sub command, if it exists
        String subCommand = null;
        if (commandParts.length > 1)
        {
            subCommand = StringUtils.join(commandParts, " ", 1, commandParts.length).toLowerCase();
        }

        // Obtain entry
        CommandBlockerEntry entry = null;
        
        // Try from full subcommand to single argument
        if (subCommand != null)
        {
            String[] subParts = subCommand.split(" ");
            for (int i = subParts.length; i >= 1 && entry == null; i--)
            {
                String partialKey = commandParts[0] + " " + StringUtils.join(subParts, " ", 0, i);
                entry = entryList.get(partialKey);
            }
        }
        
        // Fall back to base command only (for entries without subcommands)
        if (entry == null)
        {
            entry = entryList.get(commandParts[0]);
        }
        
        if (entry == null)
        {
            return false;
        }

        // Validate sub command
        if (entry.getSubCommand() != null)
        {
            if (subCommand == null || !subCommand.startsWith(entry.getSubCommand()))
            {
                return false;
            }
        }

        if (entry.getRank().hasPermission(sender))
        {
            return false;
        }

        if (doAction)
        {
            entry.doActions(sender);
        }

        return true;
    }

    private CommandMap getCommandMap()
    {
        try
        {
            // Try Paper API first (available in Paper 1.20+)
            try
            {
                java.lang.reflect.Method getCommandMapMethod = server.getClass().getMethod("getCommandMap");
                return (CommandMap) getCommandMapMethod.invoke(server);
            }
            catch (NoSuchMethodException e)
            {
                // Fall back to reflection on PluginManager
            }
            
            // Fallback: access via SimplePluginManager reflection
            java.lang.reflect.Field commandMapField = org.bukkit.plugin.SimplePluginManager.class.getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(server.getPluginManager());
        }
        catch (Exception ex)
        {
            FLog.severe("Could not get command map: " + ex.getMessage());
            return null;
        }
    }
}
