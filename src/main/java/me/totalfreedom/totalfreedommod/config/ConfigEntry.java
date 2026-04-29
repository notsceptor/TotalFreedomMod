package me.totalfreedom.totalfreedommod.config;

import java.util.List;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;

public enum ConfigEntry
{

    FORCE_IP_ENABLED(Boolean.class, "forceip.enabled"),
    FORCE_IP_PORT(Integer.class, "forceip.port"),
    FORCE_IP_KICKMSG(String.class, "forceip.kickmsg"),
    //
    ALLOW_EXPLOSIONS(Boolean.class, "allow.explosions"),
    ALLOW_FIRE_PLACE(Boolean.class, "allow.fire_place"),
    ALLOW_FIRE_SPREAD(Boolean.class, "allow.fire_spread"),
    ALLOW_FLUID_SPREAD(Boolean.class, "allow.fluid_spread"),
    ALLOW_LAVA_DAMAGE(Boolean.class, "allow.lava_damage"),
    ALLOW_LAVA_PLACE(Boolean.class, "allow.lava_place"),
    ALLOW_TNT_MINECARTS(Boolean.class, "allow.tnt_minecarts"),
    ALLOW_WATER_PLACE(Boolean.class, "allow.water_place"),
    //
    MOB_LIMITER_ENABLED(Boolean.class, "moblimiter.enabled"),
    MOB_LIMITER_MAX(Integer.class, "moblimiter.max"),
    MOB_LIMITER_DISABLE_DRAGON(Boolean.class, "moblimiter.disable.dragon"),
    MOB_LIMITER_DISABLE_GHAST(Boolean.class, "moblimiter.disable.ghast"),
    MOB_LIMITER_DISABLE_GIANT(Boolean.class, "moblimiter.disable.giant"),
    MOB_LIMITER_DISABLE_SLIME(Boolean.class, "moblimiter.disable.slime"),
    //
    HTTPD_ENABLED(Boolean.class, "httpd.enabled"),
    HTTPD_PORT(Integer.class, "httpd.port"),
    HTTPD_PUBLIC_FOLDER(String.class, "httpd.public_folder"),
    //
    SERVER_COLORFUL_MOTD(Boolean.class, "server.colorful_motd"),
    SERVER_NAME(String.class, "server.name"),
    SERVER_ADDRESS(String.class, "server.address"),
    SERVER_MOTD(String.class, "server.motd"),
    SERVER_OWNERS(List.class, "server.owners"),
    SERVER_BAN_URL(String.class, "server.ban_url"),
    SERVER_PERMBAN_URL(String.class, "server.permban_url"),
    //
    ADMINLIST_CLEAN_THESHOLD_HOURS(Integer.class, "adminlist.clean_threshold_hours"),
    ADMINLIST_CONSOLE_IS_SENIOR(Boolean.class, "adminlist.console_is_senior"),
    ADMINLIST_MOJANG_UUID_LOOKUP(Boolean.class, "adminlist.mojang_uuid_lookup"),
    //
    DISABLE_NIGHT(Boolean.class, "disable.night"),
    DISABLE_WEATHER(Boolean.class, "disable.weather"),
    //
    ENABLE_PREPROCESS_LOG(Boolean.class, "preprocess_log"),
    ENABLE_PET_PROTECT(Boolean.class, "petprotect.enabled"),
    //
    LANDMINES_ENABLED(Boolean.class, "landmines_enabled"),
    TOSSMOB_ENABLED(Boolean.class, "tossmob_enabled"),
    AUTOKICK_ENABLED(Boolean.class, "autokick.enabled"),
    MP44_ENABLED(Boolean.class, "mp44_enabled"),
    //
    PROTECTAREA_ENABLED(Boolean.class, "protectarea.enabled"),
    PROTECTAREA_SPAWNPOINTS(Boolean.class, "protectarea.auto_protect_spawnpoints"),
    PROTECTAREA_RADIUS(Double.class, "protectarea.auto_protect_radius"),
    //
    WORLDEDIT_LIMIT_MAX(Integer.class, "worldedit.limit_max"),
    WORLDEDIT_DEOP_ON_LIMIT_ABUSE(Boolean.class, "worldedit.deop_on_limit_abuse"),
    //
    NUKE_MONITOR_ENABLED(Boolean.class, "nukemonitor.enabled"),
    NUKE_MONITOR_COUNT_BREAK(Integer.class, "nukemonitor.count_break"),
    NUKE_MONITOR_COUNT_PLACE(Integer.class, "nukemonitor.count_place"),
    NUKE_MONITOR_RANGE(Double.class, "nukemonitor.range"),
    //
    AUTOKICK_THRESHOLD(Double.class, "autokick.threshold"),
    AUTOKICK_TIME(Integer.class, "autokick.time"),
    //
    AUTO_OP_ENABLED(Boolean.class, "auto_op.enabled"),
    AUTO_OP_PERSISTENT_MONITOR(Boolean.class, "auto_op.persistent_monitor"),
    AUTO_OP_MONITOR_INTERVAL(Integer.class, "auto_op.monitor_interval"),
    //
    LOGS_SECRET(String.class, "logs.secret"),
    LOGS_URL(String.class, "logs.url"),
    //
    FLATLANDS_GENERATE(Boolean.class, "flatlands.generate"),
    FLATLANDS_GENERATE_PARAMS(String.class, "flatlands.generate_params"),
    //
    ANNOUNCER_ENABLED(Boolean.class, "announcer.enabled"),
    ANNOUNCER_INTERVAL(Integer.class, "announcer.interval"),
    ANNOUNCER_PREFIX(String.class, "announcer.prefix"),
    ANNOUNCER_ANNOUNCEMENTS(List.class, "announcer.announcements"),
    //
    EXPLOSIVE_RADIUS(Double.class, "explosive_radius"),
    FREECAM_TRIGGER_COUNT(Integer.class, "freecam_trigger_count"),
    SERVICE_CHECKER_URL(String.class, "service_checker_url"),
    BLOCKED_COMMANDS(List.class, "blocked_commands"),
    BLOCK_SERVER_COMMANDS_ENABLED(Boolean.class, "server_command_blocker.enabled"),
    BLOCK_SERVER_COMMANDS_BLOCK_AT_NAMED_SENDERS(Boolean.class, "server_command_blocker.block_at_named_senders"),
    BLOCK_SERVER_COMMANDS_BLOCK_COMMAND_BLOCK_HOLDERS(Boolean.class, "server_command_blocker.block_command_block_holders"),
    BLOCK_SERVER_COMMANDS_BLOCKED_SUBSTRINGS(List.class, "server_command_blocker.blocked_substrings"),
    BLOCK_SERVER_COMMANDS_LOG_THROTTLED_WARNINGS(Boolean.class, "server_command_blocker.log_throttled_warnings"),
    BLOCK_SERVER_COMMANDS_LOG_INTERVAL_TICKS(Integer.class, "server_command_blocker.log_interval_ticks"),
    HOST_SENDER_NAMES(List.class, "host_sender_names"),
    FAMOUS_PLAYERS(List.class, "famous_players"),
    OVERLORD_IPS(List.class, "overlord_ips"),
    NOADMIN_IPS(List.class, "noadmin_ips"),
    ADMIN_ONLY_MODE(Boolean.class, "admin_only_mode"),
    AUTO_ENTITY_WIPE(Boolean.class, "auto_wipe"),
    DISGUISES_FORBIDDEN_TYPES(List.class, "disguises.forbidden_types"),
    //
    VAULT_CHAT_PROVIDER_ENABLED(Boolean.class, "chat.provider.enabled"),
    VAULT_CHAT_PROVIDER_OVERRIDE_EXISTING(Boolean.class, "chat.provider.override_existing"),
    VAULT_CHAT_FORMAT(String.class, "chat.format.message"),
    VAULT_CHAT_TAG(String.class, "chat.format.tag"),
    VAULT_CHAT_ENFORCE_PREFIX(Boolean.class, "chat.behavior.enforce_prefix"),
    VAULT_CHAT_NO_CAPS(Boolean.class, "chat.behavior.no_caps.enabled"),
    VAULT_CHAT_CAPS_RATIO(Integer.class, "chat.behavior.no_caps.caps_ratio"),
    VAULT_CHAT_PREVENT_SPAM(Boolean.class, "chat.behavior.prevent_spam"),
    VAULT_CHAT_ALLOW_COLOR_FORMATTING(Boolean.class, "chat.behavior.allow_color_formatting"),
    VAULT_CHAT_ALLOW_SPECIAL_FORMATTING(Boolean.class, "chat.behavior.allow_special_formatting"),
    VAULT_CHAT_MAX_MESSAGE_LENGTH(Integer.class, "chat.behavior.max_message_length"),
    VAULT_CHAT_NOTIFY_TRUNCATED(Boolean.class, "chat.behavior.notify_truncated"),
    VAULT_PREFIX_IMPOSTOR(String.class, "chat.prefix.impostor"),
    VAULT_PREFIX_NON_OP(String.class, "chat.prefix.non_op"),
    VAULT_PREFIX_OP(String.class, "chat.prefix.op"),
    VAULT_PREFIX_SUPER_ADMIN(String.class, "chat.prefix.super_admin"),
    VAULT_PREFIX_TELNET_ADMIN(String.class, "chat.prefix.telnet_admin"),
    VAULT_PREFIX_SENIOR_ADMIN(String.class, "chat.prefix.senior_admin"),
    VAULT_PREFIX_TELNET_CONSOLE(String.class, "chat.prefix.telnet_console"),
    VAULT_PREFIX_SENIOR_CONSOLE(String.class, "chat.prefix.senior_console"),
    VAULT_PREFIX_DEVELOPER(String.class, "chat.prefix.developer"),
    VAULT_PREFIX_OWNER(String.class, "chat.prefix.owner"),
    //
    TABLIST_ENABLED(Boolean.class, "tablist.enabled"),
    TABLIST_HEADER(String.class, "tablist.header"),
    TABLIST_FOOTER(String.class, "tablist.footer"),
    TABLIST_TYPE(String.class, "tablist.type"),
    TABLIST_SIZE(Integer.class, "tablist.size"),
    TABLIST_COLUMNS(Integer.class, "tablist.columns"),
    TABLIST_PLAYER_COMPONENT(String.class, "tablist.player_component"),
    TABLIST_AFK_TAG(String.class, "tablist.afk_tag"),
    TABLIST_DISPLAY_NICKNAME_PREFIX(String.class, "tablist.display_nickname_prefix"),
    TABLIST_UPDATE_INTERVAL(Integer.class, "tablist.update_interval");

