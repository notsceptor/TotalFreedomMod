package me.totalfreedom.totalfreedommod.command.resolver;

import me.totalfreedom.totalfreedommod.command.FreedomCommand;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class OfflinePlayerArgumentResolver implements AbstractArgumentResolver<OfflinePlayer>
{
    @Override
    public String name()
    {
        return "OfflinePlayer";
    }

    @Override
    public OfflinePlayer resolve(String arg, String strategy)
    {
        OfflinePlayer offlinePlayer;
        // UUID
        try
        {
            final UUID uuid = UUID.fromString(arg);
            offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        }
        // Username
        catch (IllegalArgumentException ex)
        {
            offlinePlayer = Bukkit.getOfflinePlayer(arg);
        }

        if (!offlinePlayer.isOnline() && !offlinePlayer.hasPlayedBefore() && strategy.equalsIgnoreCase("hideUnknownPlayers"))
        {
            throw new ArgumentResolutionException(AdventureUtil.componentToPlainText(FreedomCommand.PLAYER_NOT_FOUND));
        }

        return offlinePlayer;
    }
}
