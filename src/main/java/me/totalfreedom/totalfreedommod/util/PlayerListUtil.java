package me.totalfreedom.totalfreedommod.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.AdminList;
import me.totalfreedom.totalfreedommod.rank.CustomRank;
import me.totalfreedom.totalfreedommod.rank.Rank;

/**
 * Plaintext online player list shared by automated console contexts.
 */
public final class PlayerListUtil
{
    private static final String CANNOT_RETRIEVE_PLAYERS_MESSAGE = "Unable to retrieve list of players at this time.";

    private PlayerListUtil()
    {
    }

    public static String buildPlainList()
    {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers())
        {
            names.add(player.getName());
        }
        int max = Bukkit.getMaxPlayers();
        return "There are " + names.size() + "/" + max + " players online:\n" + StringUtils.join(names, ", ");
    }

    public static String buildRankList()
    {
        final TotalFreedomMod plugin = TotalFreedomMod.plugin();
        // If this somehow fails, just send back a list with no additional rank context
        if (plugin == null)
            return buildPlainList();

        final var players = Bukkit.getOnlinePlayers();
        if (players == null)
            return CANNOT_RETRIEVE_PLAYERS_MESSAGE;

        // Build a map from ranks to players that have that rank, with OP as a default
        final Map<CustomRank, List<Player>> playerRanks = new HashMap<>();
        final var defaultRank = CustomRank.fromLegacyRank(Rank.OP);
        playerRanks.put(defaultRank, new ArrayList<>());
        for (final var player : players)
        {
            final var admin = plugin.al.getAdmin(player);
            // If they're an admin, we can pull their rank
            if (admin != null)
            {
                final var rank = plugin.rm.getCustomRank(admin.getCustomRankId());
                if (rank == null)
                {
                    // Give them OP if somehow they don't have a rank
                    playerRanks.get(defaultRank).add(player);
                    continue;
                }
                // Add the player to their corresponding list, or create the list if it doesn't exist yet
                playerRanks.merge(rank, new ArrayList<>(), (v, _) -> {
                    v.add(player);
                    return v;
                });
                continue;
            }
            // Otherwise default to OP
            playerRanks.get(defaultRank).add(player);
        }

        final int max = Bukkit.getMaxPlayers();

        return String.format("There are %s/%s players online.\n\n%s", players.size(), max, StringUtils.join(plugin.rm.getCustomRanks()
            .values()
            .stream()
            .sorted((r1, r2) -> Integer.compare(r2.getLevel(), r1.getLevel())) // Sort in reverse level order, highest ranks listed first
            .map(rank -> {
                // Get all players with that rank
                final List<Player> rankedPlayers = playerRanks.get(rank);
                // Make a nice message out of it
                return String.format("%ss (%s): %s",
                    rank.getName(),
                    rankedPlayers.size(),
                    StringUtils.join(rankedPlayers.stream().map(Player::getName), ", "));
            }), "\n"));
    }
}
