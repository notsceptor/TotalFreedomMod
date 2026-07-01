package me.totalfreedom.totalfreedommod.command;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandDispatchTarget
{
    String pattern() default "";
    String switches() default "";
}
