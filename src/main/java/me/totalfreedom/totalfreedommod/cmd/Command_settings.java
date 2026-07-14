package me.totalfreedom.totalfreedommod.cmd;

import java.util.Optional;
import java.util.function.Consumer;

import org.bukkit.GameRules;
import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import static me.totalfreedom.totalfreedommod.config.ConfigEntry.*;

@Command(name = "settings", description = "Modify TotalFreedom Server Settings / Flags", usage = "/settings [toggle | set] <setting> [<radius> | <range> <count>]", aliases = {"toggle", "set", "tfset"}) 
@Permission(permission = "tfm.server.settings", level = Rank.SUPER_ADMIN)
public class Command_settings extends FCommand
{
    private final double defaultExplosionRadius = 4.0;
    private final double defaultMonitorRange = 10.0;
    private final int defaultBreakCount = 100;

    @Callback
    public void queryConfigOption(final CommandSender sender, final Setting setting)
    {   
        switch (setting) 
        {
            case Setting.EXPLOSIVES -> 
            {
                msg(
                    sender, 
                    "<displayable> (<flag>) is currently <status:enabled:disabled> with a radius of <radius>", 
                    Placeholder.unparsed("displayable", setting.getDisplayable()),
                    Placeholder.unparsed("flag", setting.getFlag()),
                    Formatter.number("radius", EXPLOSIVE_RADIUS.getDouble()),
                    Formatter.booleanChoice("status", setting.getValue().getBoolean())
                );
            }
            case Setting.NO_NUKE -> 
            {
                msg(
                    sender, 
                    "<displayable> (<flag>) is currently <status:enabled:disabled> with a range of <range> and a break count of <count>", 
                    Placeholder.unparsed("displayable", setting.getDisplayable()),
                    Placeholder.unparsed("flag", setting.getFlag()),
                    Formatter.number("range", NUKE_MONITOR_RANGE.getDouble()),
                    Formatter.number("count", NUKE_MONITOR_COUNT_BREAK.getDouble()),
                    Formatter.booleanChoice("status", setting.getValue().getBoolean())
                );
            }
            default ->  
            {
                msg(
                    sender, 
                    "<displayable> (<flag>) is currently <status:enabled:disabled>", 
                    Placeholder.unparsed("displayable", setting.getDisplayable()),
                    Placeholder.unparsed("flag", setting.getFlag()),
                    Formatter.booleanChoice("status", setting.getValue().getBoolean())
                );
            }
        }
    }

    @Callback
    @Subcommand("toggle")
    public void toggleConfigOption(final CommandSender sender, final Setting setting)
    {
        if (setting.equals(Setting.EXPLOSIVES))
        {
            setExplosiveRadius(sender, defaultExplosionRadius);
            return;
        }

        if (setting.equals(Setting.NO_NUKE))
        {
            setMonitorAndCount(sender, defaultMonitorRange, defaultBreakCount);
            return;
        }

        setting.consume();

        msg(
            sender, 
            "<displayable> (<flag>) changed to <status:enabled:disabled>", 
            Placeholder.unparsed("displayable", setting.getDisplayable()),
            Placeholder.unparsed("flag", setting.getFlag()),
            Formatter.booleanChoice("status", setting.getValue().setBoolean(!setting.getValue().getBoolean()))
        );
    }


    @Callback
    @Subcommand("set")
    public void setExplicit(final CommandSender sender, final Setting setting, final boolean value)
    {
        if (setting.equals(Setting.EXPLOSIVES))
        {
            setExplosiveRadius(sender, defaultExplosionRadius);
            return;
        }

        if (setting.equals(Setting.NO_NUKE))
        {
            setMonitorAndCount(sender, defaultMonitorRange, defaultBreakCount);
            return;
        }

        setting.consume();

        msg(
            sender, 
            "<displayable> (<flag>) changed to <status:enabled:disabled>", 
            Placeholder.unparsed("displayable", setting.getDisplayable()),
            Placeholder.unparsed("flag", setting.getFlag()),
            Formatter.booleanChoice("status", setting.getValue().setBoolean(value))
        );
    }

