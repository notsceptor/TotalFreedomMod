package me.totalfreedom.totalfreedommod.player;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import me.totalfreedom.totalfreedommod.util.ConfigInterfaces.ConfigLoadable;
import me.totalfreedom.totalfreedommod.util.ConfigInterfaces.ConfigSavable;
import me.totalfreedom.totalfreedommod.util.ConfigInterfaces.Validatable;
import org.apache.commons.lang3.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class PlayerData implements ConfigLoadable, ConfigSavable, Validatable
{
    public static final int MAX_STRIKES = 3;

    @Getter
    @Setter
    private String username;
    @Getter
    @Setter
    private long firstJoinUnix;
    @Getter
    @Setter
    private long lastJoinUnix;
    @Getter
    @Setter
    private boolean potionSpy;
    @Getter
    @Setter
    private boolean commandSpy;
    @Getter
    @Setter
    private boolean muted;
    @Getter
    @Setter
    private boolean frozen;
    @Getter
    @Setter
    private boolean commandsBlocked;
    @Getter
    private int strikes;
    private final List<String> ips = Lists.newArrayList();

    public PlayerData(Player player)
    {
        this(player.getName());
    }

    public PlayerData(String username)
    {
        this.username = username;
    }

    @Override
    public void loadFrom(ConfigurationSection cs)
    {
        this.username = cs.getString("username", username);
        this.ips.clear();
        this.ips.addAll(cs.getStringList("ips"));
        this.firstJoinUnix = cs.getLong("first_join", 0);
        this.lastJoinUnix = cs.getLong("last_join", 0);
        this.potionSpy = cs.getBoolean("potion_spy", false);
        this.commandSpy = cs.getBoolean("command_spy", false);
        this.muted = cs.getBoolean("muted", false);
        this.frozen = cs.getBoolean("frozen", false);
        this.commandsBlocked = cs.getBoolean("commands_blocked", false);
        this.strikes = cs.getInt("strikes", 0);
    }

    @Override
    public void saveTo(ConfigurationSection cs)
    {
        Validate.isTrue(isValid(), "Could not save player entry: " + username + ". Entry not valid!");
        cs.set("username", username);
        cs.set("ips", ips);
        cs.set("first_join", firstJoinUnix);
        cs.set("last_join", lastJoinUnix);
        cs.set("potion_spy", potionSpy);
        cs.set("command_spy", commandSpy);
        cs.set("muted", muted);
        cs.set("frozen", frozen);
        cs.set("commands_blocked", commandsBlocked);
        cs.set("strikes", strikes);
    }

    public List<String> getIps()
    {
        return Collections.unmodifiableList(ips);
    }

    // IP utils
    public boolean addIp(String ip)
    {
        return ips.contains(ip) ? false : ips.add(ip);
    }

    public boolean removeIp(String ip)
    {
        return ips.remove(ip);
    }

    @Override
    public boolean isValid()
    {
        return username != null
                && firstJoinUnix != 0
                && lastJoinUnix != 0
                && !ips.isEmpty();
    }

    public void setStrikes(int strikes)
    {
        if (strikes < 0 || strikes > MAX_STRIKES)
            return;
        this.strikes = strikes;
    }
}
