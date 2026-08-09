package me.totalfreedom.totalfreedommod.world.profile;

import java.util.Optional;

import org.bukkit.block.data.BlockData;

import me.totalfreedom.totalfreedommod.world.noise.NoiseProfile;

/**
 * How a world's terrain gets formed. One variant per generator, and each carries exactly the
 * settings that generator reads.
 * <p>
 * This is what stops a profile saying flat and then setting terrain noise, or saying heightmap with
 * no terrain at all. Those states cannot be typed, so nothing has to check for them.
 * <p>
 * Pattern match it once when wiring up the chunk generator to pick the stages. Nothing per chunk
 * and nothing per block should ever look at it again.
 */
public sealed interface Shape
{
    /** Fixed layers, no noise. */
    record Flat(LayerStack layers) implements Shape
    {
    }

    /** 2D height through a spline. No overhangs. */
    record Heightmap(Terrain terrain,
                     Optional<River> river,
                     Optional<Caves> caves) implements Shape
    {
    }

    /** 3D density. Overhangs and floating islands, at roughly fifty times the samples. */
    record Density(NoiseProfile noise,
                   double warp,
                   Optional<Caves> caves) implements Shape
    {
    }

    /** warp offsets the sample coordinates by a second noise. */
    record Terrain(NoiseProfile noise, Spline spline, double warp)
    {
    }

    /** Pulls height toward sea level where the noise is near zero. */
    record River(NoiseProfile noise, double threshold, int depth, BlockData bedBlock)
    {
    }

    /**
     * floodLevel is the y below which a carved out block fills with water instead of air.
     * <p>
     * Keep minY above the bedrock layer, since carving runs after bedrock is written.
     *
     * @throws IllegalArgumentException if minY is above maxY
     */
    record Caves(NoiseProfile noise, double threshold, int minY, int maxY, int floodLevel)
    {
        public Caves
        {
            if (minY > maxY)
                throw new IllegalArgumentException("minY (" + minY + ") must not be above maxY (" + maxY + ")");
        }
    }
}
