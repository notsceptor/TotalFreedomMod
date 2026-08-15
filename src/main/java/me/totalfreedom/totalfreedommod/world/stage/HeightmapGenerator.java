package me.totalfreedom.totalfreedommod.world.stage;

import java.util.List;
import java.util.Optional;

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
 * <p>
 * terrain/spline/warp are the fallback used wherever regions is empty or its selector matches no
 * listed region, exactly as they always have been. TODO: generateBase/surfaceHeight need to sample
 * regions.selector() per column once regions is present, blend the matched BuiltRegion's own
 * noise/spline/warp in over blendWidth, and fall back to the fields above otherwise.
 */
public final class HeightmapGenerator implements Generator
{
    private final NoiseField terrain;
    private final Optional<NoiseField> river;
    private final Spline spline;
    private final Bounds bounds;
    private final Materials materials;
    private final double warp;
    private final Optional<RegionSet> regions;

    public HeightmapGenerator(final NoiseField terrain,
                              final Optional<NoiseField> river,
                              final Spline spline,
                              final Bounds bounds,
                              final Materials materials,
                              final double warp,
                              final Optional<RegionSet> regions)
    {
        this.terrain = terrain;
        this.river = river;
        this.spline = spline;
        this.bounds = bounds;
        this.materials = materials;
        this.warp = warp;
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
     * One profile region, already built: a sampled noise field and a ready spline, not the raw
     * settings {@link me.totalfreedom.totalfreedommod.world.profile.Shape.Region} carries.
     */
    public record BuiltRegion(String name, 
                              double min, 
                              double max, 
                              NoiseField noise, 
                              Spline spline, 
                              double warp)
    {
    }

    /**
     * The selector noise plus its built regions and blend width. {@code ProfileChunkGenerator.wire()}
     * builds this from a profile's {@link me.totalfreedom.totalfreedommod.world.profile.Shape.Regions}
     * by calling {@code NoiseField.of} once per region plus once for the selector itself, each with
     * its own role string (e.g. {@code "terrain-region-<name>"}, {@code "terrain-selector"}) so no two
     * fields in one profile collapse onto the same random stream.
     */
    public record RegionSet(NoiseField selector, double blendWidth, List<BuiltRegion> regions)
    {
    }
}