    @Callback
    @Subcommand("set explosives")
    public void setExplosiveRadius(final CommandSender sender, final double radius)
    {
        final Setting setting = Setting.EXPLOSIVES;

        if (radius != 0)
        {
            setting.getValue().setBoolean(true);
            EXPLOSIVE_RADIUS.setDouble(radius);
        }
        else
        {
            setting.getValue().setBoolean(false);
        }

        msg(
            sender, 
            "<displayable> (<flag>) changed to <status:enabled:disabled> with a radius of <radius>", 
            Placeholder.unparsed("displayable", setting.getDisplayable()),
            Placeholder.unparsed("flag", setting.getFlag()),
            Formatter.number("radius", radius),
            Formatter.booleanChoice("status", setting.getValue().getBoolean())
        );
    }

    @Callback
    @Subcommand("set nonuke")
    public void setWithMonitorRange(final CommandSender sender, final double range)
    {
        if (range != 0)
        {
            setMonitorAndCount(sender, range, defaultBreakCount);
        }
    }

    @Callback
    @Subcommand("set nonuke")
    public void setMonitorAndCount(final CommandSender sender, final double range, final int count)
    {
        Setting setting = Setting.NO_NUKE;

        if (range >= 1 || count >= 1)
        {
            setting.getValue().setBoolean(true);
            NUKE_MONITOR_RANGE.setDouble(range);
            NUKE_MONITOR_COUNT_BREAK.setInteger(count);
        }
        else
        {
            setting.getValue().setBoolean(false);
        }

        msg(
            sender, 
            "<displayable> (<flag>) changed to enabled with range <range> and break count <count>", 
            Placeholder.unparsed("displayable", setting.getDisplayable()),
            Placeholder.unparsed("flag", setting.getFlag()),
            Formatter.booleanChoice("status", setting.getValue().getBoolean())
        );
    }

    // We don't need an entire Argument Resolver for this. We can just plug in as an Enum and EnumArgumentResolver will do the rest.
    private enum Setting
    {
        FIRE_PLACE("fireplace", "Fire Placement", ALLOW_FIRE_PLACE),
        LAVA_PLACE("lavaplace", "Lava Placement", ALLOW_LAVA_PLACE),
        SIGN_PLACE("signplace", "Sign Placement", ALLOW_SIGN_PLACE),
        WATER_PLACE("waterplace", "Water Placement", ALLOW_WATER_PLACE),        
        FIRE_SPREAD("firespread", "Fire Spreading", ALLOW_FIRE_SPREAD, c -> TotalFreedomMod.plugin().gr.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, c.getBoolean() ? 128 : 0)),
        FLUID_SPREAD("fluidspread", "Fluid Spreading", ALLOW_FLUID_SPREAD),

        LAVA_DAMAGE("lavadmg", "Lava Damage", ALLOW_LAVA_DAMAGE),

        FALLING_BLOCKS("fallingblocks", "Falling Blocks", ALLOW_FALLING_BLOCKS),
        FALLING_SIGNS("fallingsigns", "Falling Signs", ALLOW_FALLING_SIGNS),

        ADMIN_MODE("adminmode", "Admin-Only Mode", ADMIN_ONLY_MODE),
        LOCKDOWN("lockdown", "Lockdown", LOCKDOWN_MODE),
        ENTITY_WIPE("entitywipe", "Automatic Entity Wiping", AUTO_ENTITY_WIPE),

        EXPLOSIVES("explosives", "Allow Explosions", ALLOW_EXPLOSIONS),
        NO_NUKE("nonuke", "Nuker Detection", NUKE_MONITOR_ENABLED),
        PET_PROTECT("petprotect", "Tamed Pet Protection", ENABLE_PET_PROTECT),

        AUTOCLEAR("autoclear", "Clearing Inventories on Join", AUTO_CLEAR),
        AUTOTP("autotp", "Teleportation on Join", AUTO_TP);




        final String flag, displayable;
        final ConfigEntry value;
        final Consumer<ConfigEntry> consumer;

        Setting(final String flag, final String displayable, final ConfigEntry value, final Consumer<ConfigEntry> consumer)
        {
            this.flag = flag;
            this.displayable = displayable;
            this.value = value;
            this.consumer = consumer;
        }

        Setting(final String flag, final String displayable, final ConfigEntry value)
        {
            this.flag = flag;
            this.displayable = displayable;
            this.value = value;
            this.consumer = null;
        }

        void consume()
        {
            Optional.ofNullable(consumer)
                    .ifPresent(c -> c.accept(value));
        }

        String getFlag()
        {
            return flag;
        }

        String getDisplayable()
        {
            return displayable;
        }

        ConfigEntry getValue()
        {
            return value;
        }
    }
}
