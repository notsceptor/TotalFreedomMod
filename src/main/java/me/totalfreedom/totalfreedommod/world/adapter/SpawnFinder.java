package me.totalfreedom.totalfreedommod.world.adapter;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;

import me.totalfreedom.totalfreedommod.world.GenerationProfile;
import me.totalfreedom.totalfreedommod.world.base.Generator;

/**
 * Picks a spawn point by asking the generator's height function.
 * <p>
 * Loads no chunks, since that function is pure maths, which is what lets it check hundreds of
 * candidates in the time a single chunk load would take. The cleanroom generator it replaces loaded
 * chunk (0, 0) on the main thread during world creation just to find one column.
 */
public final class SpawnFinder
{
    /** How far out to search before giving up, in blocks. */
    private static final int RANGE = 512;

    /** Distance between candidates. Fine enough to find a coast, coarse enough to stay cheap. */
    private static final int STEP = 16;

    private final GenerationProfile profile;
    private final Generator generator;

    public SpawnFinder(final GenerationProfile profile, final Generator generator)
    {
        this.profile = profile;
        this.generator = generator;
    }

    /**
     * Spirals out from the origin for the first column that is above sea level and inside the
     * world's bounds.
     * <p>
     * Falls back to the origin at sea level if nothing qualifies, which happens for a world that is
     * entirely ocean or entirely void.
     */
    public Location findSpawn(final World world)
    {
        final int floor = this.profile.bounds().seaLevel().orElse(this.profile.bounds().minY());

        for (int ring = 0; ring <= RANGE / STEP; ring++)
        {
            final Optional<Location> found = searchRing(world, ring, floor);

            if (found.isPresent())
                return found.get();
        }

        return new Location(world, 0.5D, floor + 1, 0.5D);
    }

    /**
     * Walks the edge of one square ring at this radius.
     * <p>
     * Squares rather than circles because the point is to spread outward evenly, and a square ring
     * is a single loop with no trigonometry. Ring zero is the origin itself.
     */
    private Optional<Location> searchRing(final World world, final int ring, final int floor)
    {
        final int extent = ring * STEP;

        if (ring == 0)
            return candidate(world, 0, 0, floor);

        for (int offset = -extent; offset <= extent; offset += STEP)
        {
            final Optional<Location> north = candidate(world, offset, -extent, floor);

            if (north.isPresent())
                return north;

            final Optional<Location> south = candidate(world, offset, extent, floor);

            if (south.isPresent())
                return south;

            final Optional<Location> west = candidate(world, -extent, offset, floor);

            if (west.isPresent())
                return west;

            final Optional<Location> east = candidate(world, extent, offset, floor);

            if (east.isPresent())
                return east;
        }

        return Optional.empty();
    }

    /** A column qualifies if its ground sits above the water line and below the world's ceiling. */
    private Optional<Location> candidate(final World world, final int x, final int z, final int floor)
    {
        final int height = this.generator.surfaceHeight(x, z);

        if (height <= floor || height >= this.profile.bounds().maxY())
            return Optional.empty();

        // Centred in the block and one above the ground, so the player is not standing inside it.
        return Optional.of(new Location(world, x + 0.5D, height + 1, z + 0.5D));
    }
}
