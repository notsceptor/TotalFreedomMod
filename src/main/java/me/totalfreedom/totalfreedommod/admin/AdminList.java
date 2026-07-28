package me.totalfreedom.totalfreedommod.admin;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import java.nio.charset.StandardCharsets;
import me.totalfreedom.totalfreedommod.sql.adapter.AdminRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.totalfreedommod.util.JsonUtil;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import org.bukkit.Bukkit;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;

public class AdminList extends FreedomService
{

    public static final String CONFIG_FILENAME = "admins.json";

    private static final Type ADMIN_MAP_TYPE = new TypeToken<Map<String, Admin>>() {}.getType();

    private static final long LAST_LOGIN_DEBOUNCE_MS = 5L * 60L * 1000L;

    private static final long SHUTDOWN_FLUSH_TIMEOUT_MS = 10L * 1000L;

    private final Map<String, Admin> allAdmins = Maps.newHashMap(); // Includes disabled admins
    // Only active admins below
    private final Set<Admin> activeAdmins = Sets.newHashSet();

    // UUID-based lookup table
    private final Map<UUID, Admin> uuidTable = Maps.newHashMap();
    private final Map<String, Admin> nameTable = Maps.newHashMap();
    private final Map<String, Admin> ipTable = Maps.newHashMap();
    private final Set<Player> onlineAdminPlayers = Sets.newHashSet();
    //
    private final File configFile;

    // Flag to track if SQL is available
    private boolean usingSql = false;
    private final Object persistenceLock = new Object();
    private Mono<Void> persistenceChain = Mono.empty();

    public AdminList(TotalFreedomMod plugin)
    {
        super(plugin);

        this.configFile = new File(plugin.getDataFolder(), CONFIG_FILENAME);
    }

    @Override
    protected void onStart()
    {
        load();

        server.getServicesManager().register(Function.class, new Function<Player, Boolean>()
        {
            @Override
            public Boolean apply(Player player)
            {
                return isAdmin(player);
            }
        }, plugin, ServicePriority.Normal);

        deactivateOldEntries(false);
    }

    @Override
    protected void onStop()
    {
        // Let the queue drain first, then flush everything. Ordering matters:
        // a queued write landing after the flush would restore a stale snapshot.
        awaitPendingWrites(SHUTDOWN_FLUSH_TIMEOUT_MS);
        save();
    }

    public void load()
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

