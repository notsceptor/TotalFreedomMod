package me.totalfreedom.totalfreedommod.sql;

import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * This is a fair, non-blocking access controller that ensures our queries on the connection pool
 * don't overload our available connections.
 *
 * The {@link Semaphore} utilizes a FIFO waiting queue.
 * The query that has been waiting longest always receives the next available permit.
 * Permit release is guaranteed on completion, error, and cancellation.
 *
 * The same semaphore backs both the reactive {@link #guard} path and the synchronous
 * {@link #acquireSync()}/{@link #releaseSync()} pair, so {@link #availablePermits()} and
 * {@link #queueLength()} reflect total demand regardless of which path callers use.
 */
public final class AccessController
{
    // Matches HikariCP's own default connection timeout.
    private static final long ACQUIRE_TIMEOUT_SECONDS = 30L;

    private final Semaphore semaphore;
    private final Scheduler scheduler;

    /**
     * @param permits maximum number of concurrently executing queries.
     *                Should always match the HikariCP maximum pool size.
     * @param scheduler dedicated scheduler to acquire permits on, sized off the same pool.
     */
    public AccessController(final int permits, final Scheduler scheduler)
    {
        this.semaphore = new Semaphore(permits, true);
        this.scheduler = scheduler;
    }

    /**
     * Guard a single-result query. The permit is held for the entire duration of the query, 
     * including the time spent mapping the result to the return type.
     */
    public <T> Mono<T> guard(final Mono<T> query)
    {
        return Mono.usingWhen(
                acquire(),
                ignored -> query,
                ignored -> release());
    }

    /**
     * Guard a multi-row query or stream of results. 
     * The permit is held for the entire duration of the flux.
     */
    public <T> Flux<T> guard(final Flux<T> query)
    {
        return Flux.usingWhen(
                acquire(),
                ignored -> query,
                ignored -> release(),
                (ignored, err) -> release(),
                ignored -> release());
    }

    /**
     * Acquire a permit for a synchronous unit of work, 
     * for the handful of call sites that don't go through the reactive {@link #guard} path.
     * <p> 
     * Waits at most {@link #ACQUIRE_TIMEOUT_SECONDS} rather than blocking forever.
     * Always paired with {@link #releaseSync()}, typically in a {@code finally} block.
     */
    public void acquireSync() throws SQLException
    {
        final boolean acquired;
        try
        {
            acquired = semaphore.tryAcquire(ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a permit", ex);
        }

        if (!acquired)
            throw new SQLException(String.format(
                    "Timed out after %ds waiting for a permit", ACQUIRE_TIMEOUT_SECONDS));
    }

    public void releaseSync()
    {
        semaphore.release();
    }

    public int availablePermits()
    {
        return semaphore.availablePermits();
    }

    /** Number of queries waiting for permit. */
    public int queueLength()
    {
        return semaphore.getQueueLength();
    }

    private Mono<Boolean> acquire()
    {
        return Mono.<Boolean>create(sink ->
        {
            final Thread thread = Thread.currentThread();
            final AtomicBoolean cancelled = new AtomicBoolean(false);
            sink.onCancel(() ->
            {
                cancelled.set(true);
                thread.interrupt();
            });
            try
            {
                semaphore.acquire();
                if (cancelled.get())
                    semaphore.release();
                else
                    sink.success(Boolean.TRUE);
            }
            catch (final InterruptedException e)
            {
                sink.error(e);
            }
            finally
            {
                Thread.interrupted();
            }
        }).subscribeOn(scheduler);
    }

    private Mono<Void> release()
    {
        return Mono.fromRunnable(semaphore::release);
    }
}
