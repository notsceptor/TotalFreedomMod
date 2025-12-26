package me.totalfreedom.totalfreedommod;

import java.io.File;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import java.io.File;
import java.io.IOException;
import me.totalfreedom.totalfreedommod.framework.PluginComponent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.FileUtil;

public class BackupManager extends PluginComponent<TotalFreedomMod>
{

    public BackupManager(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    public void createBackups(String file)
    {
        createBackups(file, false);
    }

    public void createBackups(String file, boolean onlyWeekly)
    {
        final String save = file.split("\\.")[0];
        final File configFile = new File(plugin.getDataFolder(), "backup/backup.yml");
        if (!configFile.exists())
        {
            try
            {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }
            catch (IOException ex)
            {
                FLog.severe("Could not create backup.yml");
            }
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Weekly
        if (!config.isInt(save + ".weekly"))
        {
            performBackup(file, "weekly");
            config.set(save + ".weekly", FUtil.getUnixTime());
        }
        else
        {
            int lastBackupWeekly = config.getInt(save + ".weekly");

            if (lastBackupWeekly + 3600 * 24 * 7 < FUtil.getUnixTime())
            {
                performBackup(file, "weekly");
                config.set(save + ".weekly", FUtil.getUnixTime());
            }
        }

        if (onlyWeekly)
        {
            try
        {
            config.save(configFile);
        }
        catch (IOException ex)
        {
            FLog.severe("Could not save backup.yml");
        }
            return;
        }

        // Daily
        if (!config.isInt(save + ".daily"))
        {
            performBackup(file, "daily");
            config.set(save + ".daily", FUtil.getUnixTime());
        }
        else
        {
            int lastBackupDaily = config.getInt(save + ".daily");

            if (lastBackupDaily + 3600 * 24 < FUtil.getUnixTime())
            {
                performBackup(file, "daily");
                config.set(save + ".daily", FUtil.getUnixTime());
            }
        }

        try
        {
            config.save(configFile);
        }
        catch (IOException ex)
        {
            FLog.severe("Could not save backup.yml");
        }
    }

    private void performBackup(String file, String type)
    {
        FLog.info("Backing up " + file + " to " + file + "." + type + ".bak");
        final File backupFolder = new File(plugin.getDataFolder(), "backup");

        if (!backupFolder.exists())
        {
            backupFolder.mkdirs();
        }

        final File oldYaml = new File(plugin.getDataFolder(), file);
        final File newYaml = new File(backupFolder, file + "." + type + ".bak");
        FileUtil.copy(oldYaml, newYaml);
    }

}
