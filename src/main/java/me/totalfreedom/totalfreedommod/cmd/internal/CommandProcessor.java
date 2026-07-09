package me.totalfreedom.totalfreedommod.cmd.internal;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.cmd.CommandFailException;
import me.totalfreedom.totalfreedommod.cmd.FCommand;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Completer;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Cooldown;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Greedy;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Resolve;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Subcommand;
import me.totalfreedom.totalfreedommod.cmd.resolver.AbstractArgumentResolver;
import me.totalfreedom.totalfreedommod.cmd.resolver.ArgumentResolutionException;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Builds Brigadier command node trees from {@link FCommand} declarations and wires them
 * into the server command dispatcher via Paper's {@code LifecycleEvents.COMMANDS}.
 * 
 * <h3>Registration flow</h3>
 * <ol>
 *   <li>{@link #register(FCommand, TotalFreedomMod)} creates a processor per command and
 *       stores it; the first registration hooks the plugin's {@code COMMANDS} lifecycle event.
 *   <li>Every time the lifecycle event fires (startup and reloads), all stored processors
 *       are (re-)registered with the server dispatcher.
 * </ol>
 *
 * <h3>Handlers</h3>
 * A method annotated {@link Callback} is a handler. With a {@link Subcommand}, it hangs off
 * that literal path; without one (or with an empty path), it is the command's <em>root</em>
 * handler. Replaces abstract run() method.
 *
 * <h3>Nested subcommands</h3>
 * {@link Subcommand#value()} is a space-separated literal path.
 * Methods are grouped into a trie keyed by path segment before the
 * Brigadier tree is built, so declarations sharing a prefix like {@code "set"} and
 * {@code "set default"} merge into one branch. A node may carry both a handler and children at once.
 *
 * <h3>Permissions</h3>
 * {@link Permission} on the {@code FCommand} class gates the whole command and is wired into
 * the root node's {@code requires()}. On a handler method, it overrides the class-level gate for that subcommand. 
 * Permission enforcement runs through {@link PermissionGate}.
 *
 * <h3>Arguments</h3>
 * Handler parameters after the optional leading sender map positionally to Brigadier argument
 * nodes: 
 * <li>Scalars and enums via {@link ArgumentResolver}</li> 
 * <li>{@code Player} via Paper's player selector</li>
 * <li>Any type registered in {@link ResolverRegistry}</li> 
 * <li>Any type annotated with {@link Resolve}</li>. 
 * A trailing {@code String} annotated {@link Greedy} consumes the rest of the line.
 *
 * <h3>Argument chain order (Brigadier constraint)</h3>
 * Argument nodes are built innermost-first: the leaf carries {@code executes()}; each parent
 * wraps the next via {@code .then()}. {@link #attachHandler} iterates right-to-left.
 *
 * <h3>Tab completion</h3>
 * {@link Completer} methods are collected up front into a {@code (subcommand path, position)}
 * lookup. When building the argument at that position, a matching completer is wired in via
 * {@code .suggests(...)}. 
 * <br/>
 * Positions with no explicit completer fall back to, in order:
 * <ol>
 *    <li>Candidates supplied by the type's {@link ResolverRegistry} registration</li>
 *    <li>Fuzzy-matched enum constant names</li>
 *    <li>Brigadier/Paper defaults</li>
 * </ol>
 */
public final class CommandProcessor
{

    private static final Map<String, CommandProcessor> commands = new ConcurrentHashMap<>();
    private static final AtomicBoolean hooked = new AtomicBoolean(false);

    /**
     * Creates a processor for {@code command}; the first call also registers the
     * {@code COMMANDS} lifecycle handler that flushes every stored processor into the
     * server dispatcher each time the event fires (startup and reloads).
     */
    public static void register(FCommand command, TotalFreedomMod plugin)
    {
        Command meta = command.getClass().getAnnotation(Command.class);
        if (meta == null)
        {
            FLog.warning(String.format("%s is missing @Command; skipped", command.getClass().getName()));
            return;
        }

        commands.put(meta.name(), new CommandProcessor(command, plugin, meta));
        FLog.debug(String.format("Queued /%s for Brigadier registration", meta.name()));

        if (hooked.compareAndSet(false, true))
        {
            plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
            {
                for (CommandProcessor p : commands.values())
                {
                    p.registerWith(event.registrar());
                }
            });
        }
    }

    public static void reset()
    {
        commands.clear();
    }

    private final FCommand command;
    private final TotalFreedomMod plugin;
    private final String commandName;
    private final String description;
    private final List<String> aliases;
    private final Permission classPermission;
    private final Map<CompleterKey, Method> completers = new HashMap<>();

    private CommandProcessor(FCommand command, TotalFreedomMod plugin, Command meta)
    {
        this.command = command;
        this.plugin = plugin;
        this.commandName = meta.name();
        this.description = meta.description();
        this.aliases = List.of(meta.aliases());
        this.classPermission = command.getClass().getAnnotation(Permission.class);
    }

    void registerWith(Commands registrar)
    {
        try
        {
            registrar.register(buildNode().build(), description, aliases);
            FLog.info(String.format("Registered /%s (aliases: %s)", commandName, aliases));
        }
        catch (Exception e)
        {
            FLog.severe(String.format("Failed to register /%s: \n%s", commandName, ExceptionUtils.getRootCauseMessage(e)));
        }
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildNode()
    {
        completers.clear();
        for (Method method : command.getClass().getDeclaredMethods())
        {
            if (!method.isAnnotationPresent(Completer.class)) continue;
            method.setAccessible(true);
            if (!isValidCompleterSignature(method))
            {
                FLog.warning(String.format("%s has @Completer but an invalid signature (expected (SenderType, String) -> List<String>); skipped.", method.getName()));
                continue;
            }
            Completer c = method.getAnnotation(Completer.class);
            completers.put(new CompleterKey(c.value(), c.position()), method);
        }

        SubcommandNode trie = new SubcommandNode(null);
        Method rootHandler = null;
        for (Method method : command.getClass().getDeclaredMethods())
        {
            if (!method.isAnnotationPresent(Callback.class))
            {
                continue;
            }
            method.setAccessible(true);
            String pathValue = method.isAnnotationPresent(Subcommand.class)
                ? method.getAnnotation(Subcommand.class).value().trim()
                : "";

            if (pathValue.isEmpty())
            {
                if (rootHandler != null)
                {
                    FLog.warning(String.format("Duplicate root handler on /%s:\n %s overrides %s", commandName, method.getName(), rootHandler.getName()));
                }
                rootHandler = method;
                continue;
            }

            SubcommandNode node = trie;
            for (String segment : pathValue.split("\\s+"))
            {
                node = node.children.computeIfAbsent(segment, SubcommandNode::new);
            }
            if (node.handlerMethod != null)
            {
                FLog.warning(String.format("Duplicate subcommand path \"%s\" on /%s:\n %s overrides %s", pathValue, commandName, method.getName(), node.handlerMethod.getName()));
            }
            node.handlerMethod = method;
        }

        LiteralArgumentBuilder<CommandSourceStack> root = LiteralArgumentBuilder.literal(commandName);
        if (classPermission != null)
        {
            root.requires(source -> PermissionGate.test(plugin, source.getSender(), classPermission, false));
        }
        for (SubcommandNode child : trie.children.values())
        {
            root.then(buildBranch(child));
        }
        if (rootHandler != null)
        {
            attachHandler(root, rootHandler);
        }
        return root;
    }

    /**
     * A node in the subcommand path trie; may carry a handler, children, or both.
     */
    private static final class SubcommandNode
    {
        final String literal;
        final Map<String, SubcommandNode> children = new LinkedHashMap<>();
        Method handlerMethod;

        SubcommandNode(String literal)
        {
            this.literal = literal;
        }
    }

    /**
     * Pairs a {@link Completer} to the subcommand path and argument position it applies to.
     * Root handlers use the empty path {@code ""}.
     */
    private record CompleterKey(String subcommandPath, int position) {}

    private LiteralArgumentBuilder<CommandSourceStack> buildBranch(SubcommandNode node)
    {
        LiteralArgumentBuilder<CommandSourceStack> branch = LiteralArgumentBuilder.literal(node.literal);
        for (SubcommandNode child : node.children.values())
        {
            branch.then(buildBranch(child));
        }
        if (node.handlerMethod != null)
        {
            attachHandler(branch, node.handlerMethod);
        }
        return branch;
    }

    private static boolean isValidCompleterSignature(Method method)
    {
        Class<?>[] types = method.getParameterTypes();
        return types.length == 2
            && ArgumentResolver.isSenderType(types[0])
            && types[1] == String.class
            && method.getReturnType() == List.class;
    }

    private static String subcommandPath(Method method)
    {
        return method.isAnnotationPresent(Subcommand.class)
            ? method.getAnnotation(Subcommand.class).value().trim()
            : "";
    }

    private SuggestionProvider<CommandSourceStack> buildSuggestionProvider(Method completerMethod)
    {
        return (ctx, builder) ->
        {
            CommandSender sender = ctx.getSource().getSender();
            if (!completerMethod.getParameterTypes()[0].isInstance(sender))
            {
                return builder.buildFuture();
            }
            try
            {
                @SuppressWarnings("unchecked")
                List<String> suggestions = (List<String>) completerMethod.invoke(command, sender, builder.getRemaining());
                for (String suggestion : suggestions)
                {
                    builder.suggest(suggestion);
                }
            }
            catch (Exception e)
            {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                FLog.severe(String.format("Error in completer %s: \n%s", completerMethod.getName(), ExceptionUtils.getRootCauseMessage(cause)));
            }
            return builder.buildFuture();
        };
    }

    /**
     * Default suggester for enum-typed arguments with no explicit {@link Completer} which fuzzy-matches
     * the partial input against the enum's constant names via {@link FuzzyMatch}.
     */
    private SuggestionProvider<CommandSourceStack> buildEnumSuggestionProvider(Class<?> enumType)
    {
        List<String> names = Arrays.stream(enumType.getEnumConstants())
            .map(c -> ((Enum<?>) c).name())
            .toList();
        return (ctx, builder) ->
        {
            for (String name : FuzzyMatch.filter(names, builder.getRemaining()))
            {
                builder.suggest(name);
            }
            return builder.buildFuture();
        };
    }

    /**
     * Default suggester for custom-resolved argument types whose {@link ResolverRegistry}
     * registration supplied a candidate list (e.g. Material registry keys).
     */
    private SuggestionProvider<CommandSourceStack> buildCandidateSuggestionProvider(Supplier<List<String>> candidates)
    {
        return (ctx, builder) ->
        {
            for (String name : FuzzyMatch.filter(candidates.get(), builder.getRemaining()))
            {
                builder.suggest(name);
            }
            return builder.buildFuture();
        };
    }

    /** 
     * @return true if the parameter routes through a custom resolver rather than a native Brigadier type. 
     */
    private static boolean isCustomResolved(Parameter param)
    {
        return param.isAnnotationPresent(Resolve.class) || ResolverRegistry.hasType(param.getType());
    }

    private static AbstractArgumentResolver<?> resolverFor(Parameter param)
    {
        Resolve ann = param.getAnnotation(Resolve.class);
        if (ann != null && !ann.value().isEmpty())
        {
            return ResolverRegistry.byName(ann.value());
        }
        return ResolverRegistry.byType(param.getType());
    }

    /**
     * Attaches a handler's argument chain (and/or {@code executes()}) onto its literal branch.
     */
    private void attachHandler(LiteralArgumentBuilder<CommandSourceStack> branch, Method method)
    {
        String subPath = subcommandPath(method);

        Parameter[] params = method.getParameters();
        boolean hasSender = params.length > 0 && ArgumentResolver.isSenderType(params[0].getType());
        int argStart = hasSender ? 1 : 0;

        Permission methodPermission = method.isAnnotationPresent(Permission.class)
            ? method.getAnnotation(Permission.class)
            : classPermission;

        com.mojang.brigadier.Command<CommandSourceStack> dispatch = ctx ->
        {
            CommandSourceStack source = ctx.getSource();
            CommandSender sender = PermissionGate.resolveSender(source.getSender());

            if (!PermissionGate.test(plugin, sender, methodPermission, true))
            {
                return 0;
            }

            if (hasSender && !params[0].getType().isInstance(sender))
            {
                sender.sendMessage(PermissionGate.ONLY_PLAYER_MESSAGE);
                return 0;
            }

            if (sender instanceof Player player && method.isAnnotationPresent(Cooldown.class))
            {
                Cooldown cooldownAnn = method.getAnnotation(Cooldown.class);
                String key = cooldownKey(commandName, subPath);
                if (CooldownManager.isOnCooldown(player.getUniqueId(), key))
                {
                    long remainingSec = (long) Math.ceil(
                        CooldownManager.remainingMillis(player.getUniqueId(), key) / 1000.0);
                    sender.sendMessage(Component.text(
                        cooldownAnn.message().replace("{remaining}", Long.toString(remainingSec))));
                    return 0;
                }
                CooldownManager.setCooldown(
                    player.getUniqueId(), key, cooldownAnn.unit().toMillis(cooldownAnn.value()));
            }

            Object[] invokeArgs = new Object[params.length];
            if (hasSender) invokeArgs[0] = sender;

            try
            {
                for (int i = argStart; i < params.length; i++)
                {
                    String paramName = params[i].getName();
                    Class<?> type = params[i].getType();

                    if (isCustomResolved(params[i]))
                    {
                        AbstractArgumentResolver<?> resolver = resolverFor(params[i]);
                        if (resolver == null)
                        {
                            sender.sendMessage(Component.text("Internal command error: no resolver for argument " + paramName, NamedTextColor.RED));
                            return 0;
                        }
                        Resolve ann = params[i].getAnnotation(Resolve.class);
                        String strategy = ann != null ? ann.strategy() : "";
                        String raw = ctx.getArgument(paramName, String.class);
                        invokeArgs[i] = resolver.resolve(raw, strategy);
                    }
                    else if (ArgumentResolver.isPlayerArgType(type))
                    {
                        var resolver = ctx.getArgument(paramName,
                            PlayerSelectorArgumentResolver.class);
                        List<Player> players = resolver.resolve(source);
                        if (players.isEmpty())
                        {
                            sender.sendMessage(Component.text("Player not found.", NamedTextColor.GRAY));
                            return 0;
                        }
                        invokeArgs[i] = players.get(0);
                    }
                    else if (type.isEnum())
                    {
                        String raw = ctx.getArgument(paramName, String.class);
                        try
                        {
                            invokeArgs[i] = parseEnum(type, raw);
                        }
                        catch (IllegalArgumentException e)
                        {
                            sender.sendMessage(Component.text("Invalid value '" + raw + "' for argument: " + paramName, NamedTextColor.RED));
                            return 0;
                        }
                    }
                    else
                    {
                        invokeArgs[i] = ctx.getArgument(paramName, type);
                    }
                }
            }
            catch (ArgumentResolutionException e)
            {
                sender.sendMessage(e.getFormattedMessage());
                return 0;
            }

            try
            {
                Object result = method.invoke(command, invokeArgs);
                if (result instanceof Boolean b) return b ? 1 : 0;
                return 1;
            }
            catch (InvocationTargetException e)
            {
                Throwable cause = e.getCause();
                if (cause instanceof CommandFailException cfe)
                {
                    sender.sendMessage(cfe.getComponentMessage());
                    return 0;
                }
                if (cause instanceof ArgumentResolutionException are)
                {
                    sender.sendMessage(are.getFormattedMessage());
                    return 0;
                }
                FLog.severe(String.format("Error in /%s %s: \n%s", commandName, subPath, ExceptionUtils.getRootCauseMessage(e)));
                sender.sendMessage(Component.text("Command error: " + (cause == null || cause.getMessage() == null ? "Unknown cause" : cause.getMessage()), NamedTextColor.RED));
                return 0;
            }
            catch (Exception e)
            {
                FLog.severe(String.format("Error in /%s %s: \n%s", commandName, subPath, ExceptionUtils.getRootCauseMessage(e)));
                return 0;
            }
        };

        RequiredArgumentBuilder<CommandSourceStack, ?> head = null;
        for (int i = params.length - 1; i >= argStart; i--)
        {
            int position = i - argStart;
            Parameter param = params[i];
            Class<?> type = param.getType();
            boolean greedy = param.isAnnotationPresent(Greedy.class);
            if (greedy && i != params.length - 1)
            {
                FLog.warning(String.format("@Greedy on non-final parameter '%s' of /%s %s is ignored", param.getName(), commandName, subPath));
                greedy = false;
            }

            RequiredArgumentBuilder<CommandSourceStack, ?> arg;
            if (greedy)
            {
                arg = RequiredArgumentBuilder.argument(param.getName(), StringArgumentType.greedyString());
            }
            else if (isCustomResolved(param))
            {
                arg = RequiredArgumentBuilder.argument(param.getName(), StringArgumentType.word());
            }
            else if (ArgumentResolver.isPlayerArgType(type))
            {
                arg = RequiredArgumentBuilder.argument(param.getName(), ArgumentTypes.player());
            }
            else
            {
                arg = RequiredArgumentBuilder.argument(param.getName(), (ArgumentType<?>) ArgumentResolver.resolve(type));
            }

            Method completer = completers.get(new CompleterKey(subPath, position));
            Supplier<List<String>> candidates = ResolverRegistry.suggestionsFor(type);
            if (completer != null)
            {
                arg.suggests(buildSuggestionProvider(completer));
            }
            else if (isCustomResolved(param) && candidates != null)
            {
                arg.suggests(buildCandidateSuggestionProvider(candidates));
            }
            else if (type.isEnum())
            {
                arg.suggests(buildEnumSuggestionProvider(type));
            }

            if (i == params.length - 1) arg.executes(dispatch);
            if (head != null) arg.then(head);
            head = arg;
        }

        if (head != null) branch.then(head);
        else branch.executes(dispatch);
    }

    // This works I promise lmfao
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseEnum(Class<?> enumType, String raw)
    {
        return Enum.valueOf((Class<Enum>) enumType, raw.toUpperCase());
    }

    public static String cooldownKey(String commandName, String subcommandValue)
    {
        return commandName + ":" + subcommandValue;
    }
}
