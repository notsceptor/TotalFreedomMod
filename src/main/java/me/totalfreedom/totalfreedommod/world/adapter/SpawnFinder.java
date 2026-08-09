package me.totalfreedom.totalfreedommod.world.adapter;

import org.bukkit.Location;
import org.bukkit.World;

import me.totalfreedom.totalfreedommod.world.GenerationProfile;

/**
 * Picks a spawn point by asking the generator's height function.
 * <p>
 * Loads no chunks, since that function is pure maths.
 */
public final class SpawnFinder
{
    private final GenerationProfile profile;

    public SpawnFinder(final GenerationProfile profile)
    {
        this.profile = profile;
    }

    /** Searches out from origin for the first column that is not underwater or void. */
    public Location findSpawn(final World world)
    {

    }
}
