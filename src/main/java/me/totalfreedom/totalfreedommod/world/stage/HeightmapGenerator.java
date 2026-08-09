package me.totalfreedom.totalfreedommod.world.stage;

import org.bukkit.generator.ChunkGenerator;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.base.Generator;
import me.totalfreedom.totalfreedommod.world.noise.NoiseField;
import me.totalfreedom.totalfreedommod.world.profile.Bounds;
import me.totalfreedom.totalfreedommod.world.profile.Materials;
import me.totalfreedom.totalfreedommod.world.profile.Spline;

/**
 * The default mode. 2D noise through a spline, with rivers pulling height toward sea level. No
 * overhangs, and it covers most of what a custom survival world wants.
 * <p>
 * Sample on a grid and interpolate between the samples. Sampling every block is 98,304 positions
 * per chunk, times however many octaves the noise has.
 */
public final class HeightmapGenerator implements Generator
{
    private final NoiseField terrain;
    private final NoiseField river;
    private final Spline spline;
    private final Bounds bounds;
    private final Materials materials;
    private final double warp;

    public HeightmapGenerator(final NoiseField terrain,
                              final NoiseField river,
                              final Spline spline,
                              final Bounds bounds,
                              final Materials materials,
                              final double warp)
    {
        this.terrain = terrain;
        this.river = river;
        this.spline = spline;
        this.bounds = bounds;
        this.materials = materials;
        this.warp = warp;
    }

    @Override
    public void generateBase(final ChunkContext context, final ChunkGenerator.ChunkData data)
    {

    }

    @Override
    public int surfaceHeight(final ChunkContext context, final int worldX, final int worldZ)
    {

    }
}
