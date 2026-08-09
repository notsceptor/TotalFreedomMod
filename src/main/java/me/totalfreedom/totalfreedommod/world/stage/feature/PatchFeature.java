package me.totalfreedom.totalfreedommod.world.stage.feature;

import org.bukkit.generator.LimitedRegion;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.profile.FeatureDetail;

/** A scatter of blocks on the surface; flowers, grass, that sort of thing. Spec size is spread. */
public final class PatchFeature implements Feature<FeatureDetail.Patch>
{
    @Override
    public void place(final ChunkContext context,
                      final LimitedRegion region,
                      final FeatureDetail.Patch detail,
                      final int x,
                      final int y,
                      final int z)
    {

    }
}
