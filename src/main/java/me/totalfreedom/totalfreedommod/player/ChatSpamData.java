package me.totalfreedom.totalfreedommod.player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class ChatSpamData
{
    private static final int MAX_HISTORY = 32;

    public enum Verdict
    {
        OK,
        COOLDOWN,
        REPEAT
    }

    private record Entry(String signature, long time)
    {
    }

    private final Deque<Entry> recent = new ArrayDeque<>();
    private long lastAcceptTime = 0L;
    private long lastWarnTime = 0L;

    public synchronized Verdict evaluate(String signature, long now, long cooldownMs,
                                         long repeatWindowMs, int historySize)
    {
        if (cooldownMs > 0 && lastAcceptTime != 0L && now - lastAcceptTime < cooldownMs)
        {
            return Verdict.COOLDOWN;
        }

        final boolean hasSignature = signature != null && !signature.isEmpty();

        if (repeatWindowMs > 0 && hasSignature)
        {
            purgeExpired(now, repeatWindowMs);
            for (final Entry entry : recent)
            {
                if (entry.signature().equals(signature))
                {
                    return Verdict.REPEAT;
                }
            }
        }

        lastAcceptTime = now;
        if (hasSignature)
        {
            recent.addLast(new Entry(signature, now));
            final int cap = Math.min(Math.max(historySize, 1), MAX_HISTORY);
            while (recent.size() > cap)
            {
                recent.removeFirst();
            }
        }
        return Verdict.OK;
    }

    public synchronized boolean shouldWarn(long now, long intervalMs)
    {
        if (intervalMs <= 0 || now - lastWarnTime >= intervalMs)
        {
            lastWarnTime = now;
            return true;
        }
        return false;
    }

    private void purgeExpired(long now, long repeatWindowMs)
    {
        final Iterator<Entry> it = recent.iterator();
        while (it.hasNext())
        {
            if (now - it.next().time() > repeatWindowMs)
            {
                it.remove();
            }
            else
            {
                break;
            }
        }
    }
}
