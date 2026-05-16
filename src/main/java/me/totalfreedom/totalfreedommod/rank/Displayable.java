package me.totalfreedom.totalfreedommod.rank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;

@SuppressWarnings("deprecation")
public interface Displayable
{

    public String getName();

    public String getTag();

    public NamedTextColor getColor();

    @Deprecated
    public ChatColor getColorLegacy();

    public Component getColoredName();

    public Component getColoredTag();

    public Component getColoredLoginMessage();

    @Deprecated
    public default String getColoredNameLegacy()
    {
        return me.totalfreedom.totalfreedommod.util.AdventureUtil.componentToLegacy(getColoredName());
    }

    @Deprecated
    public default String getColoredTagLegacy()
    {
        return me.totalfreedom.totalfreedommod.util.AdventureUtil.componentToLegacy(getColoredTag());
    }

    @Deprecated
    public default String getColoredLoginMessageLegacy()
    {
        return me.totalfreedom.totalfreedommod.util.AdventureUtil.componentToLegacy(getColoredLoginMessage());
    }

}
