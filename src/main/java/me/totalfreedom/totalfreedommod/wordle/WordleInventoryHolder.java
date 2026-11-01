package me.totalfreedom.totalfreedommod.wordle;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.inventory.InventoryHolder;

public interface WordleInventoryHolder extends InventoryHolder
{
    Set<UUID> ACTIVE_ANVILS = ConcurrentHashMap.newKeySet();

    static void markAnvilOpen(final UUID playerId)
    {
        ACTIVE_ANVILS.add(playerId);
    }

    static boolean hasAnvilOpen(final UUID playerId)
    {
        return ACTIVE_ANVILS.contains(playerId);
    }

    static void clearAnvil(final UUID playerId)
    {
        ACTIVE_ANVILS.remove(playerId);
    }
}
