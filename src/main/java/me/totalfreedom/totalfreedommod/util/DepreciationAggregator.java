package me.totalfreedom.totalfreedommod.util;

import java.util.HashSet;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class DepreciationAggregator
{

    public static Block getTargetBlock(LivingEntity entity, HashSet<Material> transparent, int maxDistance)
    {
        return entity.getTargetBlock(transparent, maxDistance);
    }

    public static OfflinePlayer getOfflinePlayer(Server server, String name)
    {
        return server.getOfflinePlayer(name);
    }

    /**
     * Material.getMaterial(int) was removed in 1.13+ as numeric IDs no longer exist.
     * This method always returns null for compatibility.
     */
    public static Material getMaterial(int id)
    {
        return null;
    }

    /**
     * Block data (byte data) was removed in 1.13+.
     * This method returns 0 for compatibility, but data should be stored as BlockData instead.
     */
    public static byte getData_Block(Block block)
    {
        // Block data no longer exists in 1.13+, return 0 for compatibility
        return 0;
    }

    /**
     * Block data (byte data) was removed in 1.13+.
     * This method attempts to set block data using BlockData API where possible.
     * For materials that support color/variant data, it will try to apply it.
     */
    public static void setData_Block(Block block, byte data)
    {
        // Block data no longer exists in 1.13+, use BlockData API instead
        Material material = block.getType();
        BlockData blockData = material.createBlockData();
        
        // Try to apply color/variant data for specific materials
        if (material == Material.WHITE_WOOL)
        {
            // Map old wool data values to new colored wool materials
            Material[] woolColors = {
                Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL, Material.LIGHT_BLUE_WOOL,
                Material.YELLOW_WOOL, Material.LIME_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL,
                Material.LIGHT_GRAY_WOOL, Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
                Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL, Material.BLACK_WOOL
            };
            
            if (data >= 0 && data < woolColors.length)
    {
                block.setType(woolColors[data]);
                return;
            }
        }
        
        // For other materials, just set the type (data is lost)
        block.setType(material);
    }

    /**
     * Block type IDs were removed in 1.13+.
     * This method returns -1 for compatibility.
     */
    public static int getTypeId_Block(Block block)
    {
        return -1;
    }

    public static String getName_EntityType(EntityType et)
    {
        return et.name();
    }
}
