package me.totalfreedom.totalfreedommod.world.profile;

import java.util.List;

import org.bukkit.block.Biome;

import me.totalfreedom.totalfreedommod.world.noise.NoiseField;

/**
 * What the world is made of, plus the climate that decides which materials go where.
 * <p>
 * Climate and biomes are shared: the biome provider, the designer's biome-specific rules, and the
 * populator's biome filters all read them. Nothing can ask Bukkit for a biome mid-generation.
 */
public record Palette(Materials materials,
                      List<SurfaceRule> surface,
                      Climate climate,
                      Biome fallback,
                      List<BiomeBand> biomes)
{
    /** @throws IllegalArgumentException if the rule list is empty, which would leave bare filler */
    public Palette
    {
        if (surface.isEmpty())
            throw new IllegalArgumentException("surface rules must not be empty");

        surface = List.copyOf(surface);
        biomes = List.copyOf(biomes);
    }

    /** The two noise fields a position is scored against to land it in a band. */
    public record Climate(NoiseField temperature, NoiseField humidity, double scale)
    {
    }

    /**
     * One biome table entry. First band containing both values wins, and anything no band covers
     * gets the palette's fallback.
     *
     * @throws IllegalArgumentException if either range is inverted or falls outside -1 to 1
     */
    public record BiomeBand(Biome biome,
                            double minTemperature,
                            double maxTemperature,
                            double minHumidity,
                            double maxHumidity)
    {
        public BiomeBand
        {
            if (minTemperature > maxTemperature || minTemperature < -1 || maxTemperature > 1)
                throw new IllegalArgumentException("temperature range must fall within -1 to 1");

            if (minHumidity > maxHumidity || minHumidity < -1 || maxHumidity > 1)
                throw new IllegalArgumentException("humidity range must fall within -1 to 1");
        }

        public boolean matches(final double temperature, final double humidity)
        {
            return temperature >= this.minTemperature && temperature <= this.maxTemperature
                && humidity >= this.minHumidity && humidity <= this.maxHumidity;
        }
    }
}
