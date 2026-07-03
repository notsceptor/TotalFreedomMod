package me.totalfreedom.totalfreedommod.rank;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

@Getter
public enum Rank implements Displayable
{

    IMPOSTOR("an", "Impostor", Type.PLAYER, "Imp", NamedTextColor.YELLOW),
    NON_OP("a", "Non-Op", Type.PLAYER, "", NamedTextColor.GREEN),
    OP("an", "Op", Type.PLAYER, "OP", NamedTextColor.RED),
    SUPER_ADMIN("a", "Super Admin", Type.ADMIN, "SA", NamedTextColor.AQUA),
    SENIOR_ADMIN("a", "Senior Admin", Type.ADMIN, "SrA", NamedTextColor.GOLD),
    /**
     * @deprecated use {@link #SENIOR_ADMIN} directly; console-class senders now resolve to admin ranks via {@code ConsoleSenderRegistry}.
     */
    @Deprecated
    SENIOR_CONSOLE("the", "Console", Type.ADMIN_CONSOLE, "Console", NamedTextColor.DARK_PURPLE);
    private final Type type;
    private final String name;
    private final String determiner;
    private final String tag;
    private final Component coloredTag;
    private final NamedTextColor color;

    private Rank(String determiner, String name, Type type, String abbr, NamedTextColor color)
    {
        this.type = type;
        this.name = name;
        this.determiner = determiner;
        this.tag = abbr.isEmpty() ? "" : "[" + abbr + "]";
        this.color = color;

        // Build colored tag as Component
        if (abbr.isEmpty())
        {
            this.coloredTag = Component.empty();
        }
        else
        {
            this.coloredTag = Component.text("[")
                    .color(NamedTextColor.DARK_GRAY)
                    .append(Component.text(abbr).color(color))
                    .append(Component.text("]").color(NamedTextColor.DARK_GRAY))
                    .append(Component.text("").color(color));
        }
    }

    @Override
    public Component getColoredName()
    {
        return Component.text(name).color(color);
    }

    @Override
    public Component getColoredLoginMessage()
    {
        return Component.text(determiner + " ")
                .append(Component.text(name).color(color).decorate(TextDecoration.ITALIC));
    }

    public boolean isConsole()
    {
        return getType() == Type.ADMIN_CONSOLE;
    }

    public int getLevel()
    {
        return ordinal();
    }

    public boolean isAtLeast(Rank rank)
    {
        if (getLevel() < rank.getLevel())
        {
            return false;
        }

        if (!hasConsoleVariant() || !rank.hasConsoleVariant())
        {
            return true;
        }

        return getConsoleVariant().getLevel() >= rank.getConsoleVariant().getLevel();
    }

    public boolean isAdmin()
    {
        return getType() == Type.ADMIN || getType() == Type.ADMIN_CONSOLE;
    }
    
    // Manual getters - Lombok @Getter not processing on enum fields
    public Type getType()
    {
        return type;
    }
    
    public String getName()
    {
        return name;
    }
    
    @Override
    public Component getColoredTag()
    {
        return coloredTag;
    }
    
    @Override
    public NamedTextColor getColor()
    {
        return color;
    }

    @Deprecated
    public boolean hasConsoleVariant()
    {
        return getConsoleVariant() != null;
    }

    @Deprecated
    public Rank getConsoleVariant()
    {
        switch (this)
        {
            case SENIOR_ADMIN:
            case SENIOR_CONSOLE:
                return SENIOR_CONSOLE;
            default:
                return null;
        }
    }

    @Deprecated
    public Rank getPlayerVariant()
    {
        switch (this)
        {
            case SENIOR_ADMIN:
            case SENIOR_CONSOLE:
                return SENIOR_ADMIN;
            default:
                return null;
        }
    }

    public static Rank findRank(String string)
    {
        try
        {
            return Rank.valueOf(string.toUpperCase());
        }
        catch (Exception ignored)
        {
        }

        return Rank.NON_OP;
    }

    public static enum Type
    {

        PLAYER,
        ADMIN,
        ADMIN_CONSOLE;

        public boolean isAdmin()
        {
            return this != PLAYER;
        }
    }

}
