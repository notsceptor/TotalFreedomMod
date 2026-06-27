package me.totalfreedom.totalfreedommod.command;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandDispatchTarget
{
    DispatchTargetPriority priority() default DispatchTargetPriority.MEDIUM;
    String pattern() default "";
    String switches() default "";
}
