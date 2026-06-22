package me.totalfreedom.totalfreedommod.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import org.bukkit.command.CommandSender;

@SuppressWarnings("UnstableApiUsage")
public class FreedomBasicCommand implements BasicCommand
{

    private final FreedomCommandExecutor executor;
    private final String name;

    FreedomBasicCommand(FreedomCommandExecutor executor, String name)
    {
        this.executor = executor;
        this.name = name;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args)
    {
        executor.executePaper(stack.getSender(), name, args);
    }

    @Override
    public boolean canUse(CommandSender sender)
    {
        return executor.hasPermission(sender, false);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args)
    {
        return executor.tabComplete(stack.getSender(), name, args);
    }
}
