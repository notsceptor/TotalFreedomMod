package me.totalfreedom.totalfreedommod.world.profile;

import java.util.List;

/**
 * Maps raw noise to a terrain height through control points. Binary search plus a lerp per column.
 * <p>
 * Plateaus, cliffs, and flat plains all come out of one array of points.
 */
public record Spline(double[] inputs, double[] outputs)
{
    /**
     * @param points {@code [noise, height]} pairs
     * @throws IllegalArgumentException if fewer than two points, or the noise values are not
     *                                  strictly ascending, which would make the search ambiguous
     */
    public static Spline of(final List<double[]> points)
    {

    }

    /** Clamps to the first and last point outside their range. */
    public double apply(final double noise)
    {

    }
}
