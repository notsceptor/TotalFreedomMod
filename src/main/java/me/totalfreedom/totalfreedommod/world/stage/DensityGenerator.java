package me.totalfreedom.totalfreedommod.world.stage;

import org.bukkit.generator.ChunkGenerator;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.base.Generator;
import me.totalfreedom.totalfreedommod.world.noise.NoiseField;
import me.totalfreedom.totalfreedommod.world.profile.Bounds;
import me.totalfreedom.totalfreedommod.world.profile.Materials;

/**
 * 3D mode. Samples on a grid in all three directions and interpolates between the samples, which
 * gets you overhangs and floating islands.
 * <p>
 * Roughly fifty times the samples of heightmap mode, so only use it if a world actually needs
 * those shapes.
 */
public final class DensityGenerator implements Generator
{
    private final NoiseField density;
    private final Bounds bounds;
    private final Materials materials;

    public DensityGenerator(final NoiseField density, final Bounds bounds, final Materials materials)
    {
        this.density = density;
        this.bounds = bounds;
        this.materials = materials;
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
