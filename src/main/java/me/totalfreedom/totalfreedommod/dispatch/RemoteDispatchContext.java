package me.totalfreedom.totalfreedommod.dispatch;

import org.bukkit.Bukkit;

/**
 * Carries a {@link RemoteDispatchSession} across the dispatch of a command
 * through the real {@link Bukkit#getConsoleSender() console sender}.  Session is kept on a local thread.
 */
public final class RemoteDispatchContext
{

    private static final ThreadLocal<RemoteDispatchSession> ACTIVE = new ThreadLocal<>();

    private RemoteDispatchContext()
    {
    }

    public static RemoteDispatchSession getActiveSession()
    {
        return ACTIVE.get();
    }

    public static boolean isActive()
    {
        return ACTIVE.get() != null;
    }

    public static void runWithSession(RemoteDispatchSession session, Runnable task)
    {
        if (session == null)
        {
            task.run();
            return;
        }

        ACTIVE.set(session);
        try
        {
            task.run();
        }
        finally
        {
            ACTIVE.remove();
        }
    }

    /**
     * {@code command} on the main thread's current tick. When {@code session}
     * is non-null, commands see it via {@link #getActiveSession()}.
     */
    public static void dispatch(RemoteDispatchSession session, String command)
    {
        runWithSession(session, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }
}
