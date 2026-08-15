package me.totalfreedom.totalfreedommod.cmd.resolver;

import me.totalfreedom.totalfreedommod.ProtectArea;

import me.totalfreedom.totalfreedommod.ProtectArea.ProtectedRegion;
import me.totalfreedom.api.FreedomAPI;

public class ProtectedRegionArgumentResolver implements AbstractArgumentResolver<ProtectedRegion>
{
    private final FreedomAPI plugin;

    public ProtectedRegionArgumentResolver(final FreedomAPI plugin)
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
        final ProtectedRegion region = plugin.services().require(ProtectArea.class).getProtectedRegion(arg);

        if (region == null)
        {
            throw new ArgumentResolutionException(String.format("No protected region exists with the name '%s'.", arg));
        }

        return region;
    }
}
