package me.totalfreedom.totalfreedommod.cmd;

import java.util.List;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.cmd.internal.FuzzyMatch;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * Shared tab-completion sources for commands that take a player name as a plain {@code String}.
 * <p>
 * Those arguments exist so offline and previously-known names can be targeted, which is exactly why
 * they get no suggestions from the command framework.
 * <p>
 * Every source runs the candidates through {@link FuzzyMatch} so completion behaves the same as the
 * framework's built-in suggesters.
 */
final class NameCandidates
{

    private NameCandidates()
    {
    }

    static List<String> online(Server server, String partial)
    {
        return FuzzyMatch.filter(
                server.getOnlinePlayers()
                      .stream()
                      .map(Player::getName)
                      .sorted()
                      .toList(),
                partial);
    }

    static List<String> banned(TotalFreedomMod plugin, String partial)
    {
        return FuzzyMatch.filter(
                plugin.bm.getUsernameBans()
                         .stream()
                         .map(Ban::getUsername)
                         .filter(name -> name != null)
                         .distinct()
                         .sorted()
                         .toList(),
                partial);
    }

    static List<String> permbanned(TotalFreedomMod plugin, String partial)
    {
        return FuzzyMatch.filter(
                plugin.pm.getPermbannedNames()
                         .stream()
                         .sorted()
                         .toList(),
                partial);
    }

    static List<String> whitelisted(String partial)
    {
        return FuzzyMatch.filter(
                Bukkit.getWhitelistedPlayers()
                      .stream()
                      .map(OfflinePlayer::getName)
                      .filter(name -> name != null)
                      .sorted()
                      .toList(),
                partial);
    }
}
