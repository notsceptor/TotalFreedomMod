package me.totalfreedom.totalfreedommod.cmd.resolver;

import me.totalfreedom.totalfreedommod.world.WorldTime;

public class WorldTimeArgumentResolver implements AbstractArgumentResolver<WorldTime> 
{
    @Override
    public String name()
    {
        return "WorldTime";
    }

    @Override
    public WorldTime resolve(String arg, String strategy)
    {
        WorldTime resolved = WorldTime.getByAlias(arg);

        if (resolved == null)
        {
            resolved = WorldTime.NOON;
        }

        return resolved;
    }
}
