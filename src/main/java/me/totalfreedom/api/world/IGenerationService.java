package me.totalfreedom.api.world;

import java.util.Optional;
import java.util.Set;

import org.bukkit.generator.ChunkGenerator;

import me.totalfreedom.totalfreedommod.world.GenerationProfile;

public interface IGenerationService
{
    Optional<GenerationProfile> profile(String worldName);

    /** Empty if no profile covers the world, or if its file failed to parse. */
    Optional<ChunkGenerator> generatorFor(String worldName);

    /** Only worlds whose profiles parsed. A file that failed does not appear here. */
    Set<String> available();

    /**
     * Re-parses every available profile and drops any no longer on disk.
     */
    void reload();
}
