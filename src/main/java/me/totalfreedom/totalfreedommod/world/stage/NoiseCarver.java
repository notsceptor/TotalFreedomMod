package me.totalfreedom.totalfreedommod.world.stage;

import me.totalfreedom.totalfreedommod.world.base.Carver;
import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.noise.NoiseField;

/**
 * Cuts caves and ravines wherever the noise goes past the threshold.
 * <p>
 * Tighten the threshold as you get near the context's terrain height and cave mouths blend into the
 * hillside instead of cutting a flat wall into it.
 */
public final class NoiseCarver implements Carver
{
    private final NoiseField noise;
    private final double threshold;
    private final int minY;
    private final int maxY;

    public NoiseCarver(final NoiseField noise, final double threshold, final int minY, final int maxY)
    {
        this.noise = noise;
        this.threshold = threshold;
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    public boolean isCarved(final ChunkContext context, final int worldX, final int y, final int worldZ)
    {

    }

    @Override
    public int minY()
    {
        return this.minY;
    }

    @Override
    public int maxY()
    {
        return this.maxY;
    }
}
