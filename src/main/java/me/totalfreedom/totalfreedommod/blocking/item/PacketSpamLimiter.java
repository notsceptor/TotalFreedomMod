package me.totalfreedom.totalfreedommod.blocking.item;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class PacketSpamLimiter
{

    private final ConcurrentHashMap<UUID, Window> interactions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Window> chats = new ConcurrentHashMap<>();

    private final int maxInteractionsPerSecond;
    private final int maxChatsPerSecond;

    PacketSpamLimiter(int maxInteractionsPerSecond, int maxChatsPerSecond)
    {
        this.maxInteractionsPerSecond = maxInteractionsPerSecond;
        this.maxChatsPerSecond = maxChatsPerSecond;
    }

    boolean allowInteraction(UUID id)
    {
        return allow(interactions, id, maxInteractionsPerSecond);
    }

    boolean allowChat(UUID id)
    {
        return allow(chats, id, maxChatsPerSecond);
    }

    private static boolean allow(ConcurrentHashMap<UUID, Window> map, UUID id, int limit)
    {
        if (limit <= 0 || id == null)
        {
            return true;
        }
        long second = System.currentTimeMillis() / 1000L;
        Window window = map.computeIfAbsent(id, k -> new Window());
        synchronized (window)
        {
            if (window.second != second)
            {
                window.second = second;
                window.count = 0;
            }
            window.count++;
            return window.count <= limit;
        }
    }

    void forget(UUID id)
    {
        interactions.remove(id);
        chats.remove(id);
    }

    void clear()
    {
        interactions.clear();
        chats.clear();
    }

    private static final class Window
    {
        private long second;
        private int count;
    }
}
