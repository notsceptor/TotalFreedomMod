package me.totalfreedom.totalfreedommod.world;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.framework.PluginComponent;
import me.totalfreedom.totalfreedommod.util.FLog;

/**
 * Base for a world TFM creates and manages itself. Caches the {@link World} once generated and
 * rebuilds it if Bukkit ever drops it from {@link Bukkit#getWorlds()}.
 * <p>
 * A welcome sign is planted at whatever the world's own spawn location turns out to be, so a
 * subclass is free to pick that location however it likes.
 */
public abstract class CustomWorld extends PluginComponent<TotalFreedomMod>
{
    private final String name;
    private final String displayName;
    //
    private World world;

    public CustomWorld(TotalFreedomMod plugin, String name, String displayName)
    {
        super(plugin);
        this.name = name;
        this.displayName = displayName;
    }

    public CustomWorld(TotalFreedomMod plugin, String name)
    {
        this(plugin, name, name);
    }

    public final String getName()
    {
        return this.name;
    }

    public final String getDisplayName()
    {
        return this.displayName;
    }

    public final World getWorld()
    {
        if (world != null && Bukkit.getWorlds().contains(world))
        {
            return world;
        }

        world = generateWorld();

        if (world == null)
        {
            FLog.warning("Could not load world: " + name);
            return null;
        }

        placeWelcomeSign(world);
        plugin.gr.enforceGameRuleDefaultsForWorld(world);

        return world;
    }

    private void placeWelcomeSign(final World world)
    {
        final Block welcomeSignBlock = world.getSpawnLocation().getBlock();
        welcomeSignBlock.setType(Material.OAK_SIGN);

        final org.bukkit.block.data.type.Sign signData = (org.bukkit.block.data.type.Sign) Material.OAK_SIGN.createBlockData();
        signData.setRotation(BlockFace.NORTH);
        welcomeSignBlock.setBlockData(signData);

        final Sign welcomeSign = (Sign) welcomeSignBlock.getState();

        final Component[] lines = {
                Component.text(this.displayName, NamedTextColor.GREEN),
                Component.text("---", NamedTextColor.DARK_GRAY),
                Component.text("Spawn Point", NamedTextColor.YELLOW),
                Component.text("---", NamedTextColor.DARK_GRAY)
        };

        final SignSide front = welcomeSign.getSide(Side.FRONT);
        final SignSide back = welcomeSign.getSide(Side.BACK);

        for (int i = 0; i < lines.length; i++)
        {
            front.line(i, lines[i]);
            back.line(i, lines[i]);
        }

        welcomeSign.update();
    }

    public void sendToWorld(Player player)
    {
        try
        {
            player.teleport(getWorld().getSpawnLocation());
        }
        catch (Exception ex)
        {
            player.sendMessage(ex.getMessage());
        }
    }

    protected abstract World generateWorld();
}
