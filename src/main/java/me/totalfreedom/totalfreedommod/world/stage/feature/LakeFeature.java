package me.totalfreedom.totalfreedommod.world.stage.feature;

import org.bukkit.generator.LimitedRegion;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.profile.FeatureSpec;

/**
 * A dip filled with fluid. Spec size is the radius.
 * <p>
 * The only feature that removes blocks as well as placing them, so it needs to clear the bowl
 * before it fills it.
 */
public final class LakeFeature implements Feature
{
    @Override
    public void place(final ChunkContext context,
                      final LimitedRegion region,
                      final FeatureSpec spec,
                      final int x,
                      final int y,
                      final int z)
    {

    }
}
