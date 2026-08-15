package me.totalfreedom.totalfreedommod.world;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.world.adapter.ProfileChunkGenerator;
import me.totalfreedom.totalfreedommod.world.profile.WorldSettings;

/**
 * A custom world built from a profile. Applies the profile's world settings to the WorldCreator.
 * <p>
 * Keyed under the {@code minecraft} namespace so the level/folder name Paper derives from the key
 * matches {@link GenerationProfile#name()} exactly, keeping it a plain lookup for
 * {@link WorldManager#gotoWorld} and {@link GenerationService#profile}.
 * <p>
 * Sets no spawn location itself. {@link ProfileChunkGenerator#getFixedSpawnLocation} already runs
 * the same deterministic search, and Bukkit applies whatever it returns to the world during
 * {@link Bukkit#createWorld}, so searching again here would only repeat that same scan for the same
 * answer.
 */
public class GeneratedWorld extends CustomWorld
{
    private final GenerationProfile profile;

    public GeneratedWorld(final TotalFreedomMod plugin, final GenerationProfile profile, final String displayName)
    {
        super(plugin, profile.name(), displayName);

        this.profile = profile;
    }

    @Override
    protected World generateWorld()
    {
        final WorldSettings settings = this.profile.world();
        final ProfileChunkGenerator generator = new ProfileChunkGenerator(this.profile);

        final WorldCreator worldCreator = WorldCreator.ofKey(NamespacedKey.minecraft(getName()));
        worldCreator.environment(settings.environment());
        worldCreator.generateStructures(settings.generateStructures());
        worldCreator.generator(generator);
        settings.seed().ifPresent(seed -> worldCreator.seed(seed.longValue()));

        return Bukkit.getServer().createWorld(worldCreator);
    }

    public GenerationProfile getProfile()
    {
        return this.profile;
    }
}
