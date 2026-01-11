package me.totalfreedom.totalfreedommod.admin;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import me.totalfreedom.totalfreedommod.sql.adapter.AdminRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

public class AdminList extends FreedomService
{

    public static final String CONFIG_FILENAME = "admins.yml";

    @Getter
    private final Map<String, Admin> allAdmins = Maps.newHashMap(); // Includes disabled admins
    // Only active admins below
    @Getter
    private final Set<Admin> activeAdmins = Sets.newHashSet();
    
    // UUID-based lookup table
    private final Map<UUID, Admin> uuidTable = Maps.newHashMap();
    
    // Manual getters - Lombok @Getter not processing reliably
    public Map<String, Admin> getAllAdmins()
    {
        return allAdmins;
    }
    
    public Set<Admin> getActiveAdmins()
    {
        return activeAdmins;
    }
    private final Map<String, Admin> nameTable = Maps.newHashMap();
    private final Map<String, Admin> ipTable = Maps.newHashMap();
    //
    private final File configFile;
    private YamlConfiguration config;
    
    // Flag to track if SQL is available
    private boolean usingSql = false;

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

    public void save()
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
        
        try
        {
            AdminRepository repo = plugin.dm.getAdminRepository();
            for (Admin admin : allAdmins.values())
            {
                UUID uuid = admin.getUuid();
                if (uuid == null)
                {
                    // Generate UUID if not present
                    uuid = FUtil.nameToUUID(admin.getName());
                    if (uuid == null)
                    {
                        uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + admin.getName().toLowerCase()).getBytes());
                    }
                    admin.setUuid(uuid);
                }
                repo.save(uuid, admin).join();
            }
            FLog.debug("Saved " + allAdmins.size() + " admins to SQL database");
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to save admins to SQL: " + ex.getMessage());
            // Don't fall back to YAML here - we don't want to create conflicting data
        }
    }
    
    /**
     * Save all admins to YAML file (fallback).
     */
    private void saveToYaml()
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

        try
        {
            config.save(configFile);
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
        if (sender instanceof Player)
        {
            return getAdmin((Player) sender);
        }

        return getEntryByName(sender.getName());
    }

    public Admin getAdmin(Player player)
    {
        // Find admin
        String ip = player.getAddress().getAddress().getHostAddress();
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
                    save();
                    updateTables();
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
            admin.setName(player.getName());
            save();
            updateTables();
        }

        return null;
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

        admin.setLastLogin(new Date());
        admin.setName(player.getName());
        save();
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

        return true;
    }
    
    /**
     * Save a single admin to SQL database.
     */
    private void saveAdminToSql(Admin admin)
    {
        if (plugin.dm == null || !plugin.dm.isInitialized())
        {
            return;
        }
        
        try
        {
            UUID uuid = admin.getUuid();
            if (uuid == null)
            {
                uuid = FUtil.nameToUUID(admin.getName());
                if (uuid == null)
                {
                    uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + admin.getName().toLowerCase()).getBytes());
                }
                admin.setUuid(uuid);
            }
            plugin.dm.getAdminRepository().save(uuid, admin).join();
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to save admin to SQL: " + ex.getMessage());
        }
    }

    public boolean removeAdmin(Admin admin)
    {
        if (admin.getRank().isAtLeast(Rank.TELNET_ADMIN))
        {
            if (plugin.btb != null)
            {
                plugin.btb.killTelnetSessions(admin.getName());
            }
        }

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

        return true;
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
        
        try
        {
            if (admin.getUuid() != null)
            {
                plugin.dm.getAdminRepository().deleteByUuid(admin.getUuid()).join();
            }
            else
            {
                plugin.dm.getAdminRepository().deleteByUsername(admin.getName());
            }
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to remove admin from SQL: " + ex.getMessage());
        }
    }

    public void updateTables()
    {
        activeAdmins.clear();
        nameTable.clear();
        ipTable.clear();
        uuidTable.clear();

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
        }

        save();
        updateTables();
    }
}
