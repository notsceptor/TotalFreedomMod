package me.totalfreedom.totalfreedommod.world.profile;

import java.util.Optional;

import org.bukkit.World;

/**
 * Settings applied to the WorldCreator at creation. Never read during chunk generation.
 * <p>
 * keepSpawnLoaded makes createWorld generate the entire spawn square on the main thread. Leave it
 * off unless a world needs it.
 */
public record WorldSettings(World.Environment environment,
                            boolean generateStructures,
                            boolean keepSpawnLoaded,
                            Optional<Long> seed,
                            VanillaFlags vanilla)
{
    /**
     * Which vanilla generation steps run alongside ours. All default off. The chunk generator hands
     * these to Bukkit through its shouldGenerate methods.
     * <p>
     * Turning on decorations gets you vanilla trees, flowers, and ore without writing any features.
     */
    public record VanillaFlags(boolean surface,
                               boolean caves,
                               boolean decorations,
                               boolean mobs,
                               boolean structures)
    {
    }
}
