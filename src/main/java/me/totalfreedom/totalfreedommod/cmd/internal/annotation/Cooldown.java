package me.totalfreedom.totalfreedommod.cmd.internal.annotation;

import me.totalfreedom.totalfreedommod.cmd.internal.CooldownUnit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies a per-player cooldown to the annotated subcommand handler.
 * <p>
 * Use {@code {remaining}} in {@link #message()} as a placeholder; it is replaced with
 * the number of whole seconds left on the cooldown at dispatch time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Cooldown
{
    long value();
    CooldownUnit unit() default CooldownUnit.SECONDS;
    String message() default "You must wait {remaining}s before using this command again.";
}

