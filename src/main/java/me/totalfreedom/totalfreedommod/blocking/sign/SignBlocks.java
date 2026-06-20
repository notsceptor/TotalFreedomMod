package me.totalfreedom.totalfreedommod.blocking.sign;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

/**
 * Shared sign detection helpers for placement blocking and validation.
 */
public final class SignBlocks
{

    private SignBlocks()
    {
    }

    public static boolean isSignBlock(Block block)
    {
        if (block == null)
        {
            return false;
        }
        BlockState state = block.getState();
        return state instanceof Sign;
    }

    public static boolean isSignMaterial(Material material)
    {
        if (material == null)
        {
            return false;
        }
        String name = material.name();
        return name.endsWith("_SIGN") || name.endsWith("_WALL_SIGN") || name.contains("HANGING_SIGN");
    }
}
