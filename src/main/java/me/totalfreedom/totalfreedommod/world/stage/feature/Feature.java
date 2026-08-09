package me.totalfreedom.totalfreedommod.world.stage.feature;

import org.bukkit.generator.LimitedRegion;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.profile.FeatureDetail;

/**
 * Places one kind of thing into a finished chunk.
 * <p>
 * Typed to its own detail, so a tree gets a Tree and never has to check what it was handed. The
 * populator's switch over the sealed detail is what makes that safe.
 * <p>
 * Shared and run concurrently, so keep implementations immutable and take every roll from the
 * context's random. The origin is always inside the target chunk, but the overhang may not be, so
 * bounds check writes with isInRegion.
 *
 * @param <D> the detail variant this feature places
 */
public interface Feature<D extends FeatureDetail>
{
    void place(ChunkContext context, LimitedRegion region, D detail, int x, int y, int z);

    /**
     * Interpolates between two points. Written the precise way, {@code from*(1-t) + to*t}, so that
     * a progress of exactly 1 returns exactly {@code to}. The shorter {@code from + t*(to-from)}
     * rounds twice and can miss the far endpoint by an ulp.
     * <p>
     * Nothing today loops far enough to reach 1, but this is shared, and the next feature to use it
     * should not have to loop a particular way to stay correct.
     */
    default double lerp(final double progress, final double from, final double to)
    {
        return from * (1.0D - progress) + to * progress;
    }
}
