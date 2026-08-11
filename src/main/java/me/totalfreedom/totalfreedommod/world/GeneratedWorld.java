package me.totalfreedom.totalfreedommod.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.world.adapter.ProfileChunkGenerator;
import me.totalfreedom.totalfreedommod.world.adapter.SpawnFinder;
import me.totalfreedom.totalfreedommod.world.profile.WorldSettings;

/**
 * A custom world built from a profile. Applies the profile's world settings to the WorldCreator and
 * takes its spawn point from the spawn finder.
 * <p>
 * Keyed under the {@code minecraft} namespace so the level/folder name Paper derives from the key
 * matches {@link GenerationProfile#name()} exactly, keeping it a plain lookup for
 * {@link WorldManager#gotoWorld} and {@link GenerationService#profile}.
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

        final World world = Bukkit.getServer().createWorld(worldCreator);

        if (world == null)
            return null;

        final Location spawn = new SpawnFinder(this.profile, generator.generator()).findSpawn(world);
        world.setSpawnLocation(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());

        return world;
    }

    public GenerationProfile getProfile()
    {
        return this.profile;
    }
}
