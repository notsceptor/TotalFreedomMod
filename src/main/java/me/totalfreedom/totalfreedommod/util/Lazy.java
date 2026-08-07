package me.totalfreedom.totalfreedommod.util;

import java.util.function.Supplier;

/**
 * A value that gets worked out the first time you ask for it, then cached.
 * <p>
 * Wrap the expensive part in a supplier and hand it over; nothing runs until the first
 * {@link #get()}. Every call after that hands back the same value, and the supplier is never run
 * again, including when it returned null.
 * <p>
 * Safe to share between threads. Do not call {@link #get()} from inside the supplier though, since
 * it will deadlock on the lock the first call is already holding.
 *
 * @param <T> the type being worked out
 */
public class Lazy<T> implements Supplier<T>
{
    private final Supplier<T> delegate;
    private volatile boolean initialized = false;
    private T value;

    public Lazy(Supplier<T> delegate)
    {
        this.delegate = delegate;
    }

    /**
     * The value, working it out on the first call.
     * <p>
     * The volatile on {@code initialized} is load bearing. Setting it after {@code value} is what
     * makes {@code value} visible to other threads, which is what lets the first check run without
     * taking the lock. Dropping volatile off the flag, or moving it onto {@code value} instead,
     * breaks that.
     */
    @Override
    public T get()
    {
        if (!initialized)
            synchronized(this)
            {
                if (!initialized)
                {
                    value = delegate.get();
                    initialized = true;
                }
                
            }

        return value;
    }
}
