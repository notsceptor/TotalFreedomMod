package me.totalfreedom.totalfreedommod.cmd.resolver;

import me.totalfreedom.api.cmd.resolver.AbstractArgumentResolver;
import me.totalfreedom.api.cmd.resolver.ArgumentResolutionException;

public class FloatArgumentResolver implements AbstractArgumentResolver<Float>
{
    @Override
    public String name()
    {
        return "Float";
    }

    @Override
    public Float resolve(String arg, String strategy)
    {
        try
        {
            return Float.parseFloat(arg);
        }
        catch (NumberFormatException ex)
        {
            throw new ArgumentResolutionException(ex);
        }
    }
}
