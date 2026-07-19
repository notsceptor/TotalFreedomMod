package me.totalfreedom.totalfreedommod.cmd.resolver;

import me.totalfreedom.totalfreedommod.ProtectArea.ProtectedRegion;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;

public class ProtectedRegionArgumentResolver implements AbstractArgumentResolver<ProtectedRegion>
{
    private final TotalFreedomMod plugin;

    public ProtectedRegionArgumentResolver(final TotalFreedomMod plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public String name()
    {
        return "ProtectedRegion";
    }

    @Override
    public ProtectedRegion resolve(final String arg, final String strategy)
    {
        final ProtectedRegion region = plugin.pa.getProtectedRegion(arg);

        if (region == null)
        {
            throw new ArgumentResolutionException(String.format("No protected region exists with the name '%s'.", arg));
        }

        return region;
    }
}
