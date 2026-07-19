package me.totalfreedom.totalfreedommod.banning;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.sql.adapter.StrikeRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class StrikeList extends FreedomService
{

    private final Map<String, StrikeRecord> strikes = Maps.newHashMap();
    private final File configFile;
    private YamlConfiguration config;
    private boolean usingSql = false;
    private boolean persistEnabled = true;

    public StrikeList(TotalFreedomMod plugin)
    {
        super(plugin);
        this.configFile = new File(plugin.getDataFolder(), "strikes.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    protected void onStart()
    {
        strikes.clear();
        usingSql = false;

        final Boolean persist = ConfigEntry.AUTOEJECT_PERSIST.getBoolean();
        persistEnabled = persist == null || persist;

        if (!persistEnabled)
        {
            FLog.info("Strike persistence disabled; running in-memory.");
            return;
        }

        if (plugin.dm != null && plugin.dm.isInitialized())
        {
            loadFromSql();
        }
        else
        {
            loadFromYaml();
        }

        pruneDecayed();
        FLog.info("Loaded " + strikes.size() + " strike records.");
    }

    @Override
    protected void onStop()
    {
        if (!persistEnabled)
        {
            return;
        }
        if (!usingSql)
        {
            saveAllToYaml();
        }
    }

    private void loadFromSql()
    {
        try
        {
            StrikeRepository repo = plugin.dm.getStrikeRepository();
            Map<String, StrikeRecord> loaded = repo.loadAllAsync().block();
            strikes.putAll(loaded);
            usingSql = true;
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to load strikes from SQL, falling back to YAML: " + ex.getMessage());
            loadFromYaml();
        }
    }

    private void loadFromYaml()
    {
        if (!configFile.exists())
        {
            try
            {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }
            catch (IOException ex)
            {
                FLog.severe("Could not create strikes.yml");
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        for (String ip : config.getKeys(false))
        {
            if (!config.isConfigurationSection(ip))
            {
                continue;
            }
            ConfigurationSection cs = config.getConfigurationSection(ip);
            StrikeRecord r = new StrikeRecord(ip);
            r.loadFrom(cs);
            strikes.put(ip, r);
        }
        usingSql = false;
    }

    private void pruneDecayed()
    {
        final int decayHours = decayHours();
        if (decayHours <= 0)
        {
            return;
        }
        int removed = 0;
        for (Iterator<Map.Entry<String, StrikeRecord>> it = strikes.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<String, StrikeRecord> e = it.next();
            if (e.getValue().effectiveCount(decayHours) == 0)
            {
                it.remove();
                removed++;
                if (usingSql && plugin.dm != null && plugin.dm.isInitialized())
                {
                    plugin.dm.getStrikeRepository().deleteByIpAsync(e.getKey())
                            .subscribe(deleted -> {}, ex ->
                                    FLog.warning("Failed to prune decayed strike for " + e.getKey() + ": " + ex.getMessage()));
                }
            }
        }
        if (removed > 0 && !usingSql)
        {
            saveAllToYamlAsync();
        }
        if (removed > 0)
        {
            FLog.info("Pruned " + removed + " decayed strike record(s).");
        }
    }

    private int decayHours()
    {
        final Integer h = ConfigEntry.AUTOEJECT_TIME_WINDOW.getInteger();
        return h == null ? 0 : h;
    }

    public synchronized int recordStrikeAndGet(String ip, String username)
    {
        StrikeRecord r = strikes.get(ip);
        final int decay = decayHours();
        int base = 0;
        if (r != null)
        {
            base = r.effectiveCount(decay);
        }
        if (r == null)
        {
            r = new StrikeRecord(ip);
            strikes.put(ip, r);
        }
        r.setCount(base + 1);
        r.setLastStrikeUnix(System.currentTimeMillis() / 1000L);
        if (username != null)
        {
            r.setLastUsername(username);
        }

        if (persistEnabled)
        {
            persist(r);
        }
        return r.getCount();
    }

    public synchronized int peek(String ip)
    {
        StrikeRecord r = strikes.get(ip);
        if (r == null)
        {
            return 0;
        }
        return r.effectiveCount(decayHours());
    }

    public synchronized boolean clear(String ip)
    {
        StrikeRecord removed = strikes.remove(ip);
        if (removed == null)
        {
            return false;
        }
        if (!persistEnabled)
        {
            return true;
        }
        if (usingSql && plugin.dm != null && plugin.dm.isInitialized())
        {
            plugin.dm.getStrikeRepository().deleteByIpAsync(ip)
                    .subscribe(deleted -> {}, ex ->
                            FLog.warning("Failed to clear strike from SQL: " + ex.getMessage()));
        }
        else
        {
            saveAllToYamlAsync();
        }
        return true;
    }

    public synchronized Map<String, StrikeRecord> snapshot()
    {
        return Collections.unmodifiableMap(new HashMap<>(strikes));
    }

    private void persist(StrikeRecord r)
    {
        if (usingSql && plugin.dm != null && plugin.dm.isInitialized())
        {
            plugin.dm.getStrikeRepository().upsertAsync(r)
                    .subscribe(null, ex ->
                            FLog.warning("Failed to persist strike to SQL: " + ex.getMessage()));
        }
        else
        {
            saveAllToYamlAsync();
        }
    }

    private void saveAllToYamlAsync()
    {
        if (!plugin.isEnabled())
        {
            saveAllToYaml();
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveAllToYaml);
    }

    private synchronized void saveAllToYaml()
    {
        for (String key : config.getKeys(false))
        {
            config.set(key, null);
        }
        for (StrikeRecord r : strikes.values())
        {
            r.saveTo(config.createSection(r.getIp()));
        }
        try
        {
            config.save(configFile);
        }
        catch (IOException ex)
        {
            FLog.severe("Could not save strikes.yml");
        }
    }
}
