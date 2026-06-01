package me.totalfreedom.totalfreedommod.ssh;

import lombok.experimental.Delegate;
import org.bukkit.command.CommandSender;

public final class AttributedConsoleSender implements CommandSender
{

    private interface Excluded
    {
        String getName();
    }

    @Delegate(excludes = Excluded.class)
    private final CommandSender delegate;

    private final String displayName;

    public AttributedConsoleSender(CommandSender delegate, String displayName)
    {
        this.delegate = delegate;
        this.displayName = displayName;
    }

    @Override
    public String getName()
    {
        return displayName;
    }
}
