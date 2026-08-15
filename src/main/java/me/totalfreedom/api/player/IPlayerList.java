package me.totalfreedom.api.player;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.player.FPlayer;

public interface IPlayerList
{
    Map<String, FPlayer> getPlayerMap();

    Map<String, PlayerData> getDataMap();

    File getConfigFolder();

    /**
     * Queue a write of every loaded player record.
     */
    void save();

    void saveAsync();

    void saveData(PlayerData data);

    void clearSavedTag(Player player);

    boolean saveCurrentTag(Player player);

    FPlayer getPlayerSync(Player player);

    String getIp(OfflinePlayer player);

    // May not return null
    FPlayer getPlayer(Player player);

    // May not return null
    PlayerData getData(Player player);

    // May return null
    PlayerData getData(String username);

    Collection<PlayerData> getAllData();

    Collection<FPlayer> getLoadedPlayers();

    Collection<PlayerData> getLoadedData();

    int purgeAllData();
}
