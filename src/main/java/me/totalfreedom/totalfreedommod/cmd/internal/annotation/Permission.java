package me.totalfreedom.totalfreedommod.cmd.internal.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import me.totalfreedom.totalfreedommod.cmd.SourceType;
import me.totalfreedom.totalfreedommod.rank.Rank;

/**
 * Declares who may run a command (or a single subcommand handler) and from where.
 * <p>
 * Place on an {@code FCommand} class. The check is also wired into the Brigadier root node's {@code requires()}, 
 * so unauthorized senders don't see the command in tab completion. 
 * Place on a handler method to indicate a permission for that specific subcommand (this overrides the parent on a permissible check).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface Permission
{
    Rank level() default Rank.OP;

    SourceType source() default SourceType.BOTH;

    String permission();

    String message() default "You do not have permission to use this command.";
}
