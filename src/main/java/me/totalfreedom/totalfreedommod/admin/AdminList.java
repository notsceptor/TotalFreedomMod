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
import lombok.Getter;
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

    @Getter
    private final Map<String, Admin> allAdmins = Maps.newHashMap(); // Includes disabled admins
    // Only active admins below
    @Getter
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
        }

        if (resolved + offlineDerived == 0)
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
        saveAsync();
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
     * Persist admin records on a worker thread. Use this on the hot login path
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

        Admin snapshot = copyAdmin(admin);
        UUID uuid = snapshot.getUuid();

        if (uuid == null)
        {
            uuid = FUtil.usernameToUuid(snapshot.getName());
            if (uuid == null)
            {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + snapshot.getName().toLowerCase()).getBytes(StandardCharsets.UTF_8));
            }
            snapshot.setUuid(uuid);
            admin.setUuid(uuid);
        }

        UUID finalUuid = uuid;
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
        
        try
        {
            AdminRepository repo = plugin.dm.getAdminRepository();
            for (Admin admin : allAdmins.values())
            {
                UUID uuid = admin.getUuid();
                if (uuid == null)
                {
                    // Generate UUID if not present
                    uuid = FUtil.usernameToUuid(admin.getName());
                    if (uuid == null)
                    {
                        uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + admin.getName().toLowerCase()).getBytes());
                    }
                    admin.setUuid(uuid);
                }
                repo.save(uuid, admin).block();
            }
            FLog.debug("Saved " + allAdmins.size() + " admins to SQL database");
            saveToJson();
        }
        catch (Exception ex)
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
                    saveAsync();
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
                        saveAsync();
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
                saveAsync();
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
            saveAsync();
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
