package me.totalfreedom.totalfreedommod.cmd.resolver;

import me.totalfreedom.totalfreedommod.cmd.MessageUtils;
import me.totalfreedom.totalfreedommod.world.WorldWeather;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class WeatherArgumentResolver implements AbstractArgumentResolver<WorldWeather> 
{
    @Override
    public String name()
    {
        return "WorldWeather";
    }

    @Override
    public WorldWeather resolve(String arg, String strategy)
    {
        WorldWeather weatherMode = WorldWeather.getByAlias(arg);

        if (weatherMode == null)
        {
            throw new ArgumentResolutionException(MessageUtils.parse("Invalid weather type: <type>", Placeholder.unparsed("type", arg)));
        }

        return weatherMode;
    }
    
}
