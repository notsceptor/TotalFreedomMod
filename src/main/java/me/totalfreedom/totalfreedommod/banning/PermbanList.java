package me.totalfreedom.totalfreedommod.banning;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.sql.adapter.PermbanRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import java.io.File;
import java.io.IOException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class PermbanList extends FreedomService
{

    public static final String CONFIG_FILENAME = "permbans.yml";

    private final Set<String> permbannedNames = Sets.newHashSet();
    private final Set<String> permbannedIps = Sets.newHashSet();
    
    // Store full PermBan objects for SQL operations
    private final Map<String, PermBan> permbansByName = Maps.newHashMap();
    private final Object lock = new Object();
    private final Object persistenceLock = new Object();

    // Flag to track if SQL is available
    private boolean usingSql = false;

    public PermbanList(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        // Try to load from SQL database first
        if (plugin.dm != null && plugin.dm.isInitialized())
        {
            loadFromSql();
        }
        else
        {
            loadFromYaml();
        }
    }
    
    /**
     * Load permbans from SQL database.
     */
    private void loadFromSql()
    {
        try
        {
            PermbanRepository repo = plugin.dm.getPermbanRepository();
            List<PermBan> loadedPermbans = repo.findAll().block();

            synchronized (lock)
            {
                permbannedNames.clear();
                permbannedIps.clear();
                permbansByName.clear();

                for (PermBan permban : loadedPermbans)
                {
                    String name = permban.getUsername().toLowerCase().trim();
                    permbannedNames.add(name);
                    permbannedIps.addAll(permban.getIps());
                    permbansByName.put(name, permban);
                }

                usingSql = true;
                FLog.info("Loaded " + permbannedIps.size() + " perm IP bans and " + permbannedNames.size() + " perm username bans from SQL database.");
            }
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to load permbans from SQL, falling back to YAML: " + ex.getMessage());
            loadFromYaml();
        }
    }
    
    /**
     * Load permbans from YAML file (fallback).
     */
    private void loadFromYaml()
    {
        final File configFile = new File(plugin.getDataFolder(), CONFIG_FILENAME);
        if (!configFile.exists())
        {
            try
            {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }
            catch (IOException ex)
            {
                FLog.severe("Could not create " + CONFIG_FILENAME);
            }
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        synchronized (lock)
        {
            permbannedNames.clear();
            permbannedIps.clear();
            permbansByName.clear();

            for (String name : config.getKeys(false))
            {
                String lowerName = name.toLowerCase().trim();
                permbannedNames.add(lowerName);
                List<String> ips = config.getStringList(name);
                permbannedIps.addAll(ips);

                // Create PermBan object
                PermBan permban = new PermBan();
                permban.setUsername(lowerName);
                permban.setIps(ips);
                permbansByName.put(lowerName, permban);
            }

            usingSql = false;
            FLog.info("Loaded " + permbannedIps.size() + " perm IP bans and " + permbannedNames.size() + " perm username bans from YAML.");
        }
    }

    @Override
    protected void onStop()
    {
        // Save if using SQL
        if (usingSql)
        {
            saveAllToSql();
        }
    }
    
    /**
     * Save all permbans to SQL database.
     */
    private void saveAllToSql()
    {
        if (plugin.dm == null || !plugin.dm.isInitialized())
        {
            return;
        }

        final List<PermBan> snapshot;
        synchronized (lock)
        {
            snapshot = new ArrayList<>(permbansByName.values());
        }

        synchronized (persistenceLock)
        {
            try
            {
                PermbanRepository repo = plugin.dm.getPermbanRepository();
                for (PermBan permban : snapshot)
                {
                    repo.save(permban).block();
                }
                FLog.debug("Saved " + snapshot.size() + " permbans to SQL database");
            }
            catch (Exception ex)
            {
                FLog.warning("Failed to save permbans to SQL: " + ex.getMessage());
            }
        }
    }

    public void reload()
    {
        onStop();
        onStart();
    }
    
    /**
     * Add a permban.
     */
    public void addPermban(PermBan permban)
    {
        final boolean sql;
        synchronized (lock)
        {
            String name = permban.getUsername().toLowerCase().trim();
            permbannedNames.add(name);
            permbannedIps.addAll(permban.getIps());
            permbansByName.put(name, permban);
            sql = usingSql;
        }

        if (sql)
        {
            savePermbanToSqlAsync(permban);
        }
    }

    /**
     * Remove a permban by username.
     */
    public boolean removePermban(String username)
    {
        final String name = username.toLowerCase().trim();
        final PermBan permban;
        final boolean sql;
        synchronized (lock)
        {
            permban = permbansByName.remove(name);
            if (permban == null)
            {
                return false;
            }

            permbannedNames.remove(name);
            // Remove IPs associated with this permban
            permbannedIps.removeAll(permban.getIps());
            sql = usingSql;
        }

        if (sql)
        {
            removePermbanFromSqlAsync(name);
        }

        return true;
    }

    /**
     * Remove every permban that has an IP matching the given address or range.
     */
    public List<String> removePermbansByIp(String ip)
    {
        final List<String> removedNames = new ArrayList<>();
        final boolean sql;
        synchronized (lock)
        {
            for (PermBan permban : new ArrayList<>(permbansByName.values()))
            {
                boolean matches = false;
                for (String banIp : permban.getIps())
                {
                    if (FUtil.fuzzyIpMatch(ip, banIp, 4))
                    {
                        matches = true;
                        break;
                    }
                }

                if (!matches)
                {
                    continue;
                }

                final String name = permban.getUsername().toLowerCase().trim();
                permbansByName.remove(name);
                permbannedNames.remove(name);
                removedNames.add(permban.getUsername());
            }

            // Rebuild the IP view so IPs shared with surviving permbans are retained.
            permbannedIps.clear();
            for (PermBan permban : permbansByName.values())
            {
                permbannedIps.addAll(permban.getIps());
            }

            sql = usingSql;
        }

        if (sql)
        {
            for (String name : removedNames)
            {
                removePermbanFromSqlAsync(name.toLowerCase().trim());
            }
        }

        return removedNames;
    }

    /**
     * Get a permban by username.
     */
    public PermBan getPermban(String username)
    {
        synchronized (lock)
        {
            return permbansByName.get(username.toLowerCase().trim());
        }
    }

    /**
     * Returns a snapshot of the permbanned usernames.
     */
    public Set<String> getPermbannedNames()
    {
        synchronized (lock)
        {
            return new HashSet<>(permbannedNames);
        }
    }

    /**
     * Returns a snapshot of the permbanned IPs.
     */
    public Set<String> getPermbannedIps()
    {
        synchronized (lock)
        {
            return new HashSet<>(permbannedIps);
        }
    }

    /**
     * Save a single permban to SQL.
     */
    private void savePermbanToSqlAsync(PermBan permban)
    {
        if (!plugin.isEnabled())
        {
            savePermbanToSql(permban);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> savePermbanToSql(permban));
    }

    private void savePermbanToSql(PermBan permban)
    {
        if (plugin.dm == null || !plugin.dm.isInitialized())
        {
            return;
        }

        synchronized (persistenceLock)
        {
            try
            {
                plugin.dm.getPermbanRepository().save(permban).block();
            }
            catch (Exception ex)
            {
                FLog.warning("Failed to save permban to SQL: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Remove a single permban from SQL.
     */
    private void removePermbanFromSqlAsync(String name)
    {
        if (!plugin.isEnabled())
        {
            removePermbanFromSql(name);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> removePermbanFromSql(name));
    }

    private void removePermbanFromSql(String name)
    {
        if (plugin.dm == null || !plugin.dm.isInitialized())
        {
            return;
        }

        synchronized (persistenceLock)
        {
            try
            {
                plugin.dm.getPermbanRepository().deleteByUsername(name);
            }
            catch (Exception ex)
            {
                FLog.warning("Failed to remove permban from SQL: " + ex.getMessage());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event)
    {
        final String username = event.getName();
        final String ip = event.getAddress().getHostAddress().trim();

        // Permbanned IPs
        for (String testIp : getPermbannedIps())
        {
            if (FUtil.fuzzyIpMatch(testIp, ip, 4))
            {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        Component.text("Your IP address is permanently banned from this server.\n"
                                + "Release procedures are available at\n", NamedTextColor.RED)
                                .append(Component.text(ConfigEntry.SERVER_PERMBAN_URL.getString(), NamedTextColor.GOLD)));
                return;
            }
        }

        // Permbanned usernames
        for (String testPlayer : getPermbannedNames())
        {
            if (testPlayer.equalsIgnoreCase(username))
            {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        Component.text("Your username is permanently banned from this server.\n"
                                + "Release procedures are available at\n", NamedTextColor.RED)
                                .append(Component.text(ConfigEntry.SERVER_PERMBAN_URL.getString(), NamedTextColor.GOLD)));
                return;
            }
        }

    }

}
