package me.totalfreedom.totalfreedommod.command;

import io.papermc.paper.command.brigadier.Commands;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;

/**
 * Handles command discovery, registration, and execution.
 * Replaces Aero's SimpleCommandHandler.
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandHandler<T extends TotalFreedomMod>
{

    private static final Map<String, FreedomCommand> COMMAND_REGISTRY = new ConcurrentHashMap<>();

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

    public static FreedomCommand getByName(String name)
    {
        if (name == null)
        {
            return null;
        }
        return COMMAND_REGISTRY.get(name.toLowerCase());
    }

    /**
     * Clears the static registry. Called on plugin disable via CommandLoader.onStop().
     */
    public static void clearRegistry()
    {
        COMMAND_REGISTRY.clear();
    }

    /**
     * Discovers and loads commands from a package by scanning the plugin JAR.
     *
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
        return AbstractCommandBase.class.isAssignableFrom(clazz);
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
            CommandExecutor executor;
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
     */
    public void registerAllWithLifecycle(Commands registrar)
    {
        for (Map.Entry<String, CommandExecutor> entry : executors.entrySet())
        {
            String commandName = entry.getKey();
            CommandExecutor executor = entry.getValue();

            if (!(executor instanceof FreedomCommandExecutor fce))
            {
                continue;
            }

            FreedomCommand cmd = fce.getCommand();
            String description = "";
            List<String> aliases = new ArrayList<>();

            if (cmd != null && cmd.getParams() != null)
            {
                description = cmd.getParams().description();
                String aliasString = cmd.getParams().aliases();
                if (aliasString != null && !aliasString.isEmpty())
                {
                    for (String alias : aliasString.split(","))
                    {
                        String trimmed = alias.trim();
                        if (!trimmed.isEmpty())
                        {
                            aliases.add(trimmed);
                        }
                    }
                }
            }

            if (cmd != null)
            {
                COMMAND_REGISTRY.put(commandName, cmd);
                for (String alias : aliases)
                {
                    COMMAND_REGISTRY.put(alias.toLowerCase(), cmd);
                }
            }

            FreedomBasicCommand basicCommand = new FreedomBasicCommand(fce, commandName);

            try
            {
                registrar.register(
                    commandName,
                    description,
                    aliases,
                    basicCommand
                );
            }
            catch (Exception ex)
            {
                FLog.warning("Failed to register command " + commandName + ": " + ex.getMessage());
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

