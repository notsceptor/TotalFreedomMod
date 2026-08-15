package me.totalfreedom.api.economy;

import java.util.Collection;
import java.util.Optional;

import org.bukkit.entity.Player;

public interface IEcoManager
{
    IBank bank();

    /**
     * @return {@code player}'s {@link IEcoPlayer}, loading it from storage if necessary. May not return null.
     */
    IEcoPlayer getEcoPlayer(Player player);

    Optional<IEcoPlayer> getLoadedEcoPlayer(String username);

    /**
     * @return Every currently loaded (online) player's economy data.
     */
    Collection<IEcoPlayer> getLoadedPlayers();

    /**
     * Queue a write of every loaded player's balances, plus the bank's own.
     */
    void save();
}
