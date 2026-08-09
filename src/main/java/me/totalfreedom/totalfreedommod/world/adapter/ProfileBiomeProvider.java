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
 */
public final class ProfileBiomeProvider extends BiomeProvider
{
    private final GenerationProfile profile;

    public ProfileBiomeProvider(final GenerationProfile profile)
    {
        this.profile = profile;
    }

    @Override
    public Biome getBiome(final WorldInfo worldInfo, final int x, final int y, final int z)
    {

    }

    /** Must list every biome getBiome can return, or the server rejects the provider. */
    @Override
    public List<Biome> getBiomes(final WorldInfo worldInfo)
    {

    }
}
