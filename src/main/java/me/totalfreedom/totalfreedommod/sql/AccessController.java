package me.totalfreedom.totalfreedommod.sql;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * This is a fair, non-blocking access controller that ensures our queries on the connection pool
 * don't overload our available connections.
 *
 * The {@link Semaphore} utilizes a FIFO waiting queue.
 * The query that has been waiting longest always receives the next available permit. 
 * Permit release is guaranteed on completion, error, and cancellation.
 */
public final class AccessController
{
    private final Semaphore semaphore;

    /**
     * @param permits maximum number of concurrently executing queries. 
     *                Should always match the HikariCP maximum pool size.
     */
    public AccessController(final int permits)
    {
        this.semaphore = new Semaphore(permits, true);
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
                {
                    semaphore.release();
                }
                else
                {
                    sink.success(Boolean.TRUE);
                }
            }
            catch (final InterruptedException e)
            {
                sink.error(e);
            }
            finally
            {
                Thread.interrupted();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> release()
    {
        return Mono.fromRunnable(semaphore::release);
    }
}
