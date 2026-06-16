package me.totalfreedom.totalfreedommod.rank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;

/**
 * Loads and exposes the {@code host_senders:} whitelist that binds shared-secret /
 * no-identity senders to a specific rank.
 * 
 * Reload by calling {@link #load()}
 */
public class ConsoleSenderRegistry
{

    private final TotalFreedomMod plugin;
    private final Map<String, String> senderToRankId = new HashMap<>();
    private final Map<String, Rank> senderToLegacyRank = new HashMap<>();

    public ConsoleSenderRegistry(TotalFreedomMod plugin)
    {
        this.plugin = plugin;
    }

    public void load()
    {
        senderToRankId.clear();
        senderToLegacyRank.clear();

        List<?> raw = ConfigEntry.HOST_SENDERS.getList();
        if (raw == null)
        {
            FLog.warning("Host sender whitelist (config 'host_senders:') is missing. All shared-secret senders will be denied.");
            return;
        }

        for (Object obj : raw)
        {
            if (!(obj instanceof String))
            {
                FLog.warning("Console whitelist entry is not a string, skipping: " + obj);
                continue;
            }
            String entry = ((String) obj).trim();
            int colon = entry.indexOf(':');
            if (colon <= 0 || colon == entry.length() - 1)
            {
                FLog.warning("Console whitelist entry is malformed (expected '<rank_id>:<sender_name>'), skipping: " + entry);
                continue;
            }

            String rankId = entry.substring(0, colon).trim();
            String senderName = entry.substring(colon + 1).trim().toLowerCase();

            Rank legacyRank = parseLegacyRank(rankId);
            if (legacyRank == null && !isKnownCustomRank(rankId))
            {
                FLog.warning("Console whitelist entry references unknown rank '" + rankId + "', skipping: " + entry);
                continue;
            }

            String existingId = senderToRankId.put(senderName, rankId.toLowerCase());
            if (existingId != null && !existingId.equalsIgnoreCase(rankId))
            {
                FLog.warning("Console whitelist binds sender '" + senderName + "' to multiple ranks; using " + rankId);
            }
            if (legacyRank != null)
            {
                senderToLegacyRank.put(senderName, legacyRank);
            }
        }

        FLog.info("Loaded " + senderToRankId.size() + " console whitelist binding(s).");
    }

    public String getRankIdForSender(String senderName)
    {
        if (senderName == null)
        {
            return null;
        }
        return senderToRankId.get(senderName.toLowerCase());
    }

    public Rank getRankForSender(String senderName)
    {
        if (senderName == null)
        {
            return null;
        }
        return senderToLegacyRank.get(senderName.toLowerCase());
    }

    public boolean isWhitelisted(String senderName)
    {
        return getRankIdForSender(senderName) != null;
    }

    private boolean isKnownCustomRank(String rankId)
    {
        return plugin.rm != null && plugin.rm.getCustomRank(rankId.toLowerCase()) != null;
    }

    private static Rank parseLegacyRank(String id)
    {
        if (id == null || id.isEmpty())
        {
            return null;
        }
        String key = id.toUpperCase();
        try
        {
            Rank rank = Rank.valueOf(key);
            switch (rank)
            {
                case TELNET_CONSOLE:
                    return Rank.TELNET_ADMIN;
                case SENIOR_CONSOLE:
                    return Rank.SENIOR_ADMIN;
                default:
                    return rank;
            }
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }
}
