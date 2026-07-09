package me.totalfreedom.totalfreedommod.cmd.internal;

import java.util.List;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.cmd.CommandRegistry;
import me.totalfreedom.totalfreedommod.cmd.FCommand;

public final class CommandHolder
{
    private CommandHolder() {}

    /**
     * Registers a {@link FCommand} with the server command dispatcher.
     * Must be called from plugin startup so the Brigadier node is queued before Paper's {@code COMMANDS} lifecycle event fires.
     */
    public static void register(FCommand command)
    {
        CommandRegistry.store(command);
        CommandProcessor.register(command, TotalFreedomMod.plugin());
    }

    /**
     * Returns the aliases declared on the
     * {@link me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command @Command}
     * annotation for the given root command name, or an empty list if no match is found.
     */
    public static List<String> listAliases(String commandName)
    {
        return CommandRegistry.listAliases(commandName);
    }

    /**
     * Returns the subcommand literal values declared via
     * {@link me.totalfreedom.totalfreedommod.cmd.internal.annotation.Subcommand @Subcommand}
     * on the registered command, or an empty list if no match is found.
     */
    public static List<String> listSubcommands(String commandName)
    {
        return CommandRegistry.listSubcommands(commandName);
    }
}
