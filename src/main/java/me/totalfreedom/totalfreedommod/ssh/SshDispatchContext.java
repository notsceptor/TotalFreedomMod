package me.totalfreedom.totalfreedommod.ssh;

import org.bukkit.Bukkit;

/**
 * SSH session context used while dispatching a command through the
 * real {@link Bukkit#getConsoleSender() console sender}.
 *
 * Paper rejects custom {@link org.bukkit.command.CommandSender} wrappers in
 * {@code Bukkit.dispatchCommand()} so this context is used instead of the dispatch sender.
 */
public final class SshDispatchContext
{

    private static final ThreadLocal<SshSession> ACTIVE = new ThreadLocal<>();

    private SshDispatchContext()
    {
    }

    public static SshSession getActiveSession()
    {
        return ACTIVE.get();
    }

    public static boolean isActive()
    {
        return ACTIVE.get() != null;
    }

    public static void runWithSession(SshSession session, Runnable task)
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
     * Dispatch a command on the main thread's current tick. When {@code session}
     * is non-null, checks see the session via {@link #getActiveSession()}.
     */
    public static void dispatch(SshSession session, String command)
    {
        runWithSession(session, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }
}
