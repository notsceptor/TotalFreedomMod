package me.totalfreedom.totalfreedommod.command.resolver;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.ProtectArea.ProtectedRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ProtectedRegionResolver implements AbstractArgumentResolver<ProtectedRegion>
{
    @Override
    public String name()
    {
        return "ProtectedRegion";
    }

    public ProtectedRegion resolveDefault(String name)
    {
        final ProtectedRegion region = TotalFreedomMod.plugin().pa.getProtectedRegion(name);
        if (region == null)
            throw new ArgumentResolutionException(Component.text("There is no protected region named '")
                .append(Component.text(name))
                .append(Component.text("'."))
                .color(NamedTextColor.GRAY));
        return region;
    }

    @Override
    public ProtectedRegion resolve(String name, String strategy)
    {
        return resolveDefault(name);
    }
    
}
