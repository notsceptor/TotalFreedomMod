package me.totalfreedom.totalfreedommod.world;

import org.bukkit.World;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;

/**
 * A custom world built from a profile. Applies the profile's world settings to the WorldCreator and
 * takes its spawn point from the spawn finder.
 */
public class GeneratedWorld extends CustomWorld
{
    private final GenerationProfile profile;

    public GeneratedWorld(final TotalFreedomMod plugin, final GenerationProfile profile, final String displayName)
    {
        super(plugin, profile.name(), displayName);

        this.profile = profile;
    }

    @Override
    protected World generateWorld()
    {

    }

    public GenerationProfile getProfile()
    {
        return this.profile;
    }
}
