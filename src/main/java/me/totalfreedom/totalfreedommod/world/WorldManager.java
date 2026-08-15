package me.totalfreedom.totalfreedommod.world;

import java.io.File;

import me.totalfreedom.api.FreedomAPI;
import me.totalfreedom.api.world.IWorldManager;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import net.kyori.adventure.text.format.NamedTextColor;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.SavedFlags;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;

import static me.totalfreedom.totalfreedommod.util.FUtil.playerMsg;

/**
 * Creates and tracks the two worlds TFM manages itself, flatlands and the AdminWorld, both plain
 * profile-driven {@link GeneratedWorld}s sourced from {@link GenerationService}. The AdminWorld's
 * access control lives in its own {@link WorldAccessGate}, wired up here rather than owned by this
 * class, so nothing about "AdminWorld" specifically is hardcoded into world management.
 */
public class WorldManager extends FreedomService implements IWorldManager
{
    private static final String ADMIN_WORLD_PERMISSION = "tfm.world.adminworld";

    private GeneratedWorld flatlands;
    private GeneratedWorld adminworld;
    private WorldAccessGate adminGate;

    public WorldManager(FreedomAPI plugin)
    {
        super(plugin);
    }

    @Override
    public void onStart()
    {
        Bukkit.getScheduler().runTask(plugin, () ->
        {
            flatlands = buildWorld("flatlands", "Flatlands");
            adminworld = buildWorld("adminworld", "Admin World");

            if (flatlands != null && ConfigEntry.FLATLANDS_GENERATE.getBoolean())
            {
                wipeFlatlandsIfFlagged();
                flatlands.getWorld();
            }

            if (adminworld != null)
            {
                adminworld.getWorld();
                adminGate = new WorldAccessGate(plugin, adminworld, ADMIN_WORLD_PERMISSION);
            }

            // Disable weather
            if (ConfigEntry.DISABLE_WEATHER.getBoolean())
            {
                for (World world : server.getWorlds())
                {
                    world.setThundering(false);
                    world.setStorm(false);
                    world.setThunderDuration(0);
                    world.setWeatherDuration(0);
                }
            }
        });
    }

    @Override
    public void onStop()
    {
        if (flatlands != null)
        {
            World fl = Bukkit.getWorld(flatlands.getName());
            if (fl != null) fl.save();
        }

        if (adminworld != null)
        {
            World aw = Bukkit.getWorld(adminworld.getName());
            if (aw != null) aw.save();
        }
    }

    public GeneratedWorld flatlands()
    {
        return flatlands;
    }

    public GeneratedWorld adminworld()
    {
        return adminworld;
    }

    /** The AdminWorld's access gate, or null before onStart's deferred setup has run. */
    public WorldAccessGate adminGate()
    {
        return adminGate;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onThunderChange(ThunderChangeEvent event)
    {
        if (adminGate != null && adminGate.owns(event.getWorld()))
        {
            return; // The AdminWorld's own gate decides its weather.
        }

        if (ConfigEntry.DISABLE_WEATHER.getBoolean() && event.toThunderState())
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWeatherChange(WeatherChangeEvent event)
    {
        if (adminGate != null && adminGate.owns(event.getWorld()))
        {
            return; // The AdminWorld's own gate decides its weather.
        }

        if (ConfigEntry.DISABLE_WEATHER.getBoolean() && event.toWeatherState())
        {
            event.setCancelled(true);
        }
    }

    public void gotoWorld(Player player, String targetWorld)
    {
        if (player == null)
        {
            return;
        }

        if (player.getWorld().getName().equalsIgnoreCase(targetWorld))
        {
            playerMsg(player, "Going to main world.", NamedTextColor.GRAY);
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            return;
        }

        for (World world : Bukkit.getWorlds())
        {
            if (world.getName().equalsIgnoreCase(targetWorld))
            {
                playerMsg(player, "Going to world: " + targetWorld, NamedTextColor.GRAY);
                player.teleport(world.getSpawnLocation());
                return;
            }
        }

        playerMsg(player, "World " + targetWorld + " not found.", NamedTextColor.GRAY);
    }

    private GeneratedWorld buildWorld(final String name, final String displayName)
    {
        return plugin.generation()
                     .profile(name)
                     .map(profile -> new GeneratedWorld((TotalFreedomMod) plugin, profile, displayName))
                     .orElse(null);
    }

    /** Consumes the "wipe flatlands on next start" flag {@code /wipeflatlands} sets, if it's up. */
    private void wipeFlatlandsIfFlagged()
    {
        boolean doWipe;

        try
        {
            doWipe = plugin.services().require(SavedFlags.class).getSavedFlag("do_wipe_flatlands");
        }
        catch (Exception ex)
        {
            return;
        }

        if (!doWipe)
        {
            return;
        }

        if (Bukkit.getServer().getWorld("flatlands") != null)
        {
            FLog.error("Can't wipe flatlands, it is already loaded.");
            return;
        }

        FLog.info("Wiping flatlands.");
        plugin.services().require(SavedFlags.class).setSavedFlag("do_wipe_flatlands", false);
        FileUtils.deleteQuietly(new File("./flatlands"));
    }
}