    //
    private final Class<?> type;
    private final String configName;

    private ConfigEntry(Class<?> type, String configName)
    {
        this.type = type;
        this.configName = configName;
    }

    public Class<?> getType()
    {
        return type;
    }

    public String getConfigName()
    {
        return configName;
    }

    public String getString()
    {
        return getConfig().getString(this);
    }

    public String setString(String value)
    {
        getConfig().setString(this, value);
        return value;
    }

    public Double getDouble()
    {
        return getConfig().getDouble(this);
    }

    public Double setDouble(Double value)
    {
        getConfig().setDouble(this, value);
        return value;
    }

    public Boolean getBoolean()
    {
        return getConfig().getBoolean(this);
    }

    public Boolean setBoolean(Boolean value)
    {
        getConfig().setBoolean(this, value);
        return value;
    }

    public Integer getInteger()
    {
        return getConfig().getInteger(this);
    }

    public Integer setInteger(Integer value)
    {
        getConfig().setInteger(this, value);
        return value;
    }

    public List<?> getList()
    {
        return getConfig().getList(this);
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList()
    {
        return (List<String>) getList();
    }

    private MainConfig getConfig()
    {
        return TotalFreedomMod.plugin().config;
    }

    public static ConfigEntry findConfigEntry(String name)
    {
        name = name.toLowerCase().replace("_", "");
        for (ConfigEntry entry : values())
        {
            if (entry.toString().toLowerCase().replace("_", "").equals(name))
            {
                return entry;
            }
        }
        return null;
    }
}
