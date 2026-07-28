package me.totalfreedom.totalfreedommod.rank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /**
     * Console-class channels that reach the server through the host itself rather than through a
     * user identity. Console access is the privilege, so these sit at {@link Rank#SENIOR_ADMIN}
     * and {@code host_senders:} may raise them but never lower them. A binding below the floor is
     * a misconfiguration that silently locks the panel out of senior-gated commands, which is
     * precisely how these channels were broken before.
     * <p>
     * SSH and Discord are deliberately absent: they carry a real identity and are resolved per
     * user by {@link RankManager#getEffectiveRank}, falling back to their {@code host_senders:}
     * binding only when the session proved no identity.
     */
    private static final Set<String> HOST_CHANNELS = Set.of("rcon", "remotebukkit", "console");

    private static final Rank HOST_CHANNEL_FLOOR = Rank.SENIOR_ADMIN;

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
            FLog.warning("Host sender whitelist (config 'host_senders:') is missing. Identity-less senders will be denied, "
                    + "except the host channels, which hold their floor.");
            applyHostChannelFloor();
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

        applyHostChannelFloor();

        FLog.info("Loaded " + senderToRankId.size() + " console whitelist binding(s).");
    }

    /**
     * Raises every {@link #HOST_CHANNELS host channel} to {@link #HOST_CHANNEL_FLOOR}, whether it
     * was bound too low or left out of {@code host_senders:} entirely. A binding above the floor is
     * left alone, so operators can still hand a host channel a higher custom rank.
     */
    private void applyHostChannelFloor()
    {
        HOST_CHANNELS.forEach(channel ->
        {
            Rank bound = senderToLegacyRank.get(channel);
            if (bound != null && bound.isAtLeast(HOST_CHANNEL_FLOOR))
            {
                return;
            }

            // A custom rank has no legacy entry; only override it when it genuinely sits lower,
            // so 'executive:rcon' and friends survive.
            String boundId = senderToRankId.get(channel);
            if (bound == null && boundId != null && outranksFloor(boundId))
            {
                return;
            }

            if (boundId != null)
            {
                FLog.warning("Console whitelist binds host channel '" + channel + "' to '" + boundId
                        + "', below the " + HOST_CHANNEL_FLOOR.name().toLowerCase() + " floor for host channels; raising it.");
            }

            senderToRankId.put(channel, HOST_CHANNEL_FLOOR.name().toLowerCase());
            senderToLegacyRank.put(channel, HOST_CHANNEL_FLOOR);
        });
    }

    /**
     * Whether the custom rank {@code rankId} sits at or above the host-channel floor, compared on
     * the registry's own level scale so operator-defined numbering is honoured.
     */
    private boolean outranksFloor(String rankId)
    {
        if (plugin.rm == null)
        {
            return false;
        }

        CustomRank bound = plugin.rm.getCustomRank(rankId);
        CustomRank floor = plugin.rm.getCustomRankForLegacy(HOST_CHANNEL_FLOOR);

        return bound != null && floor != null && bound.isAtLeast(floor);
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