        if (ConfigEntry.ADMINLIST_USE_UUID_ONLY.getBoolean())
        {
            getMissingUuids();
        }
    }

    /**
     * Best-effort UUID backfill for admin records loaded without a stored UUID.
     */
    private void getMissingUuids()
    {
        int resolved = 0;
        int offlineDerived = 0;
        boolean mojangLookup = ConfigEntry.ADMINLIST_MOJANG_UUID_LOOKUP.getBoolean();
        final List<Admin> backfilled = new ArrayList<>();

        for (Admin admin : allAdmins.values())
        {
            if (admin.getUuid() != null)
            {
                continue;
            }

            UUID uuid = FUtil.usernameToUuid(admin.getName());
            if (uuid != null)
            {
                resolved++;
            }
            else
            {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + admin.getName().toLowerCase()).getBytes(StandardCharsets.UTF_8));
                offlineDerived++;
            }
            admin.setUuid(uuid);
            backfilled.add(admin);
        }

        if (backfilled.isEmpty())
        {
            return;
        }

        FLog.info("UUID backfill: " + resolved + " resolved via Mojang, " + offlineDerived + " offline-derived");
        if (offlineDerived > 0 && !mojangLookup)
        {
            FLog.warning("use_uuid_only is enabled but mojang_uuid_lookup is disabled; "
                    + offlineDerived + " admin record(s) fell back to offline-derived UUIDs and "
                    + "will not match premium accounts on login");
        }

        updateTables();

        // SQL can persist just the rows we touched; YAML is a whole-file format
        // so one bulk write beats N rewrites of the same file.
        if (usingSql)
        {
            backfilled.forEach(this::saveAdminAsync);
        }
        else
        {
            saveAsync();
        }
    }
    
    /**
     * Load admins from SQL database.
     */
    private void loadFromSql()
    {
        try
        {
            AdminRepository repo = plugin.dm.getAdminRepository();
            List<Admin> admins = repo.findAll().block();
            
            allAdmins.clear();
            for (Admin admin : admins)
            {
                String key = admin.getName().toLowerCase();
                admin = fixConfigKey(admin, key);
                allAdmins.put(key, admin);
            }
            
            usingSql = true;
            updateTables();
            FLog.info("Loaded " + allAdmins.size() + " admins from SQL database (" + nameTable.size() + " active, " + ipTable.size() + " IPs)");

            reconcileFromJsonIfNewer(repo);
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to load admins from SQL, falling back to JSON: " + ex.getMessage());
            loadFromJson();
        }
    }

    /**
     * If admins.json was written more recently than the database's last update (e.g. edited
     * by hand, or restored from backup while SQL was unavailable), re-import it into SQL.
     */
    private void reconcileFromJsonIfNewer(AdminRepository repo)
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

            Map<String, Admin> jsonAdmins = readJsonAdmins();
            if (jsonAdmins.isEmpty())
            {
                return;
            }

            FLog.info("admins.json is newer than the database; re-importing " + jsonAdmins.size() + " admin(s) from it.");
            for (Admin admin : jsonAdmins.values())
            {
                if (!admin.isValid())
                {
                    continue;
                }
                UUID uuid = resolveUuid(admin);
                admin.setUuid(uuid);
                repo.save(uuid, admin).block();
            }

            allAdmins.clear();
            allAdmins.putAll(jsonAdmins);
            updateTables();
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to reconcile " + CONFIG_FILENAME + " into the database: " + ex.getMessage());
        }
    }

    /**
     * Resolve a UUID for an admin missing one: Mojang lookup by name, falling back to an
     * offline-derived UUID.
     */
    private UUID resolveUuid(Admin admin)
    {
        if (admin.getUuid() != null)
        {
            return admin.getUuid();
        }
        UUID uuid = FUtil.usernameToUuid(admin.getName());
        if (uuid == null)
        {
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + admin.getName().toLowerCase()).getBytes(StandardCharsets.UTF_8));
        }
        return uuid;
    }

    private Map<String, Admin> readJsonAdmins() throws IOException
    {
        try (FileReader reader = new FileReader(configFile))
        {
            Map<String, Admin> admins = JsonUtil.GSON.fromJson(reader, ADMIN_MAP_TYPE);
            return admins != null ? admins : Maps.newHashMap();
        }
    }
    
    /**
     * Fix the config key on an admin (needed when loading from SQL).
     */
    private Admin fixConfigKey(Admin admin, String key)
    {
        // Use reflection or create new admin to set configKey
        // Since configKey is private with no setter, we need to recreate
        if (admin.getConfigKey() == null || !admin.getConfigKey().equals(key))
        {
            Admin fixed = new Admin(key);
            fixed.setUuid(admin.getUuid());
            fixed.setName(admin.getName());
            fixed.setRank(admin.getRank());
            fixed.setActive(admin.isActive());
            fixed.setLastLogin(admin.getLastLogin());
            fixed.setLoginMessage(admin.getLoginMessage());
            fixed.setCustomRankId(admin.getCustomRankId());
            fixed.addIps(admin.getIps());
            return fixed;
        }
        return admin;
    }
    
    /**
     * Load admins from JSON file (fallback).
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
                FLog.severe("Could not create " + CONFIG_FILENAME);
            }
        }

        allAdmins.clear();
        try
        {
            for (Map.Entry<String, Admin> entry : readJsonAdmins().entrySet())
            {
                Admin admin = entry.getValue();
                if (admin == null || !admin.isValid())
                {
                    FLog.warning("Could not load admin: " + entry.getKey() + ". Missing details!");
                    continue;
                }
                allAdmins.put(entry.getKey(), admin);
            }
        }
        catch (IOException ex)
        {
            FLog.severe("Could not read " + CONFIG_FILENAME + ": " + ex.getMessage());
        }

        usingSql = false;
        updateTables();
        FLog.info("Loaded " + allAdmins.size() + " admins from JSON (" + nameTable.size() + " active, " + ipTable.size() + " IPs)");
    }

    /**
     * Blocking write of every admin record. This is the shutdown flush - it is
     * called from {@link #onStop()} so nothing is lost when the server stops.
     * <p>
     * Do <b>not</b> call this from a command or event handler: under SQL it is a
     * serial round-trip per admin on the calling thread, which stalls the main
     * thread for as long as the whole list takes to write. Single-entry changes
     * belong on {@link #saveAdminAsync(Admin)}.
     */
    public synchronized void save()
    {
        if (usingSql)
        {
            saveToSql();
        }
        else
        {
            saveToJson();
        }
    }

    /**
     * Wait for queued {@link #saveAdminAsync(Admin)} writes to land, up to
     * {@code timeoutMs}. Without this a shutdown flush can race the queue and
     * let an older queued snapshot overwrite the state we just wrote.
     */
    public void awaitPendingWrites(long timeoutMs)
    {
        final CompletableFuture<Void> pending;
        synchronized (persistenceLock)
        {
            pending = persistenceChain;
        }

        try
        {
            pending.get(timeoutMs, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException ex)
        {
            FLog.warning("Timed out after " + timeoutMs + "ms waiting for pending admin writes; flushing anyway");
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
        catch (Exception ex)
        {
            FLog.warning("A queued admin write failed before shutdown: " + ex.getMessage());
        }
    }

    /**
     * Persist <i>every</i> admin record on a worker thread. Under SQL this is a
     * fan-out: one queued write per admin, whether or not it changed.
     * <p>
     * Only call this when the whole list is genuinely dirty, or when running on
     * YAML (a whole-file format that cannot be written piecemeal). If a single
     * entry changed - a login, an IP edit, a rank change - call
     * {@link #saveAdminAsync(Admin)} instead, which queues just that row.
     */
    public void saveAsync()
    {
        if (usingSql)
        {
            for (Admin admin : List.copyOf(allAdmins.values()))
            {
                saveAdminAsync(admin);
            }
            return;
        }

        // Render on this thread while we still own the maps, then hand the
        // finished text to the worker. Serialising inside the async task would
        // read allAdmins off-thread while the main thread is free to mutate it.
        final String data = serialiseAdmins();

        if (!plugin.isEnabled())
        {
            saveToJson();
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
        {
            synchronized (AdminList.this)
            {
                saveToJson();
            }
        });
    }

    public void saveAdminAsync(Admin admin)
    {
        if (admin == null)
        {
            return;
        }

        if (!usingSql)
        {
            saveAsync();
            return;
        }

        if (plugin.dm == null || !plugin.dm.isInitialized())
        {
            FLog.warning("SQL not available; admin change was not saved for " + admin.getName());
            return;
        }

        final Admin snapshot = copyAdmin(admin);

        synchronized (persistenceLock)
        {
            persistenceChain = persistenceChain
                    .onErrorResume(ignored -> Mono.empty())
                    .then(plugin.dm.getAdminRepository().save(finalUuid, snapshot))
                    .onErrorResume(ex ->
                    {
                        FLog.warning("Failed to save admin " + snapshot.getName() + " to SQL: " + ex.getMessage());
                        return Mono.empty();
                    })
                    .then(Mono.fromRunnable(this::saveToJson))
                    .then()
                    .cache();
            persistenceChain.subscribe();
        }
    }

    /**
     * Resolve the UUID for a queued write. Runs on the persistence chain rather
     * than the caller, because {@link FUtil#usernameToUuid} can make a blocking
     * Mojang request with a 5s connect and 5s read timeout - that must never
     * land on the main thread during play. The resolved value is handed back to
     * the live entry on the main thread, which owns the lookup tables.
     */
    private CompletableFuture<UUID> resolveUuid(Admin live, Admin snapshot)
    {
        if (snapshot.getUuid() != null)
        {
            return CompletableFuture.completedFuture(snapshot.getUuid());
        }

        UUID resolved = FUtil.usernameToUuid(snapshot.getName());
        if (resolved == null)
        {
            resolved = UUID.nameUUIDFromBytes(("OfflinePlayer:" + snapshot.getName().toLowerCase()).getBytes(StandardCharsets.UTF_8));
        }

        snapshot.setUuid(resolved);

        final UUID finalResolved = resolved;
        try
        {
            if (plugin.isEnabled())
            {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                {
                    if (live.getUuid() == null)
                    {
                        live.setUuid(finalResolved);
                        uuidTable.put(finalResolved, live);
                    }
                });
            }
        }
        catch (RuntimeException ex)
        {
            // Plugin disabled between the check and the schedule; Bukkit answers
            // that with IllegalPluginAccessException. The snapshot already holds
            // the UUID, so let the write below proceed regardless.
            FLog.debug("Could not sync resolved UUID back to " + snapshot.getName() + "; server is stopping");
        }

        return CompletableFuture.completedFuture(finalResolved);
    }

    private Admin copyAdmin(Admin admin)
    {
        Admin copy = new Admin(admin.getConfigKey());
        copy.setUuid(admin.getUuid());
        copy.setName(admin.getName());
        copy.setRank(admin.getRank());
        copy.setActive(admin.isActive());
        copy.setLastLogin(admin.getLastLogin() == null ? null : new Date(admin.getLastLogin().getTime()));
        copy.setLoginMessage(admin.getLoginMessage());
        copy.setCustomRankId(admin.getCustomRankId());
        copy.addIps(new ArrayList<>(admin.getIps()));
        return copy;
    }
    
    /**
     * Save all admins to SQL database.
     */
    private void saveToSql()
    {
        if (plugin.dm == null || !plugin.dm.isInitialized())
        {
            FLog.warning("SQL not available, falling back to YAML save");
            saveToJson();
            return;
        }
        
        final AdminRepository repo = plugin.dm.getAdminRepository();
        int saved = 0;
        int failed = 0;

        // Isolate per admin: this is the shutdown flush, so one unwritable row
        // must not take the rest of the list down with it.
        for (Admin admin : allAdmins.values())
        {
            try
            {
                UUID uuid = admin.getUuid();
                if (uuid == null)
                {
                    // Generate UUID if not present. Blocking Mojang lookup is
                    // acceptable here - this only runs at startup/shutdown.
                    uuid = FUtil.usernameToUuid(admin.getName());
                    if (uuid == null)
                    {
                        uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + admin.getName().toLowerCase()).getBytes(StandardCharsets.UTF_8));
                    }
                    admin.setUuid(uuid);
                }
                repo.save(uuid, admin).block();
            }
            FLog.debug("Saved " + allAdmins.size() + " admins to SQL database");
            saveToJson();
        }

        // Don't fall back to YAML on failure - we don't want conflicting data.
        if (failed > 0)
        {
            FLog.warning("Failed to save admins to SQL: " + ex.getMessage());
            // Don't fall back to JSON here - we don't want to create conflicting data
        }
    }
    
    /**
     * Save all admins to the JSON file (fallback, and the write-through snapshot when using SQL).
     */
    private void saveToJson()
    {
        try (FileWriter writer = new FileWriter(configFile))
        {
            JsonUtil.GSON.toJson(allAdmins, ADMIN_MAP_TYPE, writer);
        }
        catch (IOException ex)
        {
            FLog.severe("Could not save " + CONFIG_FILENAME);
        }
    }

    /**
     * Save all admins to YAML file (fallback). Blocking - startup/shutdown only.
     */
    private void saveToYaml()
    {
        writeYaml(serialiseAdmins());
    }

    public synchronized boolean isAdminSync(CommandSender sender)
    {
        return isAdmin(sender);
    }

    public boolean isAdmin(CommandSender sender)
    {
        if (!(sender instanceof Player))
        {
            return true;
        }

        Admin admin = getAdmin((Player) sender);

        return admin != null && admin.isActive();
    }

    public boolean isSeniorAdmin(CommandSender sender)
    {
        Admin admin = getAdmin(sender);
        if (admin == null)
        {
            return false;
        }

        return admin.getRank().ordinal() >= Rank.SENIOR_ADMIN.ordinal();
    }

    public Admin getAdmin(CommandSender sender)
    {
        if (sender instanceof Player player) // this instead of two separate methods. 
        {
            if (ConfigEntry.ADMINLIST_USE_UUID_ONLY.getBoolean())
            {
                Admin uuidAdmin = uuidTable.get(player.getUniqueId());
                if (uuidAdmin == null || !uuidAdmin.isActive())
                {
                    return null;
                }
                // Rewrite the stored display name if Mojang has changed it.
                if (!uuidAdmin.getName().equalsIgnoreCase(player.getName()))
                {
                    final String oldKey = uuidAdmin.getName().toLowerCase();
                    uuidAdmin.setName(player.getName());
                    final String newKey = uuidAdmin.getName().toLowerCase();
                    if (!oldKey.equals(newKey))
                    {
                        nameTable.remove(oldKey);
                        nameTable.put(newKey, uuidAdmin);
                    }
                    saveAdminAsync(uuidAdmin);
                }
                return uuidAdmin;
            }
    
            // Find admin
            final String ip = player.getAddress().getAddress().getHostAddress();
            Admin admin = getEntryByName(player.getName());
    
            // Admin by name
            if (admin != null)
            {
                // Check if we're in online mode,
                // Or the players IP is in the admin entry
                if (Bukkit.getOnlineMode() || admin.getIps().contains(ip))
                {
                    if (!admin.getIps().contains(ip))
                    {
                        // Add the new IP if we have to
                        admin.addIp(ip);
                        ipTable.put(ip, admin);
                        saveAdminAsync(admin);
                    }
                    return admin;
                }
    
                // Impostor
            }
    
            // Admin by ip
            admin = getEntryByIp(ip);
            if (admin != null)
            {
                // Set the new username
                final String oldKey = admin.getName().toLowerCase();
                admin.setName(player.getName());
                final String newKey = admin.getName().toLowerCase();
                if (!oldKey.equals(newKey))
                {
                    nameTable.remove(oldKey);
                    if (admin.isActive())
                    {
                        nameTable.put(newKey, admin);
                    }
                }
                saveAdminAsync(admin);
            }
    
            return null;
        }

        return getEntryByName(sender.getName());
    }

    public Admin getEntryByName(String name)
    {
        return nameTable.get(name.toLowerCase());
    }

    public Admin getEntryByIp(String ip)
    {
        return ipTable.get(ip);
    }

    public Admin getEntryByIpFuzzy(String needleIp)
    {
        final Admin directAdmin = getEntryByIp(needleIp);
        if (directAdmin != null)
        {
            return directAdmin;
        }

        for (String ip : ipTable.keySet())
        {
            if (FUtil.fuzzyIpMatch(needleIp, ip, 3))
            {
                return ipTable.get(ip);
            }
        }

        return null;
    }

    public void updateLastLogin(Player player)
    {
        final Admin admin = getAdmin(player);
        if (admin == null)
        {
            return;
        }

        final Date now = new Date();
        final Date then = admin.getLastLogin();
        final boolean debounce = then != null
                && (now.getTime() - then.getTime()) < LAST_LOGIN_DEBOUNCE_MS;

        admin.setLastLogin(now);
        admin.setName(player.getName());

        if (!debounce)
        {
            saveAdminAsync(admin);
        }
    }

    public boolean isAdminImpostor(Player player)
    {
        return getEntryByName(player.getName()) != null && !isAdmin(player);
    }

    public boolean isIdentityMatched(Player player)
    {
        if (Bukkit.getOnlineMode())
        {
            return true;
        }

        Admin admin = getAdmin(player);
        return admin == null ? false : admin.getName().equalsIgnoreCase(player.getName());
    }

    public boolean addAdmin(Admin admin)
    {
        if (!admin.isValid())
        {
            FLog.warning("Could not add admin: " + admin.getConfigKey() + " Admin is missing details!");
            return false;
        }

        final String key = admin.getConfigKey();

        // Store admin, update views
        allAdmins.put(key, admin);
        updateTables();

        // Save admin
        if (usingSql)
        {
            saveAdminToSql(admin);
        }
        else
        {
            saveToJson();
        }

        refreshWorldEditBypassForAdmin(admin);
        return true;
    }

    /**
     * Save a single admin to SQL database.
     */
    private void saveAdminToSql(Admin admin)
    {
        saveAdminAsync(admin);
    }

    public boolean removeAdmin(Admin admin)
    {
        // Remove admin, update views
        if (allAdmins.remove(admin.getConfigKey()) == null)
        {
            return false;
        }
        updateTables();

        // Remove from storage
        if (usingSql)
        {
            removeAdminFromSql(admin);
        }
        else
        {
            saveToJson();
        }

        refreshWorldEditBypassForAdmin(admin);
        return true;
    }

    private void refreshWorldEditBypassForAdmin(Admin admin)
    {
        if (plugin.web == null)
        {
            return;
        }
        try
        {
            org.bukkit.entity.Player online = null;
            final UUID uuid = admin.getUuid();
            if (uuid != null)
            {
                online = plugin.getServer().getPlayer(uuid);
            }
            if (online == null && admin.getName() != null)
            {
                online = plugin.getServer().getPlayerExact(admin.getName());
            }
            if (online != null)
            {
                plugin.web.refreshBypassNegation(online);
            }
        }
        catch (Throwable t)
        {
            FLog.warning("Failed to refresh WorldEdit bypass negation: " + t.getMessage());
        }
    }

    /**
     * Remove admin from SQL database.
     */
    private void removeAdminFromSql(Admin admin)
    {
        if (plugin.dm == null || !plugin.dm.isInitialized())
        {
            return;
        }

        UUID uuid = admin.getUuid();
        String name = admin.getName();

        synchronized (persistenceLock)
        {
            persistenceChain = persistenceChain
                    .onErrorResume(ignored -> Mono.empty())
                    .then(uuid != null
                            ? plugin.dm.getAdminRepository().deleteByUuid(uuid).then()
                            : Mono.<Void>fromRunnable(() ->
                            {
                                try
                                {
                                    plugin.dm.getAdminRepository().deleteByUsername(name);
                                }
                                catch (Exception ex)
                                {
                                    throw new RuntimeException(ex);
                                }
                            }).subscribeOn(Schedulers.boundedElastic()))
                    .onErrorResume(ex ->
                    {
                        FLog.warning("Failed to remove admin " + name + " from SQL: " + ex.getMessage());
                        return Mono.empty();
                    })
                    .then(Mono.fromRunnable(this::saveToJson))
                    .then()
                    .cache();
            persistenceChain.subscribe();
        }
    }

    /**
     * Refresh the IP lookup table for a single admin. Use this instead of
     * {@link #updateTables()} when only one entry's IP list has changed.
     */
    public void refreshIps(Admin admin)
    {
        if (admin == null)
        {
            return;
        }

        ipTable.values().removeIf(entry -> entry == admin);

        if (!admin.isActive())
        {
            return;
        }

        admin.getIps().forEach(ip -> ipTable.put(ip, admin));
    }

    public void updateTables()
    {
        activeAdmins.clear();
        nameTable.clear();
        ipTable.clear();
        uuidTable.clear();
        onlineAdminPlayers.clear();

        for (Admin admin : allAdmins.values())
        {
            // Always populate UUID table
            if (admin.getUuid() != null)
            {
                uuidTable.put(admin.getUuid(), admin);
            }
            
            if (!admin.isActive())
            {
                continue;
            }

            activeAdmins.add(admin);
            nameTable.put(admin.getName().toLowerCase(), admin);

            for (String ip : admin.getIps())
            {
                ipTable.put(ip, admin);
            }

        }

        // Re-populate online-admin cache from currently-online players.
        for (Player online : Bukkit.getOnlinePlayers())
        {
            if (isAdmin(online))
            {
                onlineAdminPlayers.add(online);
            }
        }

        plugin.wm.adminworld.wipeAccessCache();

    }
    
    public Map<String, Admin> getAllAdmins()
    {
        return allAdmins;
    }

    public Set<Admin> getActiveAdmins()
    {
        return activeAdmins;
    }

    /**
     * Get admin by UUID.
     */
    public Admin getAdminByUuid(UUID uuid)
    {
        return uuidTable.get(uuid);
    }

    public Set<String> getAdminNames()
    {
        return nameTable.keySet();
    }

    public Set<String> getAdminIps()
    {
        return ipTable.keySet();
    }

    public Set<Player> getOnlineAdmins()
    {
        return onlineAdminPlayers;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();
        if (isAdmin(player))
        {
            onlineAdminPlayers.add(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        onlineAdminPlayers.remove(event.getPlayer());
    }

    public void deactivateOldEntries(boolean verbose)
    {
        for (Admin admin : allAdmins.values())
        {
            if (!admin.isActive() || admin.getRank().isAtLeast(Rank.SENIOR_ADMIN))
            {
                continue;
            }

            final Date lastLogin = admin.getLastLogin();
            final long lastLoginHours = TimeUnit.HOURS.convert(new Date().getTime() - lastLogin.getTime(), TimeUnit.MILLISECONDS);

            if (lastLoginHours < ConfigEntry.ADMINLIST_CLEAN_THESHOLD_HOURS.getInteger())
            {
                continue;
            }

            if (verbose)
            {
                FUtil.adminAction("TotalFreedomMod", "Deactivating superadmin " + admin.getName() + ", inactive for " + lastLoginHours + " hours", true);
            }

            admin.setActive(false);
            saveAdminAsync(admin);
        }

        updateTables();
    }
}
