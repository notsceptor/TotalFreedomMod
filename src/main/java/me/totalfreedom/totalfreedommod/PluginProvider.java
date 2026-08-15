package me.totalfreedom.totalfreedommod;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import me.totalfreedom.api.FreedomAPI;

/**
 * Supplier for TotalFreedomMod that replaces the static singleton. 
 * Preferrably, use a constructor-injected plugin reference over calling this directly when possible.
 */
public final class PluginProvider
{
    private static final AtomicReference<FreedomAPI> INSTANCE = new AtomicReference<>();

    private PluginProvider()
    {
    }

    public static void bind(TotalFreedomMod plugin)
    {
        Objects.requireNonNull(plugin, "Cannot bind a null plugin instance.");

        if (!INSTANCE.compareAndSet(null, plugin))
        {
            throw new IllegalStateException("TotalFreedomMod is already bound.");
        }
    }

    public static void unbind()
    {
        INSTANCE.set(null);
    }

    /**
     * It is preferred that this is called at Point of Use.
     * <p>
     * Realistically, this should not be cached across bind boundaries 
     * because it will cause invariant states. HOWEVER, since binding should 
     * only be done in onLoad(), never during the plugins lifetime, 
     * and only unbound at onDisable(), caching the variable as in 
     * {@code var plugin = PluginProvider.get()} like in FCommand is fine, 
     * since the bind never changes during lifetime and binding is the 
     * first thing that happens in the plugins lifecycle in onLoad(), 
     * and everything else is registered and initialized in the onEnable() method.
     * <p>
     * Anything that requires work be done in the onLoad, onEnable, or onDisable blocks, 
     * should always use dependency injection and never use this class.
     * 
     * @return the live plugin instance.
     * @throws IllegalStateException if the plugin is not currently bound.
     */
    public static FreedomAPI get()
    {
        FreedomAPI plugin = INSTANCE.get(); // volatile read
        if (plugin == null)
        {
            throw new IllegalStateException("TotalFreedomMod is not currently initialized.");
        }
        return plugin;
    }
}
