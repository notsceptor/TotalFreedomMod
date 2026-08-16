package me.totalfreedom.totalfreedommod.cmd.resolver;

import me.totalfreedom.api.cmd.resolver.AbstractArgumentResolver;
import me.totalfreedom.api.cmd.resolver.ArgumentResolutionException;

public class DoubleArgumentResolver implements AbstractArgumentResolver<Double>
{
    @Override
    public String name()
    {
        return "Double";
    }

    @Override
    public Double resolve(String arg, String strategy)
    {
        try
        {
            return Double.parseDouble(arg);
        }
        catch (NumberFormatException ex)
        {
            throw new ArgumentResolutionException(ex);
        }
    }
}
