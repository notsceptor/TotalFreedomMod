package me.totalfreedom.totalfreedommod.banning;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.sql.adapter.BanRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.totalfreedommod.util.JsonUtil;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class BanManager extends FreedomService
{
    private static final Type BAN_LIST_TYPE = new TypeToken<List<Ban>>() {}.getType();

    private final Set<Ban> bans = Sets.newHashSet();
    private final Map<String, Ban> ipBans = Maps.newHashMap();
    private final Map<String, Ban> nameBans = Maps.newHashMap();
    private final List<String> unbannableUsernames = Lists.newArrayList();
    //
    private final File configFile;

    private final Object lock = new Object();
    private final Object persistenceLock = new Object();

    // Flag to track if SQL is available
    private boolean usingSql = false;

    public BanManager(TotalFreedomMod plugin)
    {
        super(plugin);
        this.configFile = new File(plugin.getDataFolder(), "bans.json");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onStart()
    {
        // Try to load from SQL database first
        if (plugin.dm != null && plugin.dm.isInitialized())
        {
            loadFromSql();
        }
        else
        {
            loadFromJson();
        }

        // Load unbannable usernames
        unbannableUsernames.clear();
        unbannableUsernames.addAll((Collection<? extends String>) ConfigEntry.FAMOUS_PLAYERS.getList());
        FLog.info("Loaded " + unbannableUsernames.size() + " unbannable usernames.");
    }

    /**
     * Load bans from SQL database.
     */
    private void loadFromSql()
    {
        try
        {
            BanRepository repo = plugin.dm.getBanRepository();
            List<Ban> loadedBans = repo.findAll().block();

            synchronized (lock)
            {
                bans.clear();
                bans.addAll(loadedBans);
                usingSql = true;
                updateViews();
                FLog.info("Loaded " + ipBans.size() + " IP bans and " + nameBans.size() + " username bans from SQL database.");
            }

            reconcileFromJsonIfNewer(repo);
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to load bans from SQL, falling back to JSON: " + ex.getMessage());
            loadFromJson();
        }
    }

    /**
     * If bans.json was written more recently than the database's last update, re-import it into SQL.
     */
    private void reconcileFromJsonIfNewer(BanRepository repo)
    {
        if (!configFile.exists())
        {
            return;
        }

        try
        {
            Long sqlUpdatedAt = repo.getMaxUpdatedAt();
            if (sqlUpdatedAt != null && configFile.lastModified() <= sqlUpdatedAt)
            {
                return;
            }

            List<Ban> jsonBans = readJsonBans();
            if (jsonBans.isEmpty())
            {
                return;
            }

            FLog.info("bans.json is newer than the database; re-importing " + jsonBans.size() + " ban(s) from it.");
            for (Ban ban : jsonBans)
            {
                if (!ban.isValid())
                {
                    continue;
                }
                repo.save(ban).block();
            }

            synchronized (lock)
            {
                bans.clear();
                bans.addAll(jsonBans);
                updateViews();
            }
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to reconcile bans.json into the database: " + ex.getMessage());
        }
    }

    private List<Ban> readJsonBans() throws IOException
    {
        try (FileReader reader = new FileReader(configFile))
        {
            List<Ban> loaded = JsonUtil.GSON.fromJson(reader, BAN_LIST_TYPE);
            return loaded != null ? loaded : new ArrayList<>();
        }
    }

    /**
     * Load bans from the JSON file (fallback).
     */
    private void loadFromJson()
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
                FLog.severe("Could not create bans.json");
            }
        }

        synchronized (lock)
        {
            bans.clear();
            try
            {
                for (Ban ban : readJsonBans())
                {
                    if (!ban.isValid())
                    {
                        FLog.warning("Not adding username ban: " + ban.getUsername() + ". Missing information.");
                        continue;
                    }
                    bans.add(ban);
                }
            }
            catch (IOException ex)
            {
                FLog.severe("Could not read bans.json: " + ex.getMessage());
            }

            usingSql = false;
            updateViews();
            FLog.info("Loaded " + ipBans.size() + " IP bans and " + nameBans.size() + " username bans from JSON.");
        }
    }

    @Override
    protected void onStop()
    {
        saveAll();
        FLog.info("Saved " + bans.size() + " player bans");
    }

    public Set<Ban> getAllBans()
    {
        return Collections.unmodifiableSet(bans);
    }

    public Collection<Ban> getIpBans()
    {
        return Collections.unmodifiableCollection(ipBans.values());
    }

    public Collection<Ban> getUsernameBans()
    {
        return Collections.unmodifiableCollection(nameBans.values());
    }

    public void saveAll()
    {
        final boolean sql;
        final List<Ban> snapshot;
        synchronized (lock)
        {
            updateViews();
            sql = usingSql;
            snapshot = new ArrayList<>(bans);
        }

        synchronized (persistenceLock)
        {
            if (sql)
            {
                writeAllToSql(snapshot);
            }
            else
            {
                writeAllToJson(snapshot);
            }
        }
    }

    public void saveAllAsync()
    {
        if (!plugin.isEnabled())
        {
            saveAll();
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveAll);
    }

    private void saveBanToSqlAsync(Ban ban)
    {
        if (!plugin.isEnabled())
        {
            saveBanToSql(ban);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveBanToSql(ban));
    }

    private void removeBanFromSqlAsync(Ban ban)
    {
        if (!plugin.isEnabled())
        {
            removeBanFromSql(ban);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> removeBanFromSql(ban));
    }

    /**
     * Write the given snapshot of bans to the SQL database. Must be called under persistenceLock.
     */
    private void writeAllToSql(List<Ban> snapshot)
    {
        if (plugin.dm == null || !plugin.dm.isInitialized())
        {
            FLog.warning("SQL not available, falling back to JSON save");
            writeAllToJson(snapshot);
            return;
        }

        try
        {
            BanRepository repo = plugin.dm.getBanRepository();
            // Clear and re-add all (simple approach for now)
            repo.deleteAll().block();
            for (Ban ban : snapshot)
            {
                repo.save(ban).block();
            }
            FLog.debug("Saved " + snapshot.size() + " bans to SQL database");
            writeAllToJson(snapshot);
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to save bans to SQL: " + ex.getMessage());
        }
    }

    /**
     * Write the given snapshot of bans to the JSON file (fallback, and the write-through
     * snapshot when using SQL). Must be called under persistenceLock.
     */
    private void writeAllToJson(List<Ban> snapshot)
    {
        try (FileWriter writer = new FileWriter(configFile))
        {
            JsonUtil.GSON.toJson(snapshot, BAN_LIST_TYPE, writer);
        }
        catch (IOException ex)
        {
            FLog.severe("Could not save bans.json");
        }
    }

    public Ban getByIp(String ip)
    {
        synchronized (lock)
        {
            final Ban directBan = ipBans.get(ip);
            if (directBan != null && !directBan.isExpired())
            {
                return directBan;
            }

            // Match fuzzy IP
            for (Ban loopBan : ipBans.values())
            {
                if (loopBan.isExpired())
                {
                    continue;
                }

                for (String loopIp : loopBan.getIps())
                {
                    if (!loopIp.contains("*"))
                    {
                        continue;
                    }

                    if (FUtil.fuzzyIpMatch(ip, loopIp, 4))
                    {
                        return loopBan;
                    }
                }
            }

            return null;
        }
    }

    public Ban getByUsername(String username)
    {
        synchronized (lock)
        {
            username = username.toLowerCase();
            final Ban directBan = nameBans.get(username);

            if (directBan != null && !directBan.isExpired())
            {
                return directBan;
            }

            return null;
        }
    }

    public Ban unbanIp(String ip)
    {
        final Ban ban;
        final boolean sql;
        synchronized (lock)
        {
            ban = getByIp(ip);
            if (ban != null)
            {
                bans.remove(ban);
                updateViews();
            }
            sql = usingSql;
        }

        if (ban != null)
        {
            if (sql)
            {
                removeBanFromSqlAsync(ban);
            }
            else
            {
                saveAllAsync();
            }
        }

        return ban;
    }

    public Ban unbanUsername(String username)
    {
        final Ban ban;
        final boolean sql;
        synchronized (lock)
        {
            ban = getByUsername(username);
            if (ban != null)
            {
                bans.remove(ban);
                updateViews();
            }
            sql = usingSql;
        }

        if (ban != null)
        {
            if (sql)
            {
                removeBanFromSqlAsync(ban);
            }
            else
            {
                saveAllAsync();
            }
        }

        return ban;
    }

    public boolean isIpBanned(String ip)
    {
        return getByIp(ip) != null;
    }

    public boolean isUsernameBanned(String username)
    {
        return getByUsername(username) != null;
    }

    public boolean addBan(Ban ban)
    {
        cancelWorldEditFor(ban);

        final boolean added;
        final boolean sql;
        synchronized (lock)
        {
            added = bans.add(ban);
            if (added)
            {
                updateViews();
            }
            sql = usingSql;
        }

        if (added)
        {
            if (sql)
            {
                saveBanToSqlAsync(ban);
            }
            else
            {
                saveAllAsync();
            }
        }

        return added;
    }

    private void cancelWorldEditFor(Ban ban)
    {
        if (plugin.web == null)
        {
            return;
        }
        Player player = null;
        if (ban.getUuid() != null)
        {
            player = server.getPlayer(ban.getUuid());
        }
        if (player == null && ban.getUsername() != null)
        {
            player = server.getPlayerExact(ban.getUsername());
        }
        if (player != null)
        {
            plugin.web.cancel(player);
        }
    }

    /**
     * Save a single ban to SQL database.
     */
    private void saveBanToSql(Ban ban)
    {
        if (plugin.dm == null || !plugin.dm.isInitialized())
        {
            return;
        }

        synchronized (persistenceLock)
        {
            try
            {
                plugin.dm.getBanRepository().save(ban).block();
                writeAllToJson(currentBansSnapshot());
            }
            catch (Exception ex)
            {
                FLog.warning("Failed to save ban to SQL: " + ex.getMessage());
            }
        }
    }

    /**
     * Remove a ban from SQL database.
     */
    private void removeBanFromSql(Ban ban)
    {
        if (plugin.dm == null || !plugin.dm.isInitialized())
        {
            return;
        }

        synchronized (persistenceLock)
        {
            try
            {
                if (ban.getUuid() != null)
                {
                    plugin.dm.getBanRepository().deleteByUuid(ban.getUuid()).block();
                }
                else if (ban.hasUsername())
                {
                    plugin.dm.getBanRepository().deleteByUsername(ban.getUsername());
                }
                writeAllToJson(currentBansSnapshot());
            }
            catch (Exception ex)
            {
                FLog.warning("Failed to remove ban from SQL: " + ex.getMessage());
            }
        }
    }

    public boolean removeBan(Ban ban)
    {
        final boolean removed;
        final boolean sql;
        synchronized (lock)
        {
            removed = bans.remove(ban);
            if (removed)
            {
                updateViews();
            }
            sql = usingSql;
        }

        if (removed)
        {
            if (sql)
            {
                removeBanFromSqlAsync(ban);
            }
            else
            {
                saveAllAsync();
            }
        }

        return removed;
    }

    public int purge()
    {
        final int size;
        synchronized (lock)
        {
            size = bans.size();
            bans.clear();
            updateViews();
        }

        saveAllAsync();

        return size;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event)
    {
        final String username = event.getName();
        final String ip = event.getAddress().getHostAddress().trim();

        // Regular ban
        Ban ban = getByUsername(username);
        if (ban == null)
        {
            ban = getByIp(ip);
        }

        if (ban != null && !ban.isExpired())
        {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ban.bakeKickMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();
        final PlayerData data = plugin.pl.getData(player);

        if (!plugin.al.isAdmin(player))
        {
            return;
        }

        // Unban admins
        for (String storedIp : data.getIps())
        {
            unbanIp(storedIp);
            unbanIp(FUtil.getFuzzyIp(storedIp));
        }

        unbanUsername(player.getName());
        player.setOp(true);
    }

    private List<Ban> currentBansSnapshot()
    {
        synchronized (lock)
        {
            return new ArrayList<>(bans);
        }
    }

    // Must be called while holding 'lock'.
    private void updateViews()
    {
        // Remove expired bans
        for (Iterator<Ban> it = bans.iterator(); it.hasNext();)
        {
            if (it.next().isExpired())
            {
                it.remove();
            }
        }

        nameBans.clear();
        ipBans.clear();
        for (Ban ban : bans)
        {
            if (ban.hasUsername())
            {
                nameBans.put(ban.getUsername().toLowerCase(), ban);
            }

            if (ban.hasIps())
            {
                for (String ip : ban.getIps())
                {
                    ipBans.put(ip, ban);
                }
            }
        }
    }

}
