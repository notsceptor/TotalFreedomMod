package me.totalfreedom.totalfreedommod.banning;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.sql.adapter.PermbanRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class PermbanList extends FreedomService
{

    public static final String CONFIG_FILENAME = "permbans.yml";

    @Getter
    private final Set<String> permbannedNames = Sets.newHashSet();
    @Getter
    private final Set<String> permbannedIps = Sets.newHashSet();
    
    // Store full PermBan objects for SQL operations
    private final Map<String, PermBan> permbansByName = Maps.newHashMap();
    
    // Flag to track if SQL is available
    private boolean usingSql = false;
    
    // Manual getters - Lombok @Getter not processing reliably
    public Set<String> getPermbannedIps()
    {
        return permbannedIps;
    }
    
    public Set<String> getPermbannedNames()
    {
        return permbannedNames;
    }

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
            List<PermBan> loadedPermbans = repo.findAll().join();
            
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
        permbannedNames.clear();
        permbannedIps.clear();
        permbansByName.clear();

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
        
        try
        {
            PermbanRepository repo = plugin.dm.getPermbanRepository();
            for (PermBan permban : permbansByName.values())
            {
                repo.save(permban).join();
            }
            FLog.debug("Saved " + permbansByName.size() + " permbans to SQL database");
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to save permbans to SQL: " + ex.getMessage());
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
        String name = permban.getUsername().toLowerCase().trim();
        permbannedNames.add(name);
        permbannedIps.addAll(permban.getIps());
        permbansByName.put(name, permban);
        
        if (usingSql && plugin.dm != null && plugin.dm.isInitialized())
        {
            try
            {
                plugin.dm.getPermbanRepository().save(permban).join();
            }
            catch (Exception ex)
            {
                FLog.warning("Failed to save permban to SQL: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Remove a permban by username.
     */
    public boolean removePermban(String username)
    {
        String name = username.toLowerCase().trim();
        PermBan permban = permbansByName.remove(name);
        if (permban == null)
        {
            return false;
        }
        
        permbannedNames.remove(name);
        // Remove IPs associated with this permban
        permbannedIps.removeAll(permban.getIps());
        
        if (usingSql && plugin.dm != null && plugin.dm.isInitialized())
        {
            try
            {
                plugin.dm.getPermbanRepository().deleteByUsername(name).join();
            }
            catch (Exception ex)
            {
                FLog.warning("Failed to remove permban from SQL: " + ex.getMessage());
            }
        }
        
        return true;
    }
    
    /**
     * Get a permban by username.
     */
    public PermBan getPermban(String username)
    {
        return permbansByName.get(username.toLowerCase().trim());
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
                        ChatColor.RED + "Your IP address is permanently banned from this server.\n"
                        + "Release procedures are available at\n"
                        + ChatColor.GOLD + ConfigEntry.SERVER_PERMBAN_URL.getString());
                return;
            }
        }

        // Permbanned usernames
        for (String testPlayer : getPermbannedNames())
        {
            if (testPlayer.equalsIgnoreCase(username))
            {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        ChatColor.RED + "Your username is permanently banned from this server.\n"
                        + "Release procedures are available at\n"
                        + ChatColor.GOLD + ConfigEntry.SERVER_PERMBAN_URL.getString());
                return;
            }
        }

    }

}
