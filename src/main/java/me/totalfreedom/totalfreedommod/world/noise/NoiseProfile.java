package me.totalfreedom.totalfreedommod.world.noise;

/**
 * One noise field's settings. Feed it to {@link NoiseField#of} to get a usable field.
 * <p>
 * Amplitude is set by the spline for terrain and by the threshold for caves, so it is not a knob
 * here. The seed comes from the field's role name mixed with the world seed when the profile
 * parses.
 *
 * @throws IllegalArgumentException if octaves is below one, or frequency is not positive
 */
public record NoiseProfile(NoiseType type,
                           int octaves,
                           double frequency,
                           double persistence,
                           double lacunarity,
                           boolean ridged)
{
    public NoiseProfile
    {

    }
}
