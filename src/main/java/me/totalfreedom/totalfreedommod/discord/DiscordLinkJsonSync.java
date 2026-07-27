package me.totalfreedom.totalfreedommod.discord;

import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.sql.adapter.DiscordLinkRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.JsonUtil;

/**
 * JSON write-through + startup reconciliation for admin-uuid to Discord-user-id links.
 * There is no in-memory manager for this domain (DiscordCommands talks to the repository
 * directly), so this holds the snapshot file logic on its own.
 */
final class DiscordLinkJsonSync
{
    static final String DATA_FILENAME = "discord_links.json";

    private static final Type LINKS_MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private DiscordLinkJsonSync()
    {
    }

    /**
     * Rewrites discord_links.json from the database's current state. Call after any
     * successful link/unlink write.
     */
    static void writeSnapshot(TotalFreedomMod plugin, DiscordLinkRepository repo)
    {
        try
        {
            Map<String, String> links = repo.loadAll();
            File file = new File(plugin.getDataFolder(), DATA_FILENAME);
            try (FileWriter writer = new FileWriter(file))
            {
                JsonUtil.GSON.toJson(links, LINKS_MAP_TYPE, writer);
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Failed to save " + DATA_FILENAME + ": " + ex.getMessage());
        }
    }

    /**
     * If discord_links.json was written more recently than the database's last update, re-import it into SQL.
     */
    static void reconcileFromJsonIfNewer(TotalFreedomMod plugin, DiscordLinkRepository repo)
    {
        File file = new File(plugin.getDataFolder(), DATA_FILENAME);
        if (!file.exists())
        {
            return;
        }

        try
        {
            Long sqlUpdatedAt = repo.getMaxUpdatedAt();
            if (sqlUpdatedAt != null && file.lastModified() <= sqlUpdatedAt)
            {
                return;
            }

            Map<String, String> jsonLinks;
            try (FileReader reader = new FileReader(file))
            {
                Map<String, String> loaded = JsonUtil.GSON.fromJson(reader, LINKS_MAP_TYPE);
                jsonLinks = loaded != null ? loaded : Map.of();
            }

            if (jsonLinks.isEmpty())
            {
                return;
            }

            FLog.info(DATA_FILENAME + " is newer than the database; re-importing " + jsonLinks.size() + " discord link(s) from it.");
            for (Map.Entry<String, String> entry : jsonLinks.entrySet())
            {
                UUID adminUuid = UUID.fromString(entry.getKey());
                repo.deleteByAdminUuid(adminUuid);
                repo.deleteByDiscordUserId(entry.getValue());
                repo.insert(adminUuid, entry.getValue());
            }
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to reconcile " + DATA_FILENAME + " into the database: " + ex.getMessage());
        }
    }
}
