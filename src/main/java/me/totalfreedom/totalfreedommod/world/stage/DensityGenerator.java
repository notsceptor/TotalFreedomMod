package me.totalfreedom.totalfreedommod.world.stage;

import java.util.List;
import java.util.Optional;

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
 * <p>
 * density is the fallback used wherever regions is empty or its selector matches no listed region.
 * TODO: generateBase/surfaceHeight need to sample regions.selector() per column once regions is
 * present, blend the matched BuiltRegion's own noise/warp in over blendWidth, and fall back to
 * density otherwise. Same shape as {@link HeightmapGenerator}, just without a spline.
 */
public final class DensityGenerator implements Generator
{
    private final NoiseField density;
    private final Bounds bounds;
    private final Materials materials;
    private final Optional<RegionSet> regions;

    public DensityGenerator(final NoiseField density,
                            final Bounds bounds,
                            final Materials materials,
                            final Optional<RegionSet> regions)
    {
        this.density = density;
        this.bounds = bounds;
        this.materials = materials;
        this.regions = regions;
    }

    @Override
    public void generateBase(final ChunkContext context, final ChunkGenerator.ChunkData data)
    {

    }

    @Override
    public int surfaceHeight(final int worldX, final int worldZ)
    {

    }

    /**
     * One profile region, already built: a sampled noise field, not the raw settings
     * {@link me.totalfreedom.totalfreedommod.world.profile.Shape.DensityLayer} carries.
     */
    record BuiltRegion(String name, double min, double max, NoiseField noise, double warp)
    {
    }

    /** See {@link HeightmapGenerator.RegionSet}; the same role-string requirement applies here too. */
    record RegionSet(NoiseField selector, double blendWidth, List<BuiltRegion> regions)
    {
    }
}
