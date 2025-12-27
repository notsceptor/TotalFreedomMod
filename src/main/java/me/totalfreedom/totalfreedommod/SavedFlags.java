package me.totalfreedom.totalfreedommod;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class SavedFlags extends FreedomService
{

    public static final String DATA_FILENAME = "savedflags.yml";
    public static final String LEGACY_DATA_FILENAME = "savedflags.dat";

    public SavedFlags(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        File ymlFile = new File(plugin.getDataFolder(), DATA_FILENAME);
        File legacyFile = new File(plugin.getDataFolder(), LEGACY_DATA_FILENAME);

        if (legacyFile.exists() && !ymlFile.exists())
        {
            migrateLegacyData(legacyFile, ymlFile);
        }
    }

    @Override
    protected void onStop()
    {
    }

    @SuppressWarnings("unchecked")
    private void migrateLegacyData(File legacyFile, File ymlFile)
    {
        FLog.info("Migrating saved flags from legacy .dat format to .yml format...");
        try (FileInputStream fis = new FileInputStream(legacyFile);
             ObjectInputStream ois = new ObjectInputStream(fis))
        {
            HashMap<String, Boolean> legacyFlags = (HashMap<String, Boolean>) ois.readObject();

            YamlConfiguration config = new YamlConfiguration();
            ConfigurationSection flagsSection = config.createSection("flags");

            for (Map.Entry<String, Boolean> entry : legacyFlags.entrySet())
            {
                flagsSection.set(entry.getKey(), entry.getValue());
            }

            config.save(ymlFile);

            File oldFile = new File(legacyFile.getParent(), LEGACY_DATA_FILENAME + ".old");
            if (legacyFile.renameTo(oldFile))
            {
                FLog.info("Migration complete. Legacy file renamed to " + LEGACY_DATA_FILENAME + ".old");
            }
            else
            {
                FLog.warning("Migration complete but could not rename legacy file.");
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Failed to migrate legacy saved flags data: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    public Map<String, Boolean> getSavedFlags()
    {
        Map<String, Boolean> flags = new HashMap<>();

        File file = new File(TotalFreedomMod.plugin().getDataFolder(), DATA_FILENAME);
        if (!file.exists())
        {
            return flags;
        }

        try
        {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection flagsSection = config.getConfigurationSection("flags");

            if (flagsSection != null)
            {
                for (String key : flagsSection.getKeys(false))
                {
                    flags.put(key, flagsSection.getBoolean(key));
                }
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Failed to load saved flags: " + ex.getMessage());
            FLog.severe(ex);
        }

        return flags;
    }

    public boolean getSavedFlag(String flag) throws Exception
    {
        Boolean flagValue = null;

        Map<String, Boolean> flags = getSavedFlags();

        if (flags != null)
        {
            if (flags.containsKey(flag))
            {
                flagValue = flags.get(flag);
            }
        }

        if (flagValue != null)
        {
            return flagValue;
        }
        else
        {
            throw new Exception();
        }
    }

    public void setSavedFlag(String flag, boolean value)
    {
        Map<String, Boolean> flags = getSavedFlags();

        if (flags == null)
        {
            flags = new HashMap<>();
        }

        flags.put(flag, value);

        try
        {
            YamlConfiguration config = new YamlConfiguration();
            ConfigurationSection flagsSection = config.createSection("flags");

            for (Map.Entry<String, Boolean> entry : flags.entrySet())
            {
                flagsSection.set(entry.getKey(), entry.getValue());
            }

            config.save(new File(plugin.getDataFolder(), DATA_FILENAME));
        }
        catch (IOException ex)
        {
            FLog.severe("Failed to save saved flags: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

}
