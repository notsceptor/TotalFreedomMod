package me.totalfreedom.totalfreedommod.cmd.internal.annotation;

import java.lang.annotation.*;

/**
 * Overrides tab-completion for a specific argument position on a subcommand.
 * <p>
 * {@link #value()} must match the {@link me.totalfreedom.totalfreedommod.cmd.internal.annotation.Subcommand#value() Subcommand#value()} path of the handler this
 * completer applies to (a Completer is always a separate method from the handler, so it needs an explicit link). 
 * {@link #position()} is zero-based and counts only the handler's positional (Brigadier argument)
 * parameters: the sender and any {@link Switch}-annotated parameters are excluded, since switches
 * become literal branches rather than argument nodes.
 * <p>
 * The annotated method must return {@code List<String>} and accept exactly two parameters:
 * the same sender type as the handler, followed by the partially-typed input ({@code String}).
 * The returned list is used as-is. Consider {@link me.totalfreedom.totalfreedommod.cmd.internal.FuzzyMatch#filter FuzzyMatch#filter(List<String>, String)} 
 * for subsequence fuzzy matching against a candidate list, which also happens to be automatically applied to enum-typed arguments with no {@code @Completer}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Completer
{
    String value();
    int position();
}
