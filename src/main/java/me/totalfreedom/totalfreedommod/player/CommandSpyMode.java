package me.totalfreedom.totalfreedommod.player;

import java.util.Locale;

public enum CommandSpyMode
{

    OFF,
    ADMINS,
    OPS,
    ALL;

    public static CommandSpyMode fromString(String value)
    {
        if (value == null)
        {
            return OFF;
        }

        try
        {
            return valueOf(value.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex)
        {
            return OFF;
        }
    }

    public String getName()
    {
        return name().toLowerCase(Locale.ROOT);
    }
}
