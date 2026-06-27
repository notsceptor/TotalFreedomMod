package me.totalfreedom.totalfreedommod.blocking.gamerule;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSetGameRule;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Targets inbound SET_GAME_RULE packets, which are used by Minecraft's in-game
 * gamerule editor.  Rules that can directly affect the main thread are logged.
 */
public final class GameRulePacketGuard
{

    private static final Set<String> HIGH_RISK = Set.of(
            "randomtickspeed",
            "spawnchunkradius",
            "maxentitycramming",
            "snowaccumulationheight",
            "playerssleepingpercentage",
            "minecartmaxspeed",
            "maxcommandchainlength",
            "commandmodificationblocklimit");

    private GameRulePacketGuard()
    {
    }

    public static boolean isHighRisk(String ruleKey)
    {
        return ruleKey != null && HIGH_RISK.contains(ruleKey.toLowerCase(Locale.ROOT));
    }

    public static String describe(WrapperPlayClientSetGameRule wrapper)
    {
        if (wrapper == null)
        {
            return "?";
        }
        try
        {
            List<WrapperPlayClientSetGameRule.Entry> entries = wrapper.getEntries();
            if (entries == null || entries.isEmpty())
            {
                return "?";
            }
            StringBuilder sb = new StringBuilder();
            for (WrapperPlayClientSetGameRule.Entry entry : entries)
            {
                if (sb.length() > 0)
                {
                    sb.append(", ");
                }
                String key = entry.getGameRule() == null ? "?" : entry.getGameRule().getKey();
                sb.append(key).append('=').append(entry.getValue());
                if (isHighRisk(key))
                {
                    sb.append(" (high-risk)");
                }
            }
            return sb.toString();
        }
        catch (Throwable t)
        {
            return "?";
        }
    }
}
