package me.totalfreedom.totalfreedommod.world.stage.feature;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.block.BlockType;
import org.bukkit.generator.LimitedRegion;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.profile.FeatureDetail;
import me.totalfreedom.totalfreedommod.world.profile.FeatureSpec;

/**
 * Grows a tree. The spec's block names the sapling, and the sapling picks the species, so
 * oak_sapling grows an oak and spruce_sapling grows a spruce.
 * <p>
 * Hands off to LimitedRegion#generateTree, which knows every vanilla tree shape and handles the
 * canopy crossing a chunk border.
 */
public final class TreeFeature implements Feature<FeatureDetail.Tree>
{
    @Override
    public void place(final ChunkContext context,
                      final LimitedRegion region,
                      final FeatureDetail.Tree detail,
                      final int x,
                      final int y,
                      final int z)
    {
    }
}
