package me.totalfreedom.totalfreedommod.world.profile;

import java.util.Optional;

/**
 * A world's vertical limits and water line. Clamped against WorldInfo when the profile parses.
 * <p>
 * An empty seaLevel means the world has no sea at all, which is what the end and flat worlds want.
 * There is no "sea level 0 means off" rule to remember.
 */
public record Bounds(int minY, int maxY, Optional<Integer> seaLevel)
{
    /** @throws IllegalArgumentException if minY is not below maxY, or a sea level falls outside them */
    public Bounds
    {
        if (minY >= maxY)
            throw new IllegalArgumentException("minY (" + minY + ") must be below maxY (" + maxY + ")");

        if (seaLevel.isPresent() && (seaLevel.get() < minY || seaLevel.get() > maxY))
            throw new IllegalArgumentException("seaLevel (" + seaLevel.get() + ") must fall within " + minY + " to " + maxY);
    }
}
