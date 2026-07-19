package me.totalfreedom.totalfreedommod.cmd.resolver;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class WorldArgumentResolver implements AbstractArgumentResolver<World>
{
    @Override
    public String name()
    {
        return "World";
    }

    @Override
    public World resolve(String arg, String strategy)
    {
        final World world = Bukkit.getWorld(arg);
        if (world == null)
        {
            throw new ArgumentResolutionException("World \"" + arg + "\" not found.");
        }
        return world;
    }
}
