package me.totalfreedom.totalfreedommod.cmd.internal;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager
{
    private static final ConcurrentHashMap<String, Long> expiryByKey = new ConcurrentHashMap<>();

    private CooldownManager() {}

    public static boolean isOnCooldown(UUID playerId, String key)
    {
        String mapKey = mapKey(playerId, key);
        Long expiry = expiryByKey.get(mapKey);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry)
        {
            expiryByKey.remove(mapKey);
            return false;
        }
        return true;
    }

    public static long remainingMillis(UUID playerId, String key)
    {
        Long expiry = expiryByKey.get(mapKey(playerId, key));
        if (expiry == null) return 0L;
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    public static void setCooldown(UUID playerId, String key, long durationMillis)
    {
        expiryByKey.put(mapKey(playerId, key), System.currentTimeMillis() + durationMillis);
    }

    public static void clearAll()
    {
        expiryByKey.clear();
    }

    private static String mapKey(UUID playerId, String commandKey)
    {
        return playerId + ":" + commandKey;
    }
}
