package me.totalfreedom.totalfreedommod.framework;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import org.bukkit.event.Listener;

/**
 * Base class for plugin listeners.
 * Extends PluginComponent and implements Listener.
 */
public class PluginListener<T extends TotalFreedomMod> extends PluginComponent<T> implements Listener
{

    public PluginListener(T plugin)
    {
        super(plugin);
    }
}

