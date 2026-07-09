package me.totalfreedom.totalfreedommod.cmd.resolver;

import me.totalfreedom.totalfreedommod.world.WorldWeather;

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
            weatherMode = WorldWeather.OFF;
        }

        return weatherMode;
    }
    
}
