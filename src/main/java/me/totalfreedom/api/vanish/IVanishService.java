package me.totalfreedom.api.vanish;

import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerListPingEvent;

public interface IVanishService
{
    boolean isVanished(Player player);

    boolean isVanished(UUID uuid);

    /**
     * Whether {@code viewer} is allowed to know {@code target} is online. Console and other
     * non-player senders always can, as can a vanished player looking at themselves.
     */
    boolean canSee(CommandSender viewer, Player target);

    List<Player> visiblePlayersFor(CommandSender viewer);

    void vanish(Player player);

    void unvanish(Player player);

    boolean toggle(Player player);

    /**
     * Removes vanished players from the server list ping sample and count.
     */
    void filterServerPing(ServerListPingEvent event);
}
