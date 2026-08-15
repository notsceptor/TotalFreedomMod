package me.totalfreedom.api.world;

import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Gates a world behind a permission node, with a guest list for letting specific players in
 * without granting them that permission, and its own weather/time overrides.
 */
public interface IWorldAccessGate
{
    boolean canAccess(Player player);

    /** @return false if guest is the supervisor themselves, guest is already an admin, or supervisor is not */
    boolean addGuest(Player guest, Player supervisor);

    boolean removeGuest(Player guest);

    Player removeGuest(String partialName);

    boolean hasGuests();

    String guestListToString();

    void purgeGuestList();

    void wipeAccessCache();

    WorldWeather getWeatherMode();

    void setWeatherMode(WorldWeather mode);

    WorldTime getTimeOfDay();

    void setTimeOfDay(WorldTime timeOfDay);

    /** Whether bukkitWorld is the one this gate manages, so an unrelated global handler can skip it. */
    boolean owns(World bukkitWorld);
}
