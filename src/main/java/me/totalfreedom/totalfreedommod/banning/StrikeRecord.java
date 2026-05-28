package me.totalfreedom.totalfreedommod.banning;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;

public class StrikeRecord
{

    @Getter
    private final String ip;
    @Getter
    @Setter
    private int count;
    @Getter
    @Setter
    private long lastStrikeUnix;
    @Getter
    @Setter
    private String lastUsername;

    public StrikeRecord(String ip)
    {
        this.ip = ip;
    }

    public StrikeRecord(String ip, int count, long lastStrikeUnix, String lastUsername)
    {
        this.ip = ip;
        this.count = count;
        this.lastStrikeUnix = lastStrikeUnix;
        this.lastUsername = lastUsername;
    }

    public int effectiveCount(int decayHours)
    {
        if (decayHours <= 0 || lastStrikeUnix <= 0L)
        {
            return count;
        }
        final long ageSeconds = (System.currentTimeMillis() / 1000L) - lastStrikeUnix;
        if (ageSeconds > (long) decayHours * 3600L)
        {
            return 0;
        }
        return count;
    }

    public void loadFrom(ConfigurationSection cs)
    {
        this.count = cs.getInt("count", 0);
        this.lastStrikeUnix = cs.getLong("last_strike_unix", 0L);
        this.lastUsername = cs.getString("last_username", null);
    }

    public void saveTo(ConfigurationSection cs)
    {
        cs.set("count", count);
        cs.set("last_strike_unix", lastStrikeUnix);
        cs.set("last_username", lastUsername);
    }
}
