package me.totalfreedom.totalfreedommod.cmd.resolver;

import me.totalfreedom.totalfreedommod.cmd.MessageUtils;
import me.totalfreedom.totalfreedommod.world.WorldTime;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

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
            throw new ArgumentResolutionException(MessageUtils.parse("Invalid world time: <type>", Placeholder.unparsed("type", arg)));
        }

        return resolved;
    }
}
