package me.totalfreedom.totalfreedommod.player;

import com.google.common.collect.Maps;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import lombok.Getter;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import java.io.IOException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerList extends FreedomService
{

    public static final long AUTO_PURGE_TICKS = 20L * 60L * 5L;
    //
    @Getter
    public final Map<String, FPlayer> playerMap = Maps.newHashMap(); // ip,dataMap
    @Getter
    public final Map<String, PlayerData> dataMap = Maps.newHashMap(); // ip,dataMap
    private final File configFolder;
    
    // Manual getter - Lombok @Getter not processing reliably
    public File getConfigFolder()
    {
        return configFolder;
    }

    public PlayerList(TotalFreedomMod plugin)
    {
        super(plugin);

        this.configFolder = new File(plugin.getDataFolder(), "players");
    }

    @Override
    protected void onStart()
    {
        playerMap.clear();
        dataMap.clear();

        // Preload online players
        for (Player player : server.getOnlinePlayers())
        {
            getPlayer(player);
        }
    }

    @Override
    protected void onStop()
    {
        save();
    }

    public void save()
    {
        for (PlayerData data : dataMap.values())
        {
            saveOne(data);
        }
    }

    public void saveAsync()
    {
        if (!plugin.isEnabled())
        {
            save();
            return;
        }
        final java.util.List<PlayerData> snapshot = new java.util.ArrayList<>(dataMap.values());
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
        {
            for (PlayerData data : snapshot)
            {
                saveOne(data);
            }
        });
    }

    private void saveOne(PlayerData data)
    {
        final YamlConfiguration config = getConfig(data);
        data.saveTo(config);
        try
        {
            config.save(getConfigFile(data.getUsername().toLowerCase()));
        }
        catch (IOException ex)
        {
            FLog.severe("Could not save player data for " + data.getUsername());
        }
    }

    public FPlayer getPlayerSync(Player player)
    {
        synchronized (playerMap)
        {
            return getPlayer(player);
        }
    }

    public String getIp(OfflinePlayer player)
    {
        if (player.isOnline())
        {
            return player.getPlayer().getAddress().getAddress().getHostAddress();
        }

        final PlayerData entry = getData(player.getName());

        return (entry == null ? null : entry.getIps().iterator().next());
    }

    // May not return null
    public FPlayer getPlayer(Player player)
    {
        FPlayer tPlayer = playerMap.get(player.getAddress().getAddress().getHostAddress());
        if (tPlayer != null)
        {
            return tPlayer;
        }

        tPlayer = new FPlayer(plugin, player);
        playerMap.put(player.getAddress().getAddress().getHostAddress(), tPlayer);

        return tPlayer;
    }

    // May not return null
    public PlayerData getData(Player player)
    {
        // Check already loaded
        PlayerData data = dataMap.get(player.getAddress().getAddress().getHostAddress());
        if (data != null)
        {
            return data;
        }

        // Load data
        data = getData(player.getName());

        // Create data if nonexistent
        if (data == null)
        {
            FLog.info("Creating new player data entry for " + player.getName());

            // Create new player
            final long unix = FUtil.getUnixTime();
            data = new PlayerData(player);
            data.setFirstJoinUnix(unix);
            data.setLastJoinUnix(unix);
            data.addIp(player.getAddress().getAddress().getHostAddress());

            // Store player
            dataMap.put(player.getName().toLowerCase(), data);

            // Save player
            YamlConfiguration config = getConfig(data);
            data.saveTo(config);
            try
            {
                config.save(getConfigFile(data.getUsername().toLowerCase()));
            }
            catch (IOException ex)
            {
                FLog.severe("Could not save player data for " + data.getUsername());
            }
        }

        return data;
    }

    // May return null
    public PlayerData getData(String username)
    {
        username = username.toLowerCase();

        // Check if the player is a known player
        final File configFile = getConfigFile(username);
        if (!configFile.exists())
        {
            return null;
        }

        // Create and load entry
        final PlayerData data = new PlayerData(username);
        data.loadFrom(getConfig(data));

        if (!data.isValid())
        {
            FLog.warning("Could not load player data entry: " + username + ". Entry is not valid!");
            configFile.delete();
            return null;
        }

        // Only store data if the player is online
        final Player onlinePlayer = Bukkit.getPlayerExact(data.getUsername());
        if (onlinePlayer != null)
        {
            final String onlineIp = onlinePlayer.getAddress().getAddress().getHostAddress();
            if (data.getIps().contains(onlineIp))
            {
                dataMap.put(onlineIp, data);
            }
        }

        return data;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        final String ip = event.getPlayer().getAddress().getAddress().getHostAddress();
        playerMap.remove(ip);
        final PlayerData data = dataMap.remove(ip);

        if (data != null && plugin.isEnabled())
        {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveOne(data));
        }
    }

    public Collection<FPlayer> getLoadedPlayers()
    {
        return playerMap.values();
    }

    public Collection<PlayerData> getLoadedData()
    {
        return dataMap.values();
    }

    public int purgeAllData()
    {
        int deleted = 0;
        for (File file : getConfigFolder().listFiles())
        {
            deleted += file.delete() ? 1 : 0;
        }

        dataMap.clear();
        return deleted;
    }

    protected File getConfigFile(String name)
    {
        return new File(getConfigFolder(), name + ".yml");
    }

    protected YamlConfiguration getConfig(PlayerData data)
    {
        final File configFile = getConfigFile(data.getUsername().toLowerCase());
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        return config;
    }
}
