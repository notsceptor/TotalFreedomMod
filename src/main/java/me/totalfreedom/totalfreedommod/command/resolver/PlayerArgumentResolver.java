package me.totalfreedom.totalfreedommod.command.resolver;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.command.FreedomCommand;

public class PlayerArgumentResolver implements AbstractArgumentResolver<Player>
{
    private Player resolveDefault(String arg)
    {
        Player player = Bukkit.getPlayer(arg);
        if (player == null)
            throw new ArgumentResolutionException(FreedomCommand.PLAYER_NOT_FOUND);
        return player;
    }

    @Override
    public String name()
    {
        return "Player";
    }

    @Override
    public Player resolve(String arg, String strategy)
    {
        return resolveDefault(arg);
    }
    
}
