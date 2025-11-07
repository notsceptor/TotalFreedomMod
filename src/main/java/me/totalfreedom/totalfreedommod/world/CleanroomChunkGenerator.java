/*
 * Cleanroom Generator
 * Copyright (C) 2011-2012 nvx
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.totalfreedom.totalfreedommod.world;

import static java.lang.System.arraycopy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import me.totalfreedom.totalfreedommod.util.MaterialHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.generator.WorldInfo;

public class CleanroomChunkGenerator extends ChunkGenerator
{

    private static final Logger log = Bukkit.getLogger();
    private Material[] layer;
    private byte[] layerDataValues;

    public CleanroomChunkGenerator()
    {
        this("64,stone");
    }

    public CleanroomChunkGenerator(String id)
    {
        if (id != null)
        {
            try
            {
                int y = 0;

                layer = new Material[128]; // Default to 128, will be resized later if required
                layerDataValues = null;

                if ((id.length() > 0) && (id.charAt(0) == '.')) // Is the first character a '.'? If so, skip bedrock generation.
                {
                    id = id.substring(1); // Skip bedrock then and remove the .
                }
                else // Guess not, bedrock at layer0 it is then.
                {
                    layer[y++] = Material.BEDROCK;
                }

                if (id.length() > 0)
                {
                    String tokens[] = id.split("[,]");

                    if ((tokens.length % 2) != 0)
                    {
                        throw new Exception();
                    }

                    for (int i = 0; i < tokens.length; i += 2)
                    {
                        int height = Integer.parseInt(tokens[i]);
                        if (height <= 0)
                        {
                            log.warning("[CleanroomGenerator] Invalid height '" + tokens[i] + "'. Using 64 instead.");
                            height = 64;
                        }

                        String materialTokens[] = tokens[i + 1].split("[:]", 2);
                        byte dataValue = 0;
                        if (materialTokens.length == 2)
                        {
                            try
                            {
                                // Lets try to read the data value
                                dataValue = Byte.parseByte(materialTokens[1]);
                            }
                            catch (Exception e)
                            {
                                log.warning("[CleanroomGenerator] Invalid Data Value '" + materialTokens[1] + "'. Defaulting to 0.");
                                dataValue = 0;
                            }
                        }
                        // Use MaterialHelper to avoid triggering legacy material support
                        Material mat = MaterialHelper.getMaterial(materialTokens[0]);
                        if (mat == null)
                        {
                            // Numeric IDs no longer exist in 1.13+, try legacy name mapping
                            try
                            {
                                // Try to map old numeric ID to material name (for legacy configs)
                                int oldId = Integer.parseInt(materialTokens[0]);
                                // This is a simplified mapping - in practice, you'd need a full mapping table
                                // For now, just default to stone
                                log.warning("[CleanroomGenerator] Numeric block IDs are no longer supported. Use material names instead. Defaulting to stone.");
                                mat = Material.STONE;
                            }
                            catch (Exception e)
                            {
                                // Not a number, default to stone
                                log.warning("[CleanroomGenerator] Invalid Block ID '" + materialTokens[0] + "'. Defaulting to stone.");
                                mat = Material.STONE;
                            }
                        }

                        if (!mat.isBlock())
                        {
                            log.warning("[CleanroomGenerator] Error, '" + materialTokens[0] + "' is not a block. Defaulting to stone.");
                            mat = Material.STONE;
                        }

                        if (y + height > layer.length)
                        {
                            Material[] newLayer = new Material[Math.max(y + height, layer.length * 2)];
                            arraycopy(layer, 0, newLayer, 0, y);
                            layer = newLayer;
                            if (layerDataValues != null)
                            {
                                byte[] newLayerDataValues = new byte[Math.max(y + height, layerDataValues.length * 2)];
                                arraycopy(layerDataValues, 0, newLayerDataValues, 0, y);
                                layerDataValues = newLayerDataValues;
                            }
                        }

                        Arrays.fill(layer, y, y + height, mat);
                        if (dataValue != 0)
                        {
                            if (layerDataValues == null)
                            {
                                layerDataValues = new byte[layer.length];
                            }
                            Arrays.fill(layerDataValues, y, y + height, dataValue);
                        }
                        y += height;
                    }
                }

                // Trim to size
                if (layer.length > y)
                {
                    Material[] newLayer = new Material[y];
                    arraycopy(layer, 0, newLayer, 0, y);
                    layer = newLayer;
                }
                if (layerDataValues != null && layerDataValues.length > y)
                {
                    byte[] newLayerDataValues = new byte[y];
                    arraycopy(layerDataValues, 0, newLayerDataValues, 0, y);
                    layerDataValues = newLayerDataValues;
                }
            }
            catch (Exception e)
            {
                log.severe("[CleanroomGenerator] Error parsing CleanroomGenerator ID '" + id + "'. using defaults '64,stone': " + e.toString());
                e.printStackTrace();
                layerDataValues = null;
                layer = new Material[65];
                layer[0] = Material.BEDROCK;
                Arrays.fill(layer, 1, 65, Material.STONE);
            }
        }
        else
        {
            layerDataValues = null;
            layer = new Material[65];
            layer[0] = Material.BEDROCK;
            Arrays.fill(layer, 1, 65, Material.STONE);
        }
    }

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData)
    {
        int maxHeight = worldInfo.getMaxHeight();
        if (layer.length > maxHeight)
        {
            log.warning("[CleanroomGenerator] Error, chunk height " + layer.length + " is greater than the world max height (" + maxHeight + "). Trimming to world max height.");
            Material[] newLayer = new Material[maxHeight];
            arraycopy(layer, 0, newLayer, 0, maxHeight);
            layer = newLayer;
        }
        
        // Fill chunk with materials from layer array
        for (int y = 0; y < Math.min(layer.length, maxHeight); y++)
        {
            if (layer[y] != null && layer[y].isBlock())
            {
                Material material = layer[y];
                for (int xx = 0; xx < 16; xx++)
            {
                    for (int zz = 0; zz < 16; zz++)
                    {
                        chunkData.setBlock(xx, y, zz, material);
            }
        }
            }
        }
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world)
    {
        if (layerDataValues != null)
        {
            return Arrays.asList((BlockPopulator) new CleanroomBlockPopulator(layer, layerDataValues));
        }
        else
        {
            // This is the default, but just in case default populators change to stock minecraft populators by default...
            return new ArrayList<>();
        }
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random)
    {
        if (!world.isChunkLoaded(0, 0))
        {
            world.loadChunk(0, 0);
        }

        if ((world.getHighestBlockYAt(0, 0) <= 0) && (world.getBlockAt(0, 0, 0).getType() == Material.AIR)) // SPACE!
        {
            return new Location(world, 0, 64, 0); // Lets allow people to drop a little before hitting the void then shall we?
        }

        return new Location(world, 0, world.getHighestBlockYAt(0, 0), 0);
    }
}
