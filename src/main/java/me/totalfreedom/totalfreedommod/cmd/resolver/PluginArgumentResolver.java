package me.totalfreedom.totalfreedommod.cmd.resolver;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import me.totalfreedom.api.cmd.resolver.AbstractArgumentResolver;
import me.totalfreedom.api.cmd.resolver.ArgumentResolutionException;

public class PluginArgumentResolver implements AbstractArgumentResolver<Plugin>
{
    private final PluginManager pluginManager = Bukkit.getPluginManager();

    @Override
    public String name()
    {
        return "Plugin";
    }

    @Override
    public Plugin resolve(String arg, String strategy)
    {
        final Plugin plugin = pluginManager.getPlugin(arg);

        if (plugin == null)
        {
            throw new ArgumentResolutionException("Unknown plugin: " + arg);
        }

        return plugin;
    }
}
