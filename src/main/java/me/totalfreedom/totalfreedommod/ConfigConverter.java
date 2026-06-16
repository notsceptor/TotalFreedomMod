package me.totalfreedom.totalfreedommod;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.admin.AdminList;
import me.totalfreedom.totalfreedommod.banning.PermbanList;
import me.totalfreedom.totalfreedommod.config.MainConfig;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.rank.RankManager;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.framework.PluginComponent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigConverter extends PluginComponent<TotalFreedomMod>
{

    public static final int CURRENT_CONFIG_VERSION = 1;

    public ConfigConverter(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    public void convert()
    {
        File data = plugin.getDataFolder();
        data.mkdirs();
        File versionFile = new File(data, "version.yml");

        boolean convert = false;
        if (!versionFile.exists() && data.listFiles().length > 0)
        {
            convert = true;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(versionFile);
        if (!versionFile.exists())
        {
            try
            {
                versionFile.getParentFile().mkdirs();
                versionFile.createNewFile();
            }
            catch (IOException ex)
            {
                // Ignore
            }
        }

        if (config.getInt("version", -1) < CURRENT_CONFIG_VERSION)
        {
            convert = true;
        }

        if (!convert)
        {
            return;
        }

        FLog.warning("Converting old configs to new format...");

        File backup = new File(data, "backup_old_format");
        backup.mkdirs();

        for (File file : data.listFiles())
        {
            if (file.equals(backup) || file.equals(versionFile))
            {
                continue;
            }

            try
            {
                Files.move(file, new File(backup, file.getName()));
            }
            catch (IOException ex)
            {
                FLog.severe("Could not backup file: " + file.getName());
                FLog.severe(ex);
            }
        }

        convertSuperadmins(new File(backup, "superadmin.yml"));
        convertPermbans(new File(backup, "permban.yml"));

        FLog.info("Conversion complete!");
    }

    /**
     * Removes some of the unused console variant keys/ranks from ranks.yml.
     */
    public void convertRanksYaml()
    {
        File ranksFile = new File(plugin.getDataFolder(), RankManager.RANKS_FILENAME);
        if (!ranksFile.exists())
        {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(ranksFile);
        boolean changed = false;

        for (String key : yaml.getKeys(false))
        {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null)
            {
                continue;
            }
            if (section.contains("console_variant"))
            {
                section.set("console_variant", null);
                changed = true;
            }
            if (section.contains("player_variant"))
            {
                section.set("player_variant", null);
                changed = true;
            }
        }

        if (yaml.contains("telnet_console"))
        {
            yaml.set("telnet_console", null);
            changed = true;
        }
        if (yaml.contains("senior_console"))
        {
            yaml.set("senior_console", null);
            changed = true;
        }

        if (!changed)
        {
            return;
        }

        try
        {
            yaml.save(ranksFile);
            FLog.info("Stripped deprecated console variant fields from ranks.yml.");
        }
        catch (IOException ex)
        {
            FLog.severe("Could not save migrated ranks.yml");
            FLog.severe(ex);
        }
    }

    /**
     * Remap admins still assigned to deprecated {@code *_CONSOLE} legacy ranks.
     */
    public void convertAdminConsoleRanks()
    {
        if (plugin.al == null)
        {
            return;
        }

        int migrated = 0;
        for (Admin admin : plugin.al.getAllAdmins().values())
        {
            Rank rank = admin.getRank();
            Rank newRank = null;
            if (rank == Rank.TELNET_CONSOLE)
            {
                newRank = Rank.TELNET_ADMIN;
            }
            else if (rank == Rank.SENIOR_CONSOLE)
            {
                newRank = Rank.SENIOR_ADMIN;
            }
            if (newRank != null)
            {
                admin.setRank(newRank);
                migrated++;
                FLog.info("Remapped admin '" + admin.getName() + "' from " + rank.name() + " to " + newRank.name());
            }
        }

        if (migrated > 0)
        {
            plugin.al.save();
            FLog.info("Remapped " + migrated + " admin(s) from deprecated console ranks.");
        }
    }

    private void convertSuperadmins(File oldFile)
    {
        if (!oldFile.exists() || !oldFile.isFile())
        {
            FLog.warning("No old superadmin list found!");
            return;
        }

        YamlConfiguration oldYaml = YamlConfiguration.loadConfiguration(oldFile);

        ConfigurationSection admins = oldYaml.getConfigurationSection("admins");
        if (admins == null)
        {
            FLog.warning("No admin section in superadmin list!");
            return;
        }

        List<Admin> conversions = Lists.newArrayList();
        for (String uuid : admins.getKeys(false))
        {
            ConfigurationSection asec = admins.getConfigurationSection(uuid);
            if (asec == null)
            {
                FLog.warning("Invalid superadmin format for admin: " + uuid);
                continue;
            }

            String username = asec.getString("last_login_name");
            Rank rank;
            if (asec.getBoolean("is_senior_admin"))
            {
                rank = Rank.SENIOR_ADMIN;
            }
            else if (asec.getBoolean("is_telnet_admin"))
            {
                rank = Rank.TELNET_ADMIN;
            }
            else
            {
                rank = Rank.SUPER_ADMIN;
            }
            List<String> ips = asec.getStringList("ips");
            String loginMessage = asec.getString("custom_login_message");
            boolean active = asec.getBoolean("is_activated");

            Admin admin = new Admin(username);
            admin.setName(username);
            admin.setRank(rank);
            admin.addIps(ips);
            admin.setLoginMessage(loginMessage);
            admin.setActive(active);
            admin.setLastLogin(new Date());
            conversions.add(admin);
        }

        File newYamlFile = new File(plugin.getDataFolder(), AdminList.CONFIG_FILENAME);
        YamlConfiguration newYaml = YamlConfiguration.loadConfiguration(newYamlFile);
        for (Admin admin : conversions)
        {
            admin.saveTo(newYaml.createSection(admin.getName().toLowerCase()));
        }
        try
        {
            newYaml.save(newYamlFile);
        }
        catch (IOException ex)
        {
            FLog.severe("Could not save converted admin list");
        }

        FLog.info("Converted " + conversions.size() + " admins");
    }

    private void convertPermbans(File oldFile)
    {
        if (!oldFile.exists())
        {
            FLog.warning("No old permban list found!");
            return;
        }

        try
        {
            Files.copy(oldFile, new File(plugin.getDataFolder(), PermbanList.CONFIG_FILENAME));
            FLog.info("Converted permban list");
        }
        catch (IOException ex)
        {
            FLog.warning("Could not copy old permban list!");
        }

    }

}
