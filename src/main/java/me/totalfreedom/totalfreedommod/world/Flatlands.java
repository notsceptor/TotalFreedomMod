package me.totalfreedom.totalfreedommod.world;

import java.io.File;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

@SuppressWarnings("deprecation")
public class Flatlands extends CustomWorld
{

    private static final String GENERATION_PARAMETERS = ConfigEntry.FLATLANDS_GENERATE_PARAMS.getString();

    public Flatlands(TotalFreedomMod plugin)
    {
        super(plugin, "flatlands");
    }

    @Override
    protected World generateWorld()
    {
        if (!ConfigEntry.FLATLANDS_GENERATE.getBoolean())
        {
            return null;
        }

        wipeFlatlandsIfFlagged();

        final WorldCreator worldCreator = new WorldCreator(getName());
        worldCreator.generateStructures(false);
        worldCreator.type(WorldType.NORMAL);
        worldCreator.environment(World.Environment.NORMAL);
        worldCreator.generator(new CleanroomChunkGenerator(GENERATION_PARAMETERS));

        final World world = Bukkit.getServer().createWorld(worldCreator);

        world.setSpawnFlags(false, false);
        world.setSpawnLocation(0, 50, 0);

        final Block welcomeSignBlock = world.getBlockAt(0, 50, 0);
        welcomeSignBlock.setType(Material.OAK_SIGN);
        // Use BlockData API instead of deprecated MaterialData
        org.bukkit.block.data.type.Sign signData = (org.bukkit.block.data.type.Sign) Material.OAK_SIGN.createBlockData();
        signData.setRotation(BlockFace.NORTH);
        welcomeSignBlock.setBlockData(signData);
        
        org.bukkit.block.Sign welcomeSign = (org.bukkit.block.Sign) welcomeSignBlock.getState();

        String[] lines = {
            "\u00A7aFlatlands",
            "\u00A78---",
            "\u00A7eSpawn Point",
            "\u00A78---"
        };

        org.bukkit.block.sign.SignSide front = welcomeSign.getSide(org.bukkit.block.sign.Side.FRONT);
        org.bukkit.block.sign.SignSide back = welcomeSign.getSide(org.bukkit.block.sign.Side.BACK);

        for (int i = 0; i < lines.length; i++) {
            front.setLine(i, lines[i]);
            back.setLine(i, lines[i]);
        }

        welcomeSign.update();

        plugin.gr.commitGameRules();

        return world;
    }

    public void wipeFlatlandsIfFlagged()
    {
        boolean doFlatlandsWipe = false;
        try
        {
            doFlatlandsWipe = plugin.sf.getSavedFlag("do_wipe_flatlands");
        }
        catch (Exception ex)
        {
        }

        if (doFlatlandsWipe)
        {
            if (Bukkit.getServer().getWorld("flatlands") == null)
            {
                FLog.info("Wiping flatlands.");
                plugin.sf.setSavedFlag("do_wipe_flatlands", false);
                FileUtils.deleteQuietly(new File("./flatlands"));
            }
            else
            {
                FLog.severe("Can't wipe flatlands, it is already loaded.");
            }
        }
    }

}
