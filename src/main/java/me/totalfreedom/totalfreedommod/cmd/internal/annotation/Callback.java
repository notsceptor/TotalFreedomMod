package me.totalfreedom.totalfreedommod.cmd.internal.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a command handler.
 * <p>
 * The method's first parameter must be {@link org.bukkit.command.CommandSender} or
 * {@link org.bukkit.entity.Player}. Remaining parameters map positionally to Brigadier
 * argument nodes (see {@code ArgumentResolver} for supported types).
 * <p>
 * A {@code void} return type is treated as always-success. You can still use a {@code boolean} to simulate the Bukkit command return system.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Callback
{
}
