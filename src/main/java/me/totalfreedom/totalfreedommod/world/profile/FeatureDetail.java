package me.totalfreedom.totalfreedommod.world.profile;

import org.bukkit.TreeType;
import org.bukkit.block.data.BlockData;

/**
 * What a feature actually places. One variant per kind, each carrying only the settings that kind
 * uses.
 * <p>
 * This is why there is no shared size field meaning vein length here and radius there. A boulder
 * has a radius, a patch has a spread, and a tree has neither.
 * <p>
 * Being sealed also means the populator's switch over these is checked for exhaustiveness, so
 * adding a variant will not compile until something knows how to place it.
 */
public sealed interface FeatureDetail
{
    /** A vein buried in the filler block. size is how many blocks the vein is. */
    record Ore(BlockData block, int size) implements FeatureDetail
    {
        @Override
        public Anchor anchor()
        {
            return Anchor.RANGE;
        }
    }

    /** A scatter across the surface. spread is how far from the origin it reaches. */
    record Patch(BlockData block, int spread) implements FeatureDetail
    {
        @Override
        public Anchor anchor()
        {
            return Anchor.SURFACE;
        }
    }

    /** A hollowed bowl filled with fluid. */
    record Lake(BlockData fluid, int radius) implements FeatureDetail
    {
        @Override
        public Anchor anchor()
        {
            return Anchor.RANGE;
        }
    }

    /** A rough blob resting on the ground. */
    record Boulder(BlockData block, int radius) implements FeatureDetail
    {
        @Override
        public Anchor anchor()
        {
            return Anchor.SURFACE;
        }
    }

    /**
     * A tree, named exactly. A sapling would not be enough, since spruce alone covers REDWOOD,
     * TALL_REDWOOD, and MEGA_REDWOOD.
     * <p>
     * For a vanilla-style mix, write one entry per variant and let rarity do the weighting: nine
     * REDWOOD to one MEGA_REDWOOD.
     */
    record Tree(TreeType type) implements FeatureDetail
    {
        @Override
        public Anchor anchor()
        {
            return Anchor.SURFACE;
        }
    }

    Anchor anchor();
}
