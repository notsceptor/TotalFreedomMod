package me.totalfreedom.totalfreedommod.world.stage.feature;

import org.bukkit.generator.LimitedRegion;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;

/**
 * One kind of thing that can be placed into a finished chunk.
 * <p>
 * Shared and run concurrently, so keep implementations immutable and take every roll from the
 * context's random. The origin is always inside the target chunk, but the overhang may not be, so
 * bounds check writes with isInRegion.
 */
public interface Feature
{
    void place(ChunkContext context, LimitedRegion region, FeatureSpec spec, int x, int y, int z);
}
