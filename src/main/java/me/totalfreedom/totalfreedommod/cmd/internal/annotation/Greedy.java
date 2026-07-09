package me.totalfreedom.totalfreedommod.cmd.internal.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the final handler parameter as variable-length.
 * <p>
 * Replaces the legacy {@code <arg..>} pattern syntax. Only valid on the last parameter of a handler. 
 * The parameter must be a {@code String} or be paired with {@link Resolve} for post-processing of the raw remainder.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
public @interface Greedy
{
}
