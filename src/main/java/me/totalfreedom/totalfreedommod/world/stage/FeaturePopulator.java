package me.totalfreedom.totalfreedommod.world.stage;

import java.util.List;
import java.util.Random;

import org.bukkit.block.Biome;
import org.bukkit.generator.LimitedRegion;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.base.Populator;
import me.totalfreedom.totalfreedommod.world.profile.Anchor;
import me.totalfreedom.totalfreedommod.world.profile.FeatureSpec;
import me.totalfreedom.totalfreedommod.world.profile.Palette;
import me.totalfreedom.totalfreedommod.world.stage.feature.FeatureRegistry;

/**
 * Rolls each feature in the profile against the chunk and hands off the hits. This only decides
 * what gets placed and where; the features do the placing.
 */
public final class FeaturePopulator implements Populator
{
    private final List<FeatureSpec> specs;
    private final FeatureRegistry registry;

    public FeaturePopulator(final List<FeatureSpec> specs, final FeatureRegistry registry)
    {
        this.specs = specs;
        this.registry = registry;
    }

    @Override
    public void populate(final ChunkContext context, final LimitedRegion data)
    {
        final Random random = context.getRandom();

        this.specs.forEach(spec -> this.roll(context, data, random, spec));
    }

    private void roll(final ChunkContext context,
                      final LimitedRegion data,
                      final Random random,
                      final FeatureSpec spec)
    {
        for (int attempt = 0; attempt < spec.rarity(); attempt++)
        {
            final int localX = random.nextInt(16);
            final int localZ = random.nextInt(16);
            final int worldX = context.worldX(localX);
            final int worldZ = context.worldZ(localZ);

            if (!spec.appliesTo(this.biomeAt(context, worldX, worldZ)))
                continue;

            final int y = spec.detail().anchor() == Anchor.SURFACE
                ? context.columnTop(localX, localZ) + 1
                : spec.minY() + random.nextInt(spec.maxY() - spec.minY() + 1);

            if (y < spec.minY() || y > spec.maxY())
                continue;

            this.registry.place(context, data, spec.detail(), worldX, y, worldZ);
        }
    }

    /**
     * Same temperature/humidity lookup {@link me.totalfreedom.totalfreedommod.world.adapter.ProfileBiomeProvider}
     * uses. Duplicated rather than shared, since that class does not expose it as a static helper.
     */
    private Biome biomeAt(final ChunkContext context, final int worldX, final int worldZ)
    {
        final Palette palette = context.getProfile().palette();
        final Palette.Climate climate = palette.climate();
        final int sampleX = (int) (worldX * climate.scale());
        final int sampleZ = (int) (worldZ * climate.scale());
        final double temperature = climate.temperature().sample(sampleX, sampleZ);
        final double humidity = climate.humidity().sample(sampleX, sampleZ);

        return palette.biomes()
                      .stream()
                      .filter(band -> band.matches(temperature, humidity))
                      .findFirst()
                      .map(Palette.BiomeBand::biome)
                      .orElse(palette.fallback());
    }
}
