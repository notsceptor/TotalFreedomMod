package me.totalfreedom.totalfreedommod.command;

import lombok.Getter;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.command.resolver.ArgumentResolutionException;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchContext;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchSession;
import me.totalfreedom.totalfreedommod.ssh.AttributedConsoleSender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public abstract class FreedomCommand extends AbstractCommandBase<TotalFreedomMod>
{
    public static final String YOU_ARE_OP = "\u00A7eYou are now op!";
    public static final String YOU_ARE_NOT_OP = "\u00A7eYou are no longer op!";
    public static final String NOT_FROM_CONSOLE = "This command may not be used from the console.";
    public static final String PLAYER_NOT_FOUND = "\u00A77Player not found!";
    //
    private static final String SIMPLE_ARGUMENT_PATTERN = "\\S+";
    private static final String VARIABLE_LENGTH_ARGUMENT_PATTERN = ".+";
    private static final String VARIABLE_LENGTH_ARGUMENT_ENDING = "..>";
    //
    @Getter
    private final CommandParameters params;
    @Getter
    private final CommandPermissions perms;
    private final List<CommandDispatchTargetMeta> dispatchers;

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

        this.dispatchers = buildDispatchers();
    }

    private final List<CommandDispatchTargetMeta> buildDispatchers()
    {
        final String commandName = getClass().getSimpleName();

        final List<CommandDispatchTargetMeta> dispatchers = new ArrayList<>();

        // Go through all the methods in the class
        for (final Method method : getClass().getMethods())
        {
            // Only consider methods that have a CommandDispatchTarget annotation
            final CommandDispatchTarget ci = method.getAnnotation(CommandDispatchTarget.class);
            if (ci == null)
                continue;

            final String[] items = ci.pattern().split(" ");

            final String[] resolverNames = new String[items.length];
            final String[] resolverStrategies = new String[items.length];

            // Convert the annotation's pattern into a regular expression:
            //  - <param> are converted to [^ ]+
            //  - <param..> are converted to .+
            // Everything else remains the same
            // Also map resolvers to the indices of their corresponding patterns
            for (int i = 0; i < items.length; ++i)
            {
                final String it = items[i];

                if (it.startsWith("<") && it.endsWith(">"))
                {
                    final boolean vararg = it.endsWith(VARIABLE_LENGTH_ARGUMENT_ENDING);
                    // Get what's inside the angle brackets
                    final String innerContent = it.substring(1, it.length() - (vararg ? VARIABLE_LENGTH_ARGUMENT_ENDING.length() : 1));
                    final String[] innerData = innerContent.split(":");
                    String resolverName = null;
                    String resolverStrategy = null;
                    if (innerData.length >= 2)
                        resolverName = innerData[1];
                    if (innerData.length >= 3)
                        resolverStrategy = innerData[2];
                    items[i] = vararg ? VARIABLE_LENGTH_ARGUMENT_PATTERN : SIMPLE_ARGUMENT_PATTERN;
                    resolverNames[i] = resolverName;
                    resolverStrategies[i] = resolverStrategy;
                    continue;
                }

                // Ensure regex metacharacters are escaped
                items[i] = it.replaceAll("[\\W]", "\\\\$0");
            }

            // Determine if the ending parameter has an ellipsis
            final Optional<Integer> ellipsis = items.length != 0 && items[items.length - 1].equals(VARIABLE_LENGTH_ARGUMENT_PATTERN) ?
                Optional.of(items.length - 1) :
                Optional.empty();

            // Confirm there are no other ellipsis arguments elsewhere in the pattern
            if (items.length != 0 && IntStream.range(0, items.length - 1)
                .anyMatch(i -> items[i].equals(VARIABLE_LENGTH_ARGUMENT_PATTERN)))
            {
                FLog.warning(String.format("Command %s has a pattern handler that contains an variable-length argument not at the end of the list, skipping...", commandName));
                continue;
            }

            // Construct the final regular expression pattern
            final String regex = String.format("^%s$", StringUtils.join(items, " "));

            // Create the pattern to match against
            final Pattern pattern = Pattern.compile(regex);

            // Get the switches
            final String[] switches = !ci.switches().isEmpty() ? ci.switches().split(",") : new String[0];

            // Insert into the map
            dispatchers.add(new CommandDispatchTargetMeta(pattern, Arrays.asList(switches), ci.priority(), method, ellipsis, resolverNames, resolverStrategies));
        }

        dispatchers.sort((a, b) -> b.priority.compareTo(a.priority));

        return dispatchers;
    }

    private final boolean dispatchCommand(CommandContext ctx, final String[] args)
    {
        final String commandName = getClass().getSimpleName();
        final String joinedArgs = StringUtils.join(args, " ");

        // Find the dispatch target, default to the normal run function if it can't find one
        final CommandDispatchTargetMeta cd = findDispatchTarget(args);
        if (cd == null)
        {
            FLog.debug(String.format("Command %s does not have a pattern handler for the arguments: \"%s\", defaulting to runner",
                commandName,
                joinedArgs));
            return run(ctx.getSender(), ctx.getPlayerSender(), ctx.getCommand(), ctx.getCommandLabel(), args, ctx.isSenderConsole());
        }

        // Arguments passed to the dispatch target:
        // 1 for the command context
        // 1 for each [^ ]+ or .+ in the dispatch target pattern

        final List<Object> passedArgs = new ArrayList<>(Arrays.asList(ctx));
        final boolean[] passedSwitches = new boolean[cd.switches.size()];

        if (args.length != 0)
        {
            final String patternStr = cd.pattern.toString();
            final String[] patternParts = patternStr.substring(1, patternStr.length() - 1).split(" ");
            final int _ = IntStream.range(0, args.length)
                // We're using a fold (reduce) here to maintain state for where we are in the pattern (j),
                // alongside where we are in the argument list (i)
                .reduce(0, (j, i) -> {
                    final String arg = args[i];
                    // If we encounter what looks like a switch
                    if (arg.startsWith("-"))
                    {
                        // Loop over all the switches and see if this is one of them
                        for (int k = 0; k < cd.switches.size(); ++k)
                        {
                            final String sw = cd.switches.get(k);
                            if (arg.substring(1).equals(sw))
                            {
                                // If so, track it and move on
                                passedSwitches[k] = true;
                                // Don't move forward in the pattern
                                return j;
                            }
                        }
                    }
                    // This just means that we hit a variable length argument pattern
                    if (j >= patternParts.length)
                        return j;
                    final String patternPart = patternParts[j];
                    String content = null;
                    // Add the rest of the arguments if we encounter an ellipsis
                    if (patternPart.equals(VARIABLE_LENGTH_ARGUMENT_PATTERN))
                        content = StringUtils.join((List<String>) Arrays.stream(args)
                            .skip(i)
                            .filter(a -> !cd.switches.contains(a.replaceAll("^-", "")))
                            .collect(Collectors.toCollection(ArrayList::new)), " ");
                    // Add the current argument if there's a placeholder here
                    else if (patternPart.equals(SIMPLE_ARGUMENT_PATTERN))
                        content = arg;
                    // Skip adding anything to the arguments list if there's no pattern
                    if (content == null)
                        return j + 1;
                    // Otherwise, attempt to resolve the argument if it's of a different type
                    Object passedArg = content;
                    final String resolverName = cd.resolverNames[j];
                    final String resolverStrategy = cd.resolverStrategies[j];
                    if (resolverName != null)
                    {
                        passedArg = plugin.cl.getHandler().resolveArgument(resolverName, content, resolverStrategy);
                        FLog.debug(String.format("Argument '%s' in '%s' command resolved to '%s'",
                            arg,
                            commandName,
                            passedArg));
                    }
                    passedArgs.add(passedArg);
                    // Move forward in the pattern
                    return j + 1;
                });
        }

        // Concatenate all switch values to the end
        for (final boolean passedSwitch : passedSwitches)
            passedArgs.add(passedSwitch);
    
        try
        {
            // Invoke the function with the arguments
            FLog.debug(String.format("For command %s: chose dispatch function %s with arguments: %s", commandName, cd.method.getName(), passedArgs));
            final Object ret = cd.method.invoke(this, passedArgs.toArray());
            if (ret instanceof Boolean)
                return (Boolean) ret;
            FLog.warning(String.format("Command %s's pattern handler for the arguments \"%s\" does not return a boolean value",
                commandName,
                joinedArgs));
        }
        catch (IllegalAccessException e)
        {
            FLog.warning(String.format("Command %s's pattern handler for the arguments \"%s\" is inaccessible by the dispatcher",
                commandName,
                joinedArgs));
        }
        catch (IllegalArgumentException e)
        {
            FLog.warning(String.format("Command %s's pattern handler for the arguments \"%s\" does not have the correct prototype for the pattern",
                commandName,
                joinedArgs));
        }
        catch (InvocationTargetException e)
        {
            final Throwable cause = e.getCause();
            if (cause instanceof CommandFailException cfe)
            {
                throw cfe;
            }
            if (cause instanceof RuntimeException rte)
            {
                throw rte;
            }
            FLog.severe(String.format("Exception in pattern handler for command %s:", commandName));
            FLog.severe(e);
        }
        msg("The command you requested could not be executed, please report this issue accordingly. Additional details have been logged to the console.");
        return true;
    }

    private final CommandDispatchTargetMeta findDispatchTarget(final String[] args)
    {
        // Just try to find a pattern that matches the arguments
        for (CommandDispatchTargetMeta cd : dispatchers)
        {
            final Pattern pat = cd.pattern;
            final String sanitizedArgs = StringUtils.join((List<String>) Arrays.stream(args)
                .filter(a -> !cd.switches.contains(a.replaceAll("^-", "")))
                .collect(Collectors.toCollection(ArrayList::new)), " ");
            if (!pat.matcher(sanitizedArgs).matches())
                continue;
            return cd;
        }
        return null;
    }

    @Override
    public final boolean runCommand(final CommandSender sender, final Command command, final String label, final String[] args)
    {
        CommandSender effectiveSender = resolveSender(sender);
        setVariables(effectiveSender, command, label, args);
        final CommandContext ctx = new CommandContext(effectiveSender, playerSender, command, label, isConsole());

        try
        {
            return dispatchCommand(ctx, args);
        }
        catch (CommandFailException | ArgumentResolutionException ex)
        {
            msg(ex.getMessage());
            return true;
        }
        catch (Exception ex)
        {
            FLog.severe("Uncaught exception executing command: " + command.getName());
            FLog.severe(ex);
            msg("Command error: " + (ex.getMessage() == null ? "Unknown cause" : ex.getMessage()), NamedTextColor.RED);
            return true;
        }
    }

    protected abstract boolean run(final CommandSender sender, final Player playerSender, final Command cmd, final String commandLabel, final String[] args, final boolean senderIsConsole);

    private static CommandSender resolveSender(CommandSender raw)
    {
        RemoteDispatchSession session = RemoteDispatchContext.getActiveSession();
        if (session != null && !(raw instanceof Player))
        {
            return new AttributedConsoleSender(raw, session.getDisplayName());
        }
        return raw;
    }

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
        return FUtil.colorizeWithLinks(message, color);
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
            target.sendMessage(FUtil.colorizeWithLinks(message));
        }
    }
    
    protected void msg(final Player target, final String message, final NamedTextColor color)
    {
        if (target != null && message != null)
        {
            target.sendMessage(FUtil.colorizeWithLinks(message, color));
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
        if (command == null)
        {
            return null;
        }
        return CommandHandler.getByName(command.getName());
    }

    private class CommandDispatchTargetMeta
    {
        private final Pattern pattern;
        private final List<String> switches;
        private final DispatchTargetPriority priority;
        private final Method method;
        private final Optional<Integer> ellipsis;
        private final String[] resolverNames;
        private final String[] resolverStrategies;

        private CommandDispatchTargetMeta(Pattern pattern,
            List<String> switches,
            DispatchTargetPriority priority,
            Method method,
            Optional<Integer> ellipsis,
            String[] resolverNames,
            String[] resolverStrategies)
        {
            this.pattern = pattern;
            this.switches = switches;
            this.priority = priority;
            this.method = method;
            this.ellipsis = ellipsis;
            this.resolverNames = resolverNames;
            this.resolverStrategies = resolverStrategies;
        }
    }
}
