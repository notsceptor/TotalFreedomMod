package me.totalfreedom.totalfreedommod.command;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;

/**
 * Handles command discovery, registration, and execution.
 * Replaces Aero's SimpleCommandHandler.
 */
public class CommandHandler<T extends TotalFreedomMod>
{

    private final T plugin;
    private final Map<String, CommandExecutor> executors;
    private String commandClassPrefix = "Command_";
    private String permissionMessage = ChatColor.RED + "You do not have permission to use this command.";
    private String onlyConsoleMessage = ChatColor.RED + "This command can only be used from the console.";
    private String onlyPlayerMessage = ChatColor.RED + "This command can only be used by players.";
    private CommandExecutorFactory executorFactory;

    public CommandHandler(T plugin)
    {
        this.plugin = plugin;
        this.executors = new HashMap<>();
    }

    /**
     * Sets the prefix for command class names (default: "Command_").
     */
    public void setCommandClassPrefix(String prefix)
    {
        this.commandClassPrefix = prefix;
    }

    /**
     * Sets the permission denied message.
     */
    public void setPermissionMessage(String message)
    {
        this.permissionMessage = message;
    }

    /**
     * Sets the "only console" message.
     */
    public void setOnlyConsoleMessage(String message)
    {
        this.onlyConsoleMessage = message;
    }

    /**
     * Sets the "only player" message.
     */
    public void setOnlyPlayerMessage(String message)
    {
        this.onlyPlayerMessage = message;
    }

    /**
     * Sets the executor factory for creating command executors.
     */
    public void setExecutorFactory(CommandExecutorFactory factory)
    {
        this.executorFactory = factory;
    }

    /**
     * Gets the permission denied message.
     */
    public String getPermissionMessage()
    {
        return permissionMessage;
    }

    /**
     * Gets the "only console" message.
     */
    public String getOnlyConsoleMessage()
    {
        return onlyConsoleMessage;
    }

    /**
     * Gets the "only player" message.
     */
    public String getOnlyPlayerMessage()
    {
        return onlyPlayerMessage;
    }

    /**
     * Gets all registered executors.
     */
    public Map<String, CommandExecutor> getExecutors()
    {
        return new HashMap<>(executors);
    }

    /**
     * Clears all registered commands.
     */
    public void clearCommands()
    {
        executors.clear();
    }

