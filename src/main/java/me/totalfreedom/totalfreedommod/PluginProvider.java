package me.totalfreedom.totalfreedommod;

import java.util.function.Supplier;

/**
 * Supplier for TotalFreedomMod that replaces the static singleton. 
 * Preferrably, use a constructor-injected plugin reference over calling this directly when possible.
 */
public final class PluginProvider
{
    private static Supplier<TotalFreedomMod> supplier = PluginProvider::unbound;

    private PluginProvider()
    {
    }

    public static void bind(Supplier<TotalFreedomMod> pluginSupplier)
    {
        supplier = pluginSupplier;
    }

    public static void unbind()
    {
        supplier = PluginProvider::unbound;
    }

    public static TotalFreedomMod get()
    {
        return supplier.get();
    }

    private static TotalFreedomMod unbound()
    {
        throw new IllegalStateException("TotalFreedomMod is not currently initialized.");
    }
}
