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
    private final Runnable onConnectionLost;
    private final int maxAttempts;
    private final long retryTicks;
    private final int retrySeconds;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicBoolean retryPending = new AtomicBoolean();

    /**
     * Whether the connection the bridge currently holds has already had a failure counted against
     * it. One disconnect can be noticed by more than one party at once, and without this it would
     * cost an attempt per witness rather than per disconnect. Cleared as the next attempt begins,
     * which is what makes the budget count attempts rather than reports.
     */
    private final AtomicBoolean failureCounted = new AtomicBoolean();

    private final AtomicBoolean givenUp = new AtomicBoolean();
    private final AtomicBoolean stopping = new AtomicBoolean();

    /**
     * @param attemptConnect   performs exactly one connection attempt; runs off the main thread
     *                         and is expected to call back into {@link #reportConnected()} or
     *                         {@link #reportFailure(String)}
     * @param onConnectionLost releases whatever the previous attempt published or opened. Called
     *                         on every reported failure, not just the terminal one, so a retry
     *                         never runs against a stale session. Always invoked off the
     *                         reporting thread, so it is safe to block here.
     */
    public DiscordConnectionSupervisor(final TotalFreedomMod plugin, final Runnable attemptConnect,
            final Runnable onConnectionLost)
    {
        this.plugin = plugin;
        this.attemptConnect = attemptConnect;
        this.onConnectionLost = onConnectionLost;
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
        failureCounted.set(false);

        if (spent > 0)
            FLog.info(String.format("[Discord] Reconnected after %d failed attempt(s).", spent));
    }

    /**
     * An attempt failed, or a live connection dropped. Always hops onto an async task first: this
     * can be called from one of JDA's own threads (via {@link #onShutdown}) or the main thread
     * (via a relay's rejected-send report), and {@code onConnectionLost} can block for several
     * seconds releasing the old connection. Once on that task, releases the old connection, then
     * either queues the next attempt or gives up if this failure spent the last of the budget.
     * <p>
     * Only the first report against a given connection is acted on, so a single disconnect costs a
     * single attempt however many parties noticed it. The cases that overlap in practice are a
     * gateway close arriving while a relay send is already in flight against the same dead client,
     * and a connection that dies during the readiness wait, where the shutdown event and the throw
     * out of the connect attempt describe the same event.
     *
     * @param reason already-formatted description, see {@link #describeFailure(Throwable)}
     */
    public void reportFailure(final String reason)
    {
        if (stopping.get() || givenUp.get())
            return;

        if (!plugin.isEnabled())
            return;

        if (!failureCounted.compareAndSet(false, true))
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, FTask.guard("DiscordConnectionSupervisor/reportFailure", () ->
        {
            if (stopping.get() || givenUp.get())
                return;

            // Whatever connect() published, or was mid-publishing, is no longer usable. Release it
            // before deciding retry vs. give-up, so neither path ever runs, or leaves the bridge
            // parked, against a stale session or a still-HELD acquisition slot.
            onConnectionLost.run();

            final int attempt = consecutiveFailures.incrementAndGet();
            if (attempt >= maxAttempts)
            {
                giveUp(reason, attempt);
                return;
            }

            FLog.warning(String.format("[Discord] Connection failure %d/%d (%s); retrying in %ds.",
                    attempt, maxAttempts, reason, retrySeconds));
            scheduleRetry();
        }));
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
     * Fires when JDA has fully stopped. This listener is attached from the moment the client is
     * built and detached by the acquisition layer before any close the bridge asks for, so anything
     * reaching here is a connection we lost rather than one we closed, including the close codes
     * JDA declines to reconnect from.
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
        // One attempt in flight at a time. reportFailure() already collapses simultaneous reports
        // of the same drop, so this is a backstop for any other route into a retry.
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

            // The connection this failure was counted against is gone, and the attempt below gets
            // to spend one of its own. Cleared before the attempt runs, not after, because that
            // attempt can fail synchronously.
            failureCounted.set(false);
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
        // onConnectionLost already ran at the top of reportFailure(); nothing left to release.
    }
}
