package me.totalfreedom.totalfreedommod.cmd.internal;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Maps Java parameter types to Brigadier {@link ArgumentType} instances.
 *
 * <p>Supported mappings:
 * <ul>
 *   <li>{@code String} -> {@link StringArgumentType#string()}
 *   <li>{@code int/Integer} -> {@link IntegerArgumentType#integer()}
 *   <li>{@code long/Long} -> {@link LongArgumentType#longArg()}
 *   <li>{@code double/Double} -> {@link DoubleArgumentType#doubleArg()}
 *   <li>{@code float/Float} -> {@link FloatArgumentType#floatArg()}
 *   <li>{@code boolean} -> {@link BoolArgumentType#bool()}
 *   <li>{@code Enum} subtype -> {@link StringArgumentType#word()} (parsed to enum at dispatch)
 *   <li>{@code Player} (non-sender) -> handled via {@link io.papermc.paper.command.brigadier.argument.ArgumentTypes#player() ArgumentTypes#player()} in
 *       {@link CommandProcessor}. Call {@link #isPlayerArgType} to detect this case.
 * </ul>
 *
 * <p>Types not listed above are handled through the custom resolvers in
 * {@link ResolverRegistry} either automatically by parameter type or explicitly via
 * {@link me.totalfreedom.totalfreedommod.cmd.internal.annotation.Resolve @Resolve}.
 */
public final class ArgumentResolver
{
    private ArgumentResolver() {}

    public static boolean isSenderType(Class<?> type)
    {
        return CommandSender.class.isAssignableFrom(type);
    }

    public static boolean isPlayerArgType(Class<?> type)
    {
        return Player.class.isAssignableFrom(type);
    }

    public static ArgumentType<?> resolve(Class<?> type)
    {
        if (type == String.class)                       return StringArgumentType.string();
        if (type == int.class || type == Integer.class) return IntegerArgumentType.integer();
        if (type == long.class || type == Long.class)   return LongArgumentType.longArg();
        if (type == double.class || type == Double.class) return DoubleArgumentType.doubleArg();
        if (type == float.class || type == Float.class) return FloatArgumentType.floatArg();
        if (type == boolean.class || type == Boolean.class) return BoolArgumentType.bool();
        if (type.isEnum())                              return StringArgumentType.word();
        throw new IllegalArgumentException(
            "No built-in Brigadier ArgumentType mapping for: " + type.getName()
            + ". Register a custom resolver in ResolverRegistry or annotate the parameter with @Resolve.");
    }
}
