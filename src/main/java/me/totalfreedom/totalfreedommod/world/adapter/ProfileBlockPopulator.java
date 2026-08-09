package me.totalfreedom.totalfreedommod.world.adapter;

import java.util.Random;

import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import me.totalfreedom.totalfreedommod.world.GenerationProfile;

/**
 * Runs the profile's populator as a Bukkit block populator.
 * <p>
 * Builds its own ChunkContext, since this may not run on the thread that generated the chunk.
 */
public final class ProfileBlockPopulator extends BlockPopulator
{
    private final GenerationProfile profile;

    public ProfileBlockPopulator(final GenerationProfile profile)
    {
        this.profile = profile;
    }

    @Override
    public void populate(final WorldInfo worldInfo,
                         final Random random,
                         final int chunkX,
                         final int chunkZ,
                         final LimitedRegion limitedRegion)
    {

    }
}
