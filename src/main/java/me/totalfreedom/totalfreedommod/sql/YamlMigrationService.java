package me.totalfreedom.totalfreedommod.sql;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.banning.PermBan;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.sql.adapter.AdminRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.BanRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.PermbanRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for migrating data from YAML files to SQL database.
 * Handles one-time migration of admins.yml, bans.yml, and permbans.yml.
 * 
 * Migration process:
 * 1. Check if migration has already been completed (migration flag in database)
 * 2. Read data from YAML files
 * 3. Insert data into SQL database
 * 4. Mark migration as complete
 * 5. Optionally rename/backup old YAML files
 */
public class YamlMigrationService
{
    private final TotalFreedomMod plugin;
    private final FreedomDatabase databaseManager;

    private static final String ADMINS_FILE = "admins.yml";
    private static final String BANS_FILE = "bans.yml";
    private static final String PERMBANS_FILE = "permbans.yml";

    public YamlMigrationService(TotalFreedomMod plugin, FreedomDatabase databaseManager)
    {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Run all migrations if they haven't been completed yet.
     * @return CompletableFuture that completes when all migrations are done
     */
    public CompletableFuture<Void> runMigrations()
    {
        return CompletableFuture.runAsync(() -> {
            try
            {
                FLog.info("Checking for YAML data migrations...");

                if (!databaseManager.isInitialized())
                {
                    FLog.warning("Database not initialized, skipping YAML migrations");
                    return;
                }

                migrateAdmins();
                migrateBans();
                migratePermbans();

                FLog.info("YAML data migration check complete");
            }
            catch (Exception ex)
            {
                FLog.severe("Error during YAML migrations: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    /**
     * Migrate admins from admins.yml to database.
     */
    private void migrateAdmins()
    {
        File adminsFile = new File(plugin.getDataFolder(), ADMINS_FILE);
        if (!adminsFile.exists())
        {
            FLog.info("No admins.yml found, skipping admin migration");
            return;
        }

        AdminRepository repo = databaseManager.getAdminRepository();

        // Check if we already have admins in the database
        try
        {
            List<Admin> existingAdmins = repo.findAll().join();
            if (!existingAdmins.isEmpty())
            {
                FLog.info("Database already contains " + existingAdmins.size() + " admins, skipping YAML migration");
                return;
            }
        }
        catch (Exception ex)
        {
            FLog.warning("Could not check existing admins: " + ex.getMessage());
        }

        FLog.info("Migrating admins from " + ADMINS_FILE + "...");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(adminsFile);
        AtomicInteger migrated = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        for (String key : config.getKeys(false))
        {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null)
            {
                FLog.warning("Invalid admin entry: " + key);
                failed.incrementAndGet();
                continue;
            }

            try
            {
                Admin admin = new Admin(key);
                admin.loadFrom(section);

                if (!admin.isValid())
                {
                    FLog.warning("Invalid admin data for: " + key);
                    failed.incrementAndGet();
                    continue;
                }

                // Generate UUID if not present (legacy data)
                UUID uuid = generateUuidForAdmin(admin);

                repo.save(uuid, admin).join();
                migrated.incrementAndGet();
            }
            catch (Exception ex)
            {
                FLog.warning("Failed to migrate admin " + key + ": " + ex.getMessage());
                failed.incrementAndGet();
            }
        }

        FLog.info("Admin migration complete: " + migrated.get() + " migrated, " + failed.get() + " failed");

        // Backup the old file
        backupFile(adminsFile);
    }

    /**
     * Migrate bans from bans.yml to database.
     */
    private void migrateBans()
    {
        File bansFile = new File(plugin.getDataFolder(), BANS_FILE);
        if (!bansFile.exists())
        {
            FLog.info("No bans.yml found, skipping ban migration");
            return;
        }

        BanRepository repo = databaseManager.getBanRepository();

        // Check if we already have bans in the database
        try
        {
            List<Ban> existingBans = repo.findAll().join();
            if (!existingBans.isEmpty())
            {
                FLog.info("Database already contains " + existingBans.size() + " bans, skipping YAML migration");
                return;
            }
        }
        catch (Exception ex)
        {
            FLog.warning("Could not check existing bans: " + ex.getMessage());
        }

        FLog.info("Migrating bans from " + BANS_FILE + "...");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(bansFile);
        AtomicInteger migrated = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        for (String key : config.getKeys(false))
        {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null)
            {
                FLog.warning("Invalid ban entry: " + key);
                failed.incrementAndGet();
                continue;
            }

            try
            {
                Ban ban = new Ban();
                ban.loadFrom(section);

                if (!ban.isValid())
                {
                    FLog.warning("Invalid ban data for: " + key);
                    failed.incrementAndGet();
                    continue;
                }

                repo.save(ban).join();
                migrated.incrementAndGet();
            }
            catch (Exception ex)
            {
                FLog.warning("Failed to migrate ban " + key + ": " + ex.getMessage());
                failed.incrementAndGet();
            }
        }

        FLog.info("Ban migration complete: " + migrated.get() + " migrated, " + failed.get() + " failed");

        // Backup the old file
        backupFile(bansFile);
    }

    /**
     * Migrate permbans from permbans.yml to database.
     */
    private void migratePermbans()
    {
        File permbansFile = new File(plugin.getDataFolder(), PERMBANS_FILE);
        if (!permbansFile.exists())
        {
            FLog.info("No permbans.yml found, skipping permban migration");
            return;
        }

        PermbanRepository repo = databaseManager.getPermbanRepository();

        // Check if we already have permbans in the database
        try
        {
            List<PermBan> existingPermbans = repo.findAll().join();
            if (!existingPermbans.isEmpty())
            {
                FLog.info("Database already contains " + existingPermbans.size() + " permbans, skipping YAML migration");
                return;
            }
        }
        catch (Exception ex)
        {
            FLog.warning("Could not check existing permbans: " + ex.getMessage());
        }

        FLog.info("Migrating permbans from " + PERMBANS_FILE + "...");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(permbansFile);
        AtomicInteger migrated = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        for (String name : config.getKeys(false))
        {
            try
            {
                List<String> ips = config.getStringList(name);

                PermBan permban = new PermBan();
                permban.setUsername(name.toLowerCase().trim());
                permban.setIps(ips);
                permban.setReason("Migrated from permbans.yml");

                // Generate UUID for name
                permban.setUuid(FUtil.usernameToUuid(name));

                repo.save(permban).join();
                migrated.incrementAndGet();
            }
            catch (Exception ex)
            {
                FLog.warning("Failed to migrate permban " + name + ": " + ex.getMessage());
                failed.incrementAndGet();
            }
        }

        FLog.info("Permban migration complete: " + migrated.get() + " migrated, " + failed.get() + " failed");

        // Backup the old file
        backupFile(permbansFile);
    }

    /**
     * Generate or retrieve UUID for an admin.
     */
    private UUID generateUuidForAdmin(Admin admin)
    {
        // Try to get UUID by name from Mojang/cache
        UUID uuid = FUtil.usernameToUuid(admin.getName());
        if (uuid != null)
        {
            return uuid;
        }

        // Fallback: generate offline-mode UUID
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + admin.getName().toLowerCase()).getBytes());
    }

    /**
     * Backup a file by renaming it with .migrated extension.
     */
    private void backupFile(File file)
    {
        if (!file.exists())
        {
            return;
        }

        File backupFile = new File(file.getParentFile(), file.getName() + ".migrated");
        
        // If backup already exists, add timestamp
        if (backupFile.exists())
        {
            backupFile = new File(file.getParentFile(), 
                file.getName() + ".migrated." + System.currentTimeMillis());
        }

        if (file.renameTo(backupFile))
        {
            FLog.info("Backed up " + file.getName() + " to " + backupFile.getName());
        }
        else
        {
            FLog.warning("Could not backup " + file.getName() + " - please manually remove or rename it");
        }
    }

    /**
     * Force re-migration of all YAML data.
     * WARNING: This will clear existing database data and re-import from YAML.
     */
    public CompletableFuture<Void> forceMigration()
    {
        return CompletableFuture.runAsync(() -> {
            FLog.warning("Force migration requested - this will overwrite database data!");

            // Clear existing data
            try
            {
                databaseManager.getAdminRepository().deleteAll().join();
                databaseManager.getBanRepository().deleteAll().join();
                databaseManager.getPermbanRepository().deleteAll().join();
            }
            catch (Exception ex)
            {
                FLog.severe("Failed to clear existing data: " + ex.getMessage());
                return;
            }

            // Restore backup files if they exist
            restoreBackupFiles();

            // Run migrations
            migrateAdmins();
            migrateBans();
            migratePermbans();
        });
    }

    /**
     * Restore .migrated backup files to their original names.
     */
    private void restoreBackupFiles()
    {
        File dataFolder = plugin.getDataFolder();

        restoreBackupFile(new File(dataFolder, ADMINS_FILE + ".migrated"), new File(dataFolder, ADMINS_FILE));
        restoreBackupFile(new File(dataFolder, BANS_FILE + ".migrated"), new File(dataFolder, BANS_FILE));
        restoreBackupFile(new File(dataFolder, PERMBANS_FILE + ".migrated"), new File(dataFolder, PERMBANS_FILE));
    }

    private void restoreBackupFile(File backup, File original)
    {
        if (backup.exists() && !original.exists())
        {
            if (backup.renameTo(original))
            {
                FLog.info("Restored " + original.getName() + " from backup");
            }
        }
    }
}