    /**
     * Discovers and loads commands from a package.
     * Scans for classes with the command prefix that extend AbstractCommandBase.
     *
     * @param packageObj The package to scan
     * @return Number of commands loaded
     */
    public int loadFrom(Package packageObj)
    {
        String packageName = packageObj.getName();
        int loaded = 0;

        try
        {
            // Use plugin's classloader, not Package.class's classloader (which can be null)
            ClassLoader classLoader = plugin.getClass().getClassLoader();
            String packagePath = packageName.replace('.', '/');
            
            java.io.File codeSource = new java.io.File(
                plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
            );

            if (codeSource.isFile())
            {
                // JAR file
                try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(codeSource))
                {
                    java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements())
                    {
                        java.util.jar.JarEntry entry = entries.nextElement();
                        String name = entry.getName();

                        if (name.startsWith(packagePath) && name.endsWith(".class") && !name.contains("$"))
                        {
                            String className = name.substring(0, name.length() - 6).replace('/', '.');
                            try
                            {
                                Class<?> clazz = classLoader.loadClass(className);
                                if (isCommandClass(clazz))
                                {
                                    loadCommand(clazz);
                                    loaded++;
                                }
                            }
                            catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError ex)
                            {
                                // Skip classes that can't be loaded
                                FLog.warning("Could not load command class " + className + ": " + ex.getMessage());
                            }
                        }
                    }
                }
            }
            else if (codeSource.isDirectory())
            {
                // Development environment - scan directory
                java.io.File packageDir = new java.io.File(codeSource, packagePath);
                if (packageDir.exists() && packageDir.isDirectory())
                {
                    scanDirectory(packageDir, packageName, classLoader);
                    loaded = executors.size(); // Count what was loaded
                }
            }
        }
        catch (Exception ex)
        {
            FLog.warning("Error loading commands from package " + packageName + ": " + ex.getMessage());
            ex.printStackTrace();
        }

        return loaded;
    }

    /**
     * Recursively scans a directory for command classes.
     */
    private void scanDirectory(java.io.File directory, String packageName, ClassLoader classLoader)
    {
        java.io.File[] files = directory.listFiles();
        if (files == null)
        {
            return;
        }

        for (java.io.File file : files)
        {
            if (file.isDirectory())
            {
                scanDirectory(file, packageName + "." + file.getName(), classLoader);
            }
            else if (file.getName().endsWith(".class") && !file.getName().contains("$"))
            {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                try
                {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (isCommandClass(clazz))
                    {
                        loadCommand(clazz);
                    }
                }
                catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError ex)
                {
                    // Skip classes that can't be loaded
                }
            }
        }
    }

    /**
     * Checks if a class is a command class.
     */
    private boolean isCommandClass(Class<?> clazz)
    {
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()))
        {
            return false;
        }

        if (!clazz.getSimpleName().startsWith(commandClassPrefix))
        {
            return false;
        }

        if (!AbstractCommandBase.class.isAssignableFrom(clazz))
        {
            return false;
        }

        return true;
    }

    /**
     * Loads a single command class.
     */
    @SuppressWarnings("unchecked")
    private void loadCommand(Class<?> commandClass)
    {
        try
        {
            // Create command instance
            Constructor<?> constructor = commandClass.getConstructor();
            AbstractCommandBase<T> command = (AbstractCommandBase<T>) constructor.newInstance();
            command.setPlugin(plugin);
            command.setHandler(this);

            // Get command name from class name (remove prefix)
            String commandName = commandClass.getSimpleName().substring(commandClassPrefix.length()).toLowerCase();

            // Create executor
            CommandExecutor executor = null;
            if (executorFactory != null)
            {
                executor = executorFactory.newExecutor(this, commandName, command);
            }
            else
            {
                executor = new FreedomCommandExecutor(plugin, this, commandName, command);
            }

            executors.put(commandName, executor);
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to load command " + commandClass.getName() + ": " + ex.getMessage());
        }
    }

    /**
     * Registers all loaded commands with Bukkit.
     *
     * @param pluginName The plugin name
     * @param fallbackPrefix The fallback prefix
     */
    public void registerAll(String pluginName, boolean fallbackPrefix)
    {
        org.bukkit.command.CommandMap commandMap = plugin.getServer().getCommandMap();
        
        for (Map.Entry<String, CommandExecutor> entry : executors.entrySet())
        {
            String commandName = entry.getKey();
            CommandExecutor executor = entry.getValue();

            try
            {
                // Get aliases before creating the command
                List<String> aliases = new ArrayList<>();
                if (executor instanceof FreedomCommandExecutor)
                {
                    FreedomCommand cmd = ((FreedomCommandExecutor) executor).getCommand();
                    if (cmd != null && cmd.getParams() != null)
                    {
                        String aliasString = cmd.getParams().aliases();
                        if (aliasString != null && !aliasString.isEmpty())
                        {
                            for (String alias : aliasString.split(","))
                            {
                                aliases.add(alias.trim());
                            }
                        }
                    }
                }

                PluginCommand pluginCommand = plugin.getCommand(commandName);
                if (pluginCommand == null)
                {
                    // Command not in plugin.yml - create dynamically
                    try
                    {
                        Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
                        constructor.setAccessible(true);
                        pluginCommand = constructor.newInstance(commandName, plugin);
                        
                        // Set aliases BEFORE registering
                        if (!aliases.isEmpty())
                        {
                            pluginCommand.setAliases(aliases);
                        }
                        
                        // Register the command with aliases
                        commandMap.register(pluginName, pluginCommand);
                    }
                    catch (Exception ex)
                    {
                        FLog.warning("Could not register command " + commandName + ": " + ex.getMessage());
                        continue;
                    }
                }
                else
                {
                    // Command exists in plugin.yml, but we still need to register aliases
                    if (!aliases.isEmpty())
                    {
                        pluginCommand.setAliases(aliases);
                        // Re-register aliases manually
                        for (String alias : aliases)
                        {
                            commandMap.register(alias, pluginName, pluginCommand);
                        }
                    }
                }

                pluginCommand.setExecutor(executor);

                // Setup command if it's a FreedomCommandExecutor
                if (executor instanceof FreedomCommandExecutor)
                {
                    ((FreedomCommandExecutor) executor).setupCommand(pluginCommand);
                }
            }
            catch (Exception ex)
            {
                FLog.warning("Error registering command " + commandName + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Factory interface for creating command executors.
     */
    public interface CommandExecutorFactory
    {
        CommandExecutor newExecutor(CommandHandler<?> handler, String name, AbstractCommandBase<?> command);
    }
}

