package me.totalfreedom.totalfreedommod.cmd.resolver;

import me.totalfreedom.api.cmd.resolver.AbstractArgumentResolver;
import me.totalfreedom.api.cmd.resolver.ArgumentResolutionException;

public class IntegerArgumentResolver implements AbstractArgumentResolver<Integer>
{
    @Override
    public String name()
    {
        return "Integer";
    }

    @Override
    public Integer resolve(String arg, String strategy)
    {
        try
        {
            return Integer.parseInt(arg);
        }
        catch (NumberFormatException ex)
        {
            throw new ArgumentResolutionException(ex);
        }
    }
}
