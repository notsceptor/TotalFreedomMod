package me.totalfreedom.totalfreedommod.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import me.totalfreedom.api.FreedomAPI;
import me.totalfreedom.api.world.IWorldAccessGate;
import me.totalfreedom.api.world.WorldTime;
import me.totalfreedom.api.world.WorldWeather;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.util.FLog;

/**
 * Gates a {@link CustomWorld} behind a permission node, with a guest list for letting specific
 * players in without granting them that permission, and its own weather/time overrides.
 * <p>
 * Not tied to any one world's identity or purpose; any {@link CustomWorld} can be wrapped in one of
 * these. Registers its own listeners on construction, so wiring a world up is a single constructor
 * call and nothing else needs to know a gate exists.
 */
public final class WorldAccessGate implements Listener, IWorldAccessGate
{
    private static final long ACCESS_CACHE_LIFETIME = 30L * 1000L; // 30 seconds
    private static final long TELEPORT_COOLDOWN = 500L; // 0.5 seconds

    private final FreedomAPI plugin;
    private final CustomWorld world;
    private final String permissionNode;
    private final Map<Player, Long> teleportCooldown = new HashMap<>();
    private final Map<Player, Boolean> accessCache = new HashMap<>();
    private final Map<Player, Player> guestList = new HashMap<>(); // Guest, Supervisor
    //
    private Long accessCacheClearedAt;
    private WorldWeather weather = WorldWeather.OFF;
    private WorldTime time = WorldTime.INHERIT;

    /**
     * @param permissionNode held by staff through their rank and by trusted players through a
     *                       title, which is why entry is a permission check rather than an admin
     *                       check
     */
    public WorldAccessGate(final FreedomAPI plugin, final CustomWorld world, final String permissionNode)
    {
        this.plugin = plugin;
        this.world = world;
        this.permissionNode = permissionNode;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public boolean canAccess(final Player player)
    {
        final long now = System.currentTimeMillis();

        if (accessCacheClearedAt == null || accessCacheClearedAt + ACCESS_CACHE_LIFETIME <= now)
        {
            accessCacheClearedAt = now;
            accessCache.clear();
        }

        final Boolean cached = accessCache.get(player);

        if (cached != null)
            return cached;

        boolean canAccess = plugin.ranks().hasPermission(player, permissionNode);

        if (!canAccess)
        {
            final Player supervisor = guestList.get(player);
            canAccess = supervisor != null && supervisor.isOnline() && plugin.admins().isAdmin(supervisor);

            if (!canAccess)
                guestList.remove(player);
        }

        accessCache.put(player, canAccess);
        return canAccess;
    }

    /** @return false if guest is the supervisor themselves, guest is already an admin, or supervisor is not */
    public boolean addGuest(final Player guest, final Player supervisor)
    {
        if (guest == supervisor || plugin.admins().isAdmin(guest))
            return false;

        if (!plugin.admins().isAdmin(supervisor))
            return false;

        guestList.put(guest, supervisor);
        wipeAccessCache();
        return true;
    }

    public boolean removeGuest(final Player guest)
    {
        final boolean removed = guestList.remove(guest) != null;

        if (removed)
            wipeAccessCache();

        return removed;
    }

    public Player removeGuest(String partialName)
    {
        partialName = partialName.toLowerCase();

        final Iterator<Player> it = guestList.keySet().iterator();

        while (it.hasNext())
        {
            final Player player = it.next();

            if (player.getName().toLowerCase().contains(partialName))
            {
                removeGuest(player);
                return player;
            }
        }

        return null;
    }

    public boolean hasGuests()
    {
        return !guestList.isEmpty();
    }

    public String guestListToString()
    {
        final List<String> output = new ArrayList<>();

        for (final Entry<Player, Player> entry : guestList.entrySet())
            output.add(entry.getKey().getName() + " (Supervisor: " + entry.getValue().getName() + ")");

        return String.join(", ", output);
    }

    public void purgeGuestList()
    {
        guestList.clear();
        wipeAccessCache();
    }

    public void wipeAccessCache()
    {
        accessCacheClearedAt = System.currentTimeMillis();
        accessCache.clear();
    }

    public WorldWeather getWeatherMode()
    {
        return weather;
    }

    public void setWeatherMode(final WorldWeather mode)
    {
        this.weather = mode;

        try
        {
            mode.setWorldToWeather(world.getWorld());
        }
        catch (Exception ignored)
        {
        }
    }

    public WorldTime getTimeOfDay()
    {
        return time;
    }

    public void setTimeOfDay(final WorldTime timeOfDay)
    {
        this.time = timeOfDay;

        try
        {
            timeOfDay.setWorldToTime(world.getWorld());
        }
        catch (Exception ignored)
        {
        }
    }

    /** Whether bukkitWorld is the one this gate manages, so an unrelated global handler can skip it. */
    public boolean owns(final World bukkitWorld)
    {
        final World gated = world.getWorld();

        return gated != null && gated.equals(bukkitWorld);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent event)
    {
        final Player player = event.getPlayer();
        final FPlayer fPlayer = plugin.players().getPlayer(player);

        if (!plugin.admins().isAdmin(player) && fPlayer.getFreezeData().isFrozen())
            return; // A frozen player's forced teleport is not an access attempt.

        validateMovement(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event)
    {
        if (!event.hasChangedPosition())
            return;

        validateMovement(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onThunderChange(final ThunderChangeEvent event)
    {
        if (!owns(event.getWorld()) || weather != WorldWeather.OFF)
            return;

        if (ConfigEntry.DISABLE_WEATHER.getBoolean() && event.toThunderState())
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWeatherChange(final WeatherChangeEvent event)
    {
        if (!owns(event.getWorld()) || weather != WorldWeather.OFF)
            return;

        if (ConfigEntry.DISABLE_WEATHER.getBoolean() && event.toWeatherState())
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        teleportCooldown.remove(event.getPlayer());
        accessCache.remove(event.getPlayer());
    }

    /** Bounces a player who cannot access the gated world back to the main world's spawn. */
    private void validateMovement(final PlayerMoveEvent event)
    {
        final World gated = world.getWorld();

        if (gated == null || !event.getTo().getWorld().equals(gated))
            return;

        final Player player = event.getPlayer();

        if (canAccess(player))
            return;

        final Long lastAttempt = teleportCooldown.get(player);
        final long now = System.currentTimeMillis();

        if (lastAttempt == null || lastAttempt + TELEPORT_COOLDOWN <= now)
        {
            teleportCooldown.put(player, now);
            FLog.info(player.getName() + " attempted to access " + world.getDisplayName() + ".");
            event.setTo(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
    }
}
