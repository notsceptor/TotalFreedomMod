package me.totalfreedom.totalfreedommod.command.resolver;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ArgumentResolutionException extends RuntimeException
{
    private final Component message;

    public ArgumentResolutionException(String message)
    {
        super(message);
        this.message = Component.text(message, NamedTextColor.RED);
    }

    public ArgumentResolutionException(Component message)
    {
        super();
        this.message = message;
    }

    public ArgumentResolutionException(Exception ex)
    {
        super(ex);
        this.message = Component.text(ex.getMessage());
    }

    public Component getFormattedMessage()
    {
        return message;
    }
}
