package me.totalfreedom.totalfreedommod.admin;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;

public class AdminList extends FreedomService
{

    public static final String CONFIG_FILENAME = "admins.yml";

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
    private YamlConfiguration config;
    
    // Flag to track if SQL is available
    private boolean usingSql = false;
    private final Object persistenceLock = new Object();
    private final Object fileLock = new Object();
    private CompletableFuture<Void> persistenceChain = CompletableFuture.completedFuture(null);

    public AdminList(TotalFreedomMod plugin)
    {
        super(plugin);

        this.configFile = new File(plugin.getDataFolder(), CONFIG_FILENAME);
        this.config = YamlConfiguration.loadConfiguration(configFile);
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
            loadFromYaml();
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
            List<Admin> admins = repo.findAll().join();
            
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
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to load admins from SQL, falling back to YAML: " + ex.getMessage());
            loadFromYaml();
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
     * Load admins from YAML file (fallback).
     */
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
                FLog.severe("Could not create " + CONFIG_FILENAME);
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        allAdmins.clear();
        for (String key : config.getKeys(false))
        {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null)
            {
                FLog.warning("Invalid admin list format: " + key);
                continue;
            }

            Admin admin = new Admin(key);
            admin.loadFrom(section);

            if (!admin.isValid())
            {
                FLog.warning("Could not load admin: " + key + ". Missing details!");
                continue;
            }

            allAdmins.put(key, admin);
        }

        usingSql = false;
        updateTables();
        FLog.info("Loaded " + allAdmins.size() + " admins from YAML (" + nameTable.size() + " active, " + ipTable.size() + " IPs)");
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
            saveToYaml();
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
            writeYaml(data);
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> writeYaml(data));
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
                    .handle((ignored, throwable) -> null)
                    // Async: resolveUuid may hit Mojang. A plain thenCompose would
                    // run it on this thread whenever the chain is already complete.
                    .thenComposeAsync(ignored -> resolveUuid(admin, snapshot))
                    .thenCompose(uuid -> plugin.dm.getAdminRepository().save(uuid, snapshot).thenAccept(id ->
                    {
                    }))
                    .exceptionally(ex ->
                    {
                        FLog.warning("Failed to save admin " + snapshot.getName() + " to SQL: " + ex.getMessage());
                        return null;
                    });
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
            saveToYaml();
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
                repo.save(uuid, admin).join();
                saved++;
            }
            catch (Exception ex)
            {
                failed++;
                FLog.warning("Failed to save admin " + admin.getName() + " to SQL: " + ex.getMessage());
            }
        }

        // Don't fall back to YAML on failure - we don't want conflicting data.
        if (failed > 0)
        {
            FLog.warning("Saved " + saved + " admins to SQL database, " + failed + " failed");
        }
        else
        {
            FLog.debug("Saved " + saved + " admins to SQL database");
        }
    }
    
    /**
     * Render the whole admin list to YAML text.
     * <p>
     * Reads {@code allAdmins} and mutates {@code config}, so it must run on the
     * thread that owns them - the main thread. Callers wanting an off-thread
     * write should call this first and hand the result to {@link #writeYaml}.
     */
    private String serialiseAdmins()
    {
        // Clear the config
        for (String key : config.getKeys(false))
        {
            config.set(key, null);
        }

        for (Admin admin : allAdmins.values())
        {
            ConfigurationSection section = config.createSection(admin.getConfigKey());
            admin.saveTo(section);
        }

        return config.saveToString();
    }

    /**
     * Write pre-rendered YAML to disk. Touches no shared state beyond the file,
     * so it is safe from any thread; the lock only serialises concurrent writers
     * so two saves cannot interleave into a half-written file.
     */
    private void writeYaml(String data)
    {
        synchronized (fileLock)
        {
            try
            {
                configFile.getParentFile().mkdirs();
                Files.writeString(configFile.toPath(), data, StandardCharsets.UTF_8);
            }
            catch (IOException ex)
            {
                FLog.severe("Could not save " + CONFIG_FILENAME + ": " + ex.getMessage());
            }
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
            admin.saveTo(config.createSection(key));
            try
            {
                config.save(configFile);
            }
            catch (IOException ex)
            {
                FLog.severe("Could not save " + CONFIG_FILENAME);
            }
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
            config.set(admin.getConfigKey(), null);
            try
            {
                config.save(configFile);
            }
            catch (IOException ex)
            {
                FLog.severe("Could not save " + CONFIG_FILENAME);
            }
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
                    .handle((ignored, throwable) -> null)
                    .thenCompose(ignored ->
                    {
                        if (uuid != null)
                        {
                            return plugin.dm.getAdminRepository().deleteByUuid(uuid).thenAccept(deleted ->
                            {
                            });
                        }

                        return CompletableFuture.runAsync(() ->
                        {
                            try
                            {
                                plugin.dm.getAdminRepository().deleteByUsername(name);
                            }
                            catch (Exception ex)
                            {
                                throw new RuntimeException(ex);
                            }
                        });
                    })
                    .exceptionally(ex ->
                    {
                        FLog.warning("Failed to remove admin " + name + " from SQL: " + ex.getMessage());
                        return null;
                    });
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
