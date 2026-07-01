package me.totalfreedom.totalfreedommod.bridge;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldEditBridge extends FreedomService
{

    private WorldEditHook hook = null;
    private Plugin worldedit = null;

    public WorldEditBridge(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        if (!ConfigEntry.WORLDEDIT_ENABLED.getBoolean())
        {
            FLog.info("TFM WorldEdit integration disabled via config (worldedit.enabled=false).");
            return;
        }

        // Defer one tick so other plugins finish enabling first; if WorldEdit
        // still isn't present after that we exit silently.
        server.getScheduler().runTaskLater(plugin, this::attachHook, 20L);
    }

    private void attachHook()
    {
        if (resolveWorldEditProvider() == null)
        {
            return;
        }
        try
        {
            hook = new WorldEditHook(plugin);
            hook.register();
        }
        catch (Throwable t)
        {
            FLog.warning("Failed to attach WorldEdit hook: " + t.getMessage());
            FLog.warning(t);
            hook = null;
        }
    }

    /**
     * Returns whichever WorldEdit-compatible plugin is loaded, or null if none.
     */
    private Plugin resolveWorldEditProvider()
    {
        Plugin we = server.getPluginManager().getPlugin("WorldEdit");
        if (we != null && we.isEnabled())
        {
            return we;
        }
        we = server.getPluginManager().getPlugin("FastAsyncWorldEdit");
        if (we != null && we.isEnabled())
        {
            return we;
        }
        return null;
    }

    @Override
    protected void onStop()
    {
        if (hook != null)
        {
            try
            {
                hook.unregister();
            }
            catch (Throwable ignored)
            {
            }
            hook = null;
        }
    }

    public void undo(Player player, int count)
    {
        if (hook != null)
        {
            hook.undo(player, count);
        }
    }

    public void refreshBypassNegation(Player player)
    {
        if (hook != null)
        {
            hook.refreshBypassNegation(player);
        }
    }

    public void setLimit(Player player, int limit)
    {
        if (hook != null)
        {
            hook.setPlayerLimit(player.getUniqueId(), limit);
        }
        try
        {
            final Object session = getPlayerSession(player);
            if (session != null)
            {
                java.lang.reflect.Method setLimitMethod = session.getClass().getMethod("setBlockChangeLimit", int.class);
                setLimitMethod.invoke(session, limit);
            }
        }
        catch (Exception ex)
        {
            FLog.severe(ex);
        }
    }

    private Plugin getWorldEditPlugin()
    {
        if (worldedit == null)
        {
            try
            {
                worldedit = resolveWorldEditProvider();
            }
            catch (Exception ex)
            {
                FLog.severe(ex);
            }
        }
        return worldedit;
    }

    private Object getPlayerSession(Player player)
    {
        final Plugin wep = getWorldEditPlugin();
        if (wep == null)
        {
            return null;
        }
        try
        {
            java.lang.reflect.Method getSessionMethod = wep.getClass().getMethod("getSession", Player.class);
            return getSessionMethod.invoke(wep, player);
        }
        catch (Exception ex)
        {
            FLog.severe(ex);
            return null;
        }
    }

}
