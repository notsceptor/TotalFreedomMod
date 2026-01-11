package me.totalfreedom.totalfreedommod.command;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import me.totalfreedom.totalfreedommod.rank.Rank;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandPermissions
{
    /**
     * The minimum legacy rank required to use this command.
     * This is used as a fallback if no TFM permission is specified,
     * or for backward compatibility with existing commands.
     */
    Rank level();

    /**
     * Where this command can be executed from.
     */
    SourceType source();

    /**
     * Whether to block execution from the host console.
     */
    boolean blockHostConsole() default false;
    
    /**
     * TFM internal permission string required to use this command.
     * This is checked via RankManager.hasPermission() which uses the
     * custom rank permission system (NOT Bukkit permission nodes).
     * 
     * If specified, this takes priority over the legacy 'level' check.
     * Leave empty ("") to use only the legacy rank level check.
     * 
     * Examples:
     *   "tfm.manage.ranks" - Only ranks with this permission can use the command
     *   "tfm.admin.ban" - Requires ban permission
     *   "tfm.fun.smite" - Requires smite permission
     */
    String permission() default "";
}
