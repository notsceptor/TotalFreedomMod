package me.totalfreedom.totalfreedommod;

import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import me.totalfreedom.totalfreedommod.sql.adapter.SavedFlagRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.JsonUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class SavedFlags extends FreedomService
{

    public static final String DATA_FILENAME = "savedflags.json";
    public static final String LEGACY_YAML_FILENAME = "savedflags.yml";
    public static final String LEGACY_DATA_FILENAME = "savedflags.dat";

    private static final Type FLAGS_MAP_TYPE = new TypeToken<Map<String, Boolean>>() {}.getType();

    private boolean usingSql = false;

    public SavedFlags(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        usingSql = plugin.dm != null && plugin.dm.isInitialized();

        File dataFile = new File(plugin.getDataFolder(), DATA_FILENAME);
        File legacyFile = new File(plugin.getDataFolder(), LEGACY_DATA_FILENAME);

        if (legacyFile.exists() && !dataFile.exists())
        {
            migrateLegacyData(legacyFile, dataFile);
        }

        if (usingSql)
        {
            reconcileFromJsonIfNewer();
        }
    }

    @Override
    protected void onStop()
    {
    }

    @SuppressWarnings("unchecked")
    private void migrateLegacyData(File legacyFile, File dataFile)
    {
        FLog.info("Migrating saved flags from legacy .dat format...");
        try (FileInputStream fis = new FileInputStream(legacyFile);
             ObjectInputStream ois = new ObjectInputStream(fis))
        {
            HashMap<String, Boolean> legacyFlags = (HashMap<String, Boolean>) ois.readObject();

            if (usingSql)
            {
                try
                {
                    SavedFlagRepository repo = plugin.dm.getSavedFlagRepository();
                    for (Map.Entry<String, Boolean> entry : legacyFlags.entrySet())
                    {
                        repo.upsert(entry.getKey(), entry.getValue());
                    }
                }
                catch (SQLException ex)
                {
                    FLog.severe("Could not save migrated flags to SQL: " + ex.getMessage());
                }
            }
            saveToJson(legacyFlags);

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

    /**
     * Reads the pre-JSON {@code savedflags.yml} format. Retained only for the one-time
     * legacy-install migration path (not called during normal startup).
     */
    private Map<String, Boolean> loadLegacyYaml(File file)
    {
        Map<String, Boolean> flags = new HashMap<>();
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
            FLog.severe("Failed to load legacy saved flags: " + ex.getMessage());
            FLog.severe(ex);
        }

        return flags;
    }

    /**
     * If savedflags.json was written more recently than the database's last update, re-import it into SQL.
     */
    private void reconcileFromJsonIfNewer()
    {
        File dataFile = new File(plugin.getDataFolder(), DATA_FILENAME);
        if (!dataFile.exists())
        {
            return;
        }

        try
        {
            SavedFlagRepository repo = plugin.dm.getSavedFlagRepository();
            Long sqlUpdatedAt = repo.getMaxUpdatedAt();
            if (sqlUpdatedAt != null && dataFile.lastModified() <= sqlUpdatedAt)
            {
                return;
            }

            Map<String, Boolean> jsonFlags = readJsonFlags(dataFile);
            if (jsonFlags.isEmpty())
            {
                return;
            }

            FLog.info(DATA_FILENAME + " is newer than the database; re-importing " + jsonFlags.size() + " flag(s) from it.");
            for (Map.Entry<String, Boolean> entry : jsonFlags.entrySet())
            {
                repo.upsert(entry.getKey(), entry.getValue());
            }
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to reconcile " + DATA_FILENAME + " into the database: " + ex.getMessage());
        }
    }

    private Map<String, Boolean> readJsonFlags(File file)
    {
        Map<String, Boolean> flags = new HashMap<>();
        if (!file.exists())
        {
            return flags;
        }

        try (FileReader reader = new FileReader(file))
        {
            Map<String, Boolean> loaded = JsonUtil.GSON.fromJson(reader, FLAGS_MAP_TYPE);
            if (loaded != null)
            {
                flags.putAll(loaded);
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Failed to load saved flags: " + ex.getMessage());
            FLog.severe(ex);
        }

        return flags;
    }

    private void saveToJson(Map<String, Boolean> flags)
    {
        File file = new File(plugin.getDataFolder(), DATA_FILENAME);
        try (FileWriter writer = new FileWriter(file))
        {
            JsonUtil.GSON.toJson(flags, FLAGS_MAP_TYPE, writer);
        }
        catch (IOException ex)
        {
            FLog.severe("Failed to save saved flags: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    public Map<String, Boolean> getSavedFlags()
    {
        if (usingSql && plugin.dm != null && plugin.dm.isInitialized())
        {
            try
            {
                return plugin.dm.getSavedFlagRepository().loadAll();
            }
            catch (SQLException ex)
            {
                FLog.severe("Failed to load saved flags from SQL: " + ex.getMessage());
            }
        }

        return readJsonFlags(new File(PluginProvider.get().getDataFolder(), DATA_FILENAME));
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
        if (usingSql && plugin.dm != null && plugin.dm.isInitialized())
        {
            try
            {
                plugin.dm.getSavedFlagRepository().upsert(flag, value);
            }
            catch (SQLException ex)
            {
                FLog.severe("Could not save flag '" + flag + "' to SQL: " + ex.getMessage());
            }
        }

        Map<String, Boolean> flags = getSavedFlags();
        if (flags == null)
        {
            flags = new HashMap<>();
        }
        flags.put(flag, value);
        saveToJson(flags);
    }

}
