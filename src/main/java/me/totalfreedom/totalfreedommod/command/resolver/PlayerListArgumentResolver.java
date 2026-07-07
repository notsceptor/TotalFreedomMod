package me.totalfreedom.totalfreedommod.command.resolver;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerListArgumentResolver implements AbstractArgumentResolver<List<Player>>
{
    private List<Player> resolveDefault(String arg)
    {
        final String[] candidates = arg.split(",");
        final List<Player> results = new ArrayList<>();

        for (String candidate : candidates)
        {
            candidate = candidate.trim();

            Player player;

            // UUID
            try
            {
                final UUID uuid = UUID.fromString(candidate);
                player = Bukkit.getPlayer(uuid);
            }
            // Username
            catch (IllegalArgumentException ex)
            {
                player = Bukkit.getPlayer(candidate);
            }

            if (player == null)
                throw new ArgumentResolutionException("Player not found: " + candidate);

            results.add(player);
        }

        return results;
    }

    @Override
    public String name()
    {
        return "PlayerList";
    }

    @Override
    public List<Player> resolve(String arg, String strategy)
    {
        return resolveDefault(arg);
    }
    
}
