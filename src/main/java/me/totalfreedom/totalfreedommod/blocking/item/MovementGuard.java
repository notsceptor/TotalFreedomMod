package me.totalfreedom.totalfreedommod.blocking.item;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains player movement based checks based upon contents of their inbound packets.  Further movement
 * checks can be added here rather than a sibling.  This implementation runs on packetevents netty threads
 * with a caller that is responsible for cancelling the action on the main thread.
 */
final class MovementGuard
{

    private final ConcurrentHashMap<UUID, State> states = new ConcurrentHashMap<>();

    private final double maxHorizontalDeltaSq;
    private final int maxOversizedPerWindow;

    MovementGuard(int maxHorizontalDelta, int maxOversizedPerWindow)
    {
        this.maxHorizontalDeltaSq = (double) maxHorizontalDelta * (double) maxHorizontalDelta;
        this.maxOversizedPerWindow = maxOversizedPerWindow;
    }

    // Obtains the latest position from a movement packet.
    Decision recordAndCheck(UUID id, double x, double z)
    {
        if (maxOversizedPerWindow <= 0 || id == null)
        {
            return Decision.ALLOW;
        }

        final State state = states.computeIfAbsent(id, k -> new State());
        synchronized (state)
        {
            if (!state.hasLast)
            {
                state.lastX = x;
                state.lastZ = z;
                state.hasLast = true;
                return Decision.ALLOW;
            }

            final double dx = x - state.lastX;
            final double dz = z - state.lastZ;
            state.lastX = x;
            state.lastZ = z;

            if (dx * dx + dz * dz <= maxHorizontalDeltaSq)
            {
                return Decision.ALLOW;
            }

            // Oversized horizontal move.
            if (state.flagged)
            {
                return Decision.BLOCK;
            }

            final long second = System.currentTimeMillis() / 1000L;
            if (state.windowSecond != second)
            {
                state.windowSecond = second;
                state.oversizedCount = 0;
            }
            state.oversizedCount++;

            if (state.oversizedCount >= maxOversizedPerWindow)
            {
                state.flagged = true;
                return Decision.PUNISH;
            }

            return Decision.ALLOW;
        }
    }

    void forget(UUID id)
    {
        if (id != null)
        {
            states.remove(id);
        }
    }

    void clear()
    {
        states.clear();
    }

    enum Decision
    {
        ALLOW,
        BLOCK,
        PUNISH
    }

    private static final class State
    {
        private boolean hasLast;
        private double lastX;
        private double lastZ;
        private long windowSecond;
        private int oversizedCount;
        private boolean flagged;
    }
}
