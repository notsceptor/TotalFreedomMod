package me.totalfreedom.totalfreedommod.world.profile;

import org.bukkit.block.data.BlockData;

/**
 * Block layers for a flat world, bottom to top, starting at the world's minY.
 * <p>
 * Takes the same syntax as flatlands.generate_params in the config.
 * <p>
 * A class rather than a record because the layers are arrays. A record would hand out its backing
 * arrays through the generated accessors, and anything holding the profile could then rewrite a
 * compiled world's layers in place.
 */
public final class LayerStack
{
    private final BlockData[] blocks;
    private final int[] heights;

    private LayerStack(final BlockData[] blocks, final int[] heights)
    {
        this.blocks = blocks;
        this.heights = heights;
    }

    /**
     * @param spec e.g. {@code "16|stone|32|dirt|1|grass_block"}; the legacy comma form also works
     * @throws IllegalArgumentException if the spec is malformed, names an unknown block, or gives a
     *                                  height below one
     */
    public static LayerStack parse(final String spec)
    {

    }

    /** How many layers there are, bottom to top. */
    public int size()
    {

    }

    /** @throws IndexOutOfBoundsException if the layer does not exist */
    public BlockData blockAt(final int layer)
    {

    }

    /** @throws IndexOutOfBoundsException if the layer does not exist */
    public int heightAt(final int layer)
    {

    }

    /** Total height of every layer combined. */
    public int totalHeight()
    {

    }
}
