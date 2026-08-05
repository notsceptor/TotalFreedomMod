package me.totalfreedom.totalfreedommod.discord.acquisition;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import me.totalfreedom.totalfreedommod.util.FLog;
import net.dv8tion.jda.api.JDA;

/**
 * Sole owner of the bridge's gateway connection.
 * <p>
 * A bot token supports one gateway session. Discord does not reject a second one, it serves it:
 * both sessions receive every message, so every console command typed in Discord runs twice and
 * both sessions share a REST rate-limit bucket that each client models privately. Nothing in the
 * Discord API will tell you this is happening, which is why acquiring the connection has to be
 * guarded on this side.
 * <p>
 * This class exists because the bridge previously did not guard it. {@code JDABuilder.build()}
 * opens the gateway before {@code awaitReady()} returns, so a start that failed after build lost
 * its only reference to a client that was already connected and stayed connected. A reload then
 * opened a second one, and that server was talking to Discord twice over a single token, with the
 * first session still holding listeners nothing could reach.
 * <p>
 * The guard is a small state machine:
 * <ul>
 *   <li>{@link #acquire} refuses outright if a connection is already held or being opened, so a
 *       retry, a reload and a standby poll racing each other still produce exactly one client;</li>
 *   <li>the client reference is captured the instant {@code build()} returns, before anything that
 *       can throw, so a failed acquisition always has something to shut down;</li>
 *   <li>a failed acquisition shuts that client down and returns to idle, leaking nothing;</li>
 *   <li>{@link #release} shuts the held client down exactly once, whoever calls it;</li>
 *   <li>every close performed here detaches the client's listeners first, so a close this layer
 *       performed is never mistaken for a connection that dropped on its own.</li>
 * </ul>
 */
public final class DiscordAcquisition
{
    private static final Duration SHUTDOWN_GRACE = Duration.ofSeconds(10);

    /**
     * Opens a client. Implementations return as soon as the builder does; waiting for the gateway
     * to become ready is this class's job, so the reference is captured first.
     */
    @FunctionalInterface
    public interface ConnectionFactory
    {
        JDA build() throws Exception;
    }

    private enum State
    {
        /** No client, and none being opened. The only state {@link #acquire} will act from. */
        IDLE,

        /** A client is being opened. Blocks any concurrent acquisition. */
        ACQUIRING,

        /** A client is open and in use. */
        HELD,

        /** The held client is being shut down. Blocks acquisition until it finishes. */
        RELEASING
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);

    private volatile JDA client;

    /**
     * Open a connection and take ownership of it.
     * <p>
     * On any failure the partially opened client is shut down before this returns, so the caller
     * never has to clean up after a failed attempt and no orphan session survives.
     *
     * @return the ready client, or empty if a connection is already held or being opened, which is
     *         a refusal rather than an error: something else already owns the token
     * @throws Exception whatever the factory or the readiness wait threw, after cleanup
     */
    public Optional<JDA> acquire(final ConnectionFactory factory) throws Exception
    {
        if (!state.compareAndSet(State.IDLE, State.ACQUIRING))
        {
            FLog.warning(String.format("[Discord] Refusing to open a second gateway connection: one is already %s.",
                    state.get() == State.HELD ? "established" : "being opened or closed"));
            return Optional.empty();
        }

        try
        {
            // Captured before awaitReady() so a throw below still has a client to shut down.
            // This assignment is the whole reason connections stopped being orphaned.
            final JDA opened = factory.build();
            client = opened;

            opened.awaitReady();
            state.set(State.HELD);
            return Optional.of(opened);
        }
        catch (Exception | Error thrown)
        {
            shutdownHeldClient();
            state.set(State.IDLE);
            throw thrown;
        }
    }

    /**
     * Shut the held connection down and return to idle. Idempotent, and safe to call when nothing
     * is held or while an acquisition is still in flight.
     */
    public void release()
    {
        final State previous = state.getAndSet(State.RELEASING);
        if (previous == State.IDLE && client == null)
        {
            state.set(State.IDLE);
            return;
        }

        shutdownHeldClient();
        state.set(State.IDLE);
    }

    /**
     * The live client, absent whenever one is not currently held.
     */
    public Optional<JDA> current()
    {
        return state.get() == State.HELD ? Optional.ofNullable(client) : Optional.empty();
    }

    public boolean isHeld()
    {
        return state.get() == State.HELD;
    }

    /**
     * Shut down whatever client this layer is holding, clearing the reference first so a second
     * caller cannot shut the same client down again.
     */
    private void shutdownHeldClient()
    {
        final JDA held = client;
        client = null;

        if (held == null)
            return;

        detachListeners(held);

        try
        {
            held.shutdown();
            if (!held.awaitShutdown(SHUTDOWN_GRACE))
            {
                FLog.warning("[Discord] Gateway did not close within the grace period; forcing it.");
                held.shutdownNow();
            }
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            held.shutdownNow();
        }
        catch (Exception ex)
        {
            FLog.warning(String.format("[Discord] Error while closing the gateway: %s", ex.getMessage()));
        }
    }

    /**
     * Drop everything listening on {@code client} before it is closed, so the shutdown event that
     * follows reaches nobody.
     * <p>
     * Every close this class performs is deliberate: a caller releasing the connection, or cleanup
     * after an attempt that failed. The supervisor treats a shutdown it observes as a connection
     * that dropped on its own and spends a reconnect attempt on it, and none of these are that.
     * Detaching here rather than at each call site is what lets the supervisor stay attached from
     * the moment the client is built, with no window where a genuine drop goes unseen.
     * <p>
     * Failing to detach only costs a duplicate failure report, so it must not stop the shutdown
     * below from running.
     */
    private static void detachListeners(final JDA client)
    {
        try
        {
            final List<Object> listeners = client.getRegisteredListeners();
            if (!listeners.isEmpty())
                client.removeEventListener(listeners.toArray());
        }
        catch (Exception ex)
        {
            FLog.warning(String.format("[Discord] Could not detach listeners before closing the gateway: %s",
                    ex.getMessage()));
        }
    }
}
