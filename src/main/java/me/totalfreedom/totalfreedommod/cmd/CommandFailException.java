package me.totalfreedom.totalfreedommod.cmd;

import net.kyori.adventure.text.Component;

/**
 * Thrown from a command handler to abort execution and message the sender.
 * The dispatcher catches this, sends {@link #getComponentMessage()} to the sender,
 * and treats the command as handled.
 */
public class CommandFailException extends RuntimeException
{

    private static final long serialVersionUID = -92333791173124L;

    private final transient Component component;

    public CommandFailException(String message)
    {
        super(message);
        this.component = Component.text(message);
    }

    public CommandFailException(Component component)
    {
        super(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component));
        this.component = component;
    }

    public Component getComponentMessage()
    {
        return component;
    }
}
