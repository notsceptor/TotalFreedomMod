package me.totalfreedom.totalfreedommod.world.noise;

import org.bukkit.util.noise.OctaveGenerator;

/**
 * A built noise field. Immutable, sampled concurrently from every worldgen thread.
 * <p>
 * Build once at compile and hand it to a stage as a final field; that is what guarantees other
 * threads see it fully constructed. Never reconfigure it afterward, setScale mutates and may only
 * be touched inside {@link #of}.
 */
public final class NoiseField
{
    private final NoiseProfile profile;
    private final OctaveGenerator generator;

    private NoiseField(final NoiseProfile profile, final OctaveGenerator generator)
    {
        this.profile = profile;
        this.generator = generator;
    }

    /**
     * @param role stable name like "terrain" or "caves", mixed into the seed so two fields in one
     *             profile cannot end up on the same stream
     */
    public static NoiseField of(final NoiseProfile profile, final long seed, final String role)
    {

    }

    /** 2D sample, in the range -1 to 1. */
    public double sample(final int x, final int z)
    {

    }

    /** 3D sample, in the range -1 to 1. */
    public double sample(final int x, final int y, final int z)
    {

    }

    public NoiseProfile getProfile()
    {
        return this.profile;
    }
}
