package me.totalfreedom.totalfreedommod.world.adapter;

import java.util.List;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import me.totalfreedom.totalfreedommod.world.GenerationProfile;

/**
 * Samples the profile's temperature and humidity noise and drops the result into a biome band.
 * <p>
 * Sets grass and water colour and what mobs spawn, and gives the designer's rules and the
 * populator's filters something to match against.
 * <p>
 * A band's target may be a plain vanilla biome or a TFM-only one; either way this only ever hands
 * Bukkit {@link me.totalfreedom.totalfreedommod.world.profile.BiomeTarget#display()}'s result, since
 * that is the one thing the client and the server's own biome-driven systems can understand.
 */
public final class ProfileBiomeProvider extends BiomeProvider
{
    private final GenerationProfile profile;

    public ProfileBiomeProvider(final GenerationProfile profile)
    {
        this.profile = profile;
    }

    /** TODO: {@code return this.profile.palette().resolveBiome(x, z); } once resolveBand is implemented. */
    @Override
    public Biome getBiome(final WorldInfo worldInfo, final int x, final int y, final int z)
    {

    }

    /**
     * Must list every biome getBiome can return, or the server rejects the provider.
     * <p>
     * TODO: collect every band's {@code target().display()} plus {@code palette.fallback()},
     * deduplicated.
     */
    @Override
    public List<Biome> getBiomes(final WorldInfo worldInfo)
    {

    }
}
