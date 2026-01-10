package me.totalfreedom.totalfreedommod.rank;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;

public enum Title implements Displayable
{

    DEVELOPER("a", "Developer", NamedTextColor.DARK_PURPLE, "Dev"),
    OWNER("the", "Owner", NamedTextColor.BLUE, "Owner"),
    EXECUTIVE("an", "Executive", NamedTextColor.YELLOW, "Exec");

    private final String determiner;
    @Getter
    private final String name;
    @Getter
    private final String tag;
    @Getter
    private final Component coloredTag;
    private final NamedTextColor color;
    private final ChatColor colorLegacy; // For backward compatibility

    private Title(String determiner, String name, NamedTextColor color, String tag)
    {
        this.determiner = determiner;
        this.name = name;
        this.tag = "[" + tag + "]";
        this.color = color;
        this.colorLegacy = me.totalfreedom.totalfreedommod.util.AdventureUtil.namedTextColorToChatColor(color);

        // Build colored tag as Component
        this.coloredTag = Component.text("[")
                .color(NamedTextColor.DARK_GRAY)
                .append(Component.text(tag).color(color))
                .append(Component.text("]").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("").color(color));
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

    // Manual getters - Lombok @Getter not processing on enum fields
    public String getName()
    {
        return name;
    }

    public String getTag()
    {
        return tag;
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

    @Override
    @Deprecated
    public ChatColor getColorLegacy()
    {
        return colorLegacy;
    }

}
