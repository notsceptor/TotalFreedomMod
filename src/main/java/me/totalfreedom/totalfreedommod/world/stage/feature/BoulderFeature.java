package me.totalfreedom.totalfreedommod.world.stage.feature;

import org.bukkit.generator.LimitedRegion;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.profile.FeatureDetail;

/** A rough blob sitting on the surface. Spec size is the radius. */
public final class BoulderFeature implements Feature<FeatureDetail.Boulder>
{
    @Override
    public void place(final ChunkContext context,
                      final LimitedRegion region,
                      final FeatureDetail.Boulder detail,
                      final int x,
                      final int y,
                      final int z)
    {

    }
}
