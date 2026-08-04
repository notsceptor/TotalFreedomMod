package me.totalfreedom.totalfreedommod.discord;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FTask;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.CloseCode;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps one reconnect budget for the bridge and decides when to stop spending it.
 * <p>
 * JDA reconnects a dropped gateway on its own for close codes it considers recoverable. This
 * supervisor covers what that leaves: the codes JDA gives up on, a client that shut itself down,
 * a connect attempt that never completed, and REST work rejected because the client underneath it
 * is gone. All of those funnel into one counter.
 * <p>
 * Attempts are spaced by {@code discord.reconnect.interval_seconds} and capped at
 * {@code discord.reconnect.max_attempts} <em>consecutive</em> failures. Any successful connection
 * resets the count, so a server that drops once an hour retries forever, while one that cannot
 * reach Discord at all stops after the cap and stays down until restarted rather than reconnecting
 * in a loop against a token or an outage that is not going to recover on its own.
 */
public final class DiscordConnectionSupervisor extends ListenerAdapter
{
    private static final int MIN_INTERVAL_SECONDS = 5;

    private final TotalFreedomMod plugin;
    private final Runnable attemptConnect;
    private final Runnable onGiveUp;
    private final int maxAttempts;
    private final long retryTicks;
    private final int retrySeconds;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicBoolean retryPending = new AtomicBoolean();
    private final AtomicBoolean givenUp = new AtomicBoolean();
    private final AtomicBoolean stopping = new AtomicBoolean();

    /**
     * @param attemptConnect performs exactly one connection attempt; runs off the main thread and
     *                       is expected to call back into {@link #reportConnected()} or
     *                       {@link #reportFailure(String, Throwable)}
     * @param onGiveUp       tears the bridge down for good once the budget is spent
     */
    public DiscordConnectionSupervisor(final TotalFreedomMod plugin, final Runnable attemptConnect,
            final Runnable onGiveUp)
    {
        this.plugin = plugin;
        this.attemptConnect = attemptConnect;
        this.onGiveUp = onGiveUp;
        this.maxAttempts = Math.max(1, ConfigEntry.DISCORD_RECONNECT_MAX_ATTEMPTS.getInteger(5));
        this.retrySeconds = Math.max(MIN_INTERVAL_SECONDS,
                ConfigEntry.DISCORD_RECONNECT_INTERVAL_SECONDS.getInteger(30));
        this.retryTicks = retrySeconds * 20L;
    }

    /**
     * Turn a connection failure into the short reason string used in logs, naming the two cases
     * worth telling apart: work rejected because the client is already gone, and an attempt that
     * simply never completed.
     */
    public static String describeFailure(final Throwable thrown)
    {
        if (thrown == null)
            return "unknown failure";

        if (thrown instanceof RejectedExecutionException)
            return "rejected (client is shut down)";

        if (thrown instanceof TimeoutException)
            return "timed out";

        if (thrown instanceof InterruptedException)
            return "interrupted";

        final String message = thrown.getMessage();
        return message == null || message.isBlank()
                ? thrown.getClass().getSimpleName()
                : String.format("%s: %s", thrown.getClass().getSimpleName(), message);
    }

    /**
     * A connection came up. Clears the budget so an unrelated drop later gets a full set of
     * attempts of its own.
     */
    public void reportConnected()
    {
        final int spent = consecutiveFailures.getAndSet(0);
        retryPending.set(false);

        if (spent > 0)
            FLog.info(String.format("[Discord] Reconnected after %d failed attempt(s).", spent));
    }

    /**
     * An attempt failed, or a live connection dropped. Queues the next attempt, or gives up if
     * this failure spent the last of the budget.
     *
     * @param reason already-formatted description, see {@link #describeFailure(Throwable)}
     */
    public void reportFailure(final String reason)
    {
        if (stopping.get() || givenUp.get())
            return;

        final int attempt = consecutiveFailures.incrementAndGet();
        if (attempt >= maxAttempts)
        {
            giveUp(reason, attempt);
            return;
        }

        FLog.warning(String.format("[Discord] Connection failure %d/%d (%s); retrying in %ds.",
                attempt, maxAttempts, reason, retrySeconds));
        scheduleRetry();
    }

    /**
     * Whether the budget is spent. The bridge stays down for the rest of this run once true.
     */
    public boolean hasGivenUp()
    {
        return givenUp.get();
    }

    /**
     * Mark the coming disconnect as deliberate, so the shutdown it produces is not mistaken for a
     * dropped connection and does not spend an attempt.
     */
    public void beginIntentionalShutdown()
    {
        stopping.set(true);
    }

    /**
     * Fires when JDA has fully stopped. Anything reaching here that we did not ask for is a
     * connection we lost, including the close codes JDA declines to reconnect from.
     */
    @Override
    public void onShutdown(@NotNull final ShutdownEvent event)
    {
        if (stopping.get() || givenUp.get())
            return;

        final CloseCode closeCode = event.getCloseCode();
        reportFailure(closeCode == null
                ? "gateway closed without a close code"
                : String.format("gateway closed with %s (%d)", closeCode.name(), closeCode.getCode()));
    }

    private void scheduleRetry()
    {
        // One attempt in flight at a time: a drop can be reported by the gateway thread and by a
        // failing send at the same moment, and both would otherwise queue their own retry.
        if (!retryPending.compareAndSet(false, true))
            return;

        if (!plugin.isEnabled())
        {
            retryPending.set(false);
            return;
        }

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, FTask.guard("DiscordConnectionSupervisor/retry", () ->
        {
            retryPending.set(false);

            if (stopping.get() || givenUp.get())
                return;

            attemptConnect.run();
        }), retryTicks);
    }

    private void giveUp(final String reason, final int attempt)
    {
        if (!givenUp.compareAndSet(false, true))
            return;

        FLog.severe(String.format("[Discord] Giving up after %d consecutive failed connection attempts %ds apart. "
                + "Last failure: %s. The bridge stays down until the server is restarted; the rest of the plugin is "
                + "unaffected.", attempt, retrySeconds, reason));
        onGiveUp.run();
    }
}
