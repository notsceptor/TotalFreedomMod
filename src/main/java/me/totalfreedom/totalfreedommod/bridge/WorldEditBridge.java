package me.totalfreedom.totalfreedommod.bridge;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldEditBridge extends FreedomService
{

    private final WorldEditListener listener;
    //
    private WorldEditPlugin worldedit = null;

    public WorldEditBridge(TotalFreedomMod plugin)
    {
        super(plugin);
        listener = new WorldEditListener(plugin);
    }

    @Override
    protected void onStart()
    {
        // Register listener - it's safe to register even if TF-WorldEdit isn't loaded yet
        // The events just won't fire if TF-WorldEdit isn't installed
        // TF-WorldEdit provides SelectionChangedEvent, LimitChangedEvent, and WorldEditOperationEvent
        listener.register();
        
        Plugin tfWorldEdit = server.getPluginManager().getPlugin("TF-WorldEdit");
        if (tfWorldEdit != null)
        {
            FLog.info("TF-WorldEdit detected. WorldEdit protection enabled.");
            // Try to manually register the event handler using the real event class
            registerWorldEditOperationHandler(tfWorldEdit);
        }
        else
        {
            FLog.info("TF-WorldEdit not detected. WorldEdit protection will be enabled when TF-WorldEdit loads.");
            // Schedule a delayed check in case TF-WorldEdit loads after TFM
            server.getScheduler().runTaskLater(plugin, () -> {
                Plugin tfwe = server.getPluginManager().getPlugin("TF-WorldEdit");
                if (tfwe != null)
                {
                    FLog.info("TF-WorldEdit detected (delayed). Registering WorldEdit protection.");
                    registerWorldEditOperationHandler(tfwe);
                }
            }, 40L);
        }
    }

    private void registerWorldEditOperationHandler(Plugin tfWorldEdit)
    {
        try
        {
            // Get the real event class from TF-WorldEdit's classloader
            ClassLoader tfweClassLoader = tfWorldEdit.getClass().getClassLoader();
            Class<?> eventClass = Class.forName("me.totalfreedom.worldedit.WorldEditOperationEvent", true, tfweClassLoader);
            
            // Verify it's an Event subclass
            if (!org.bukkit.event.Event.class.isAssignableFrom(eventClass))
            {
                FLog.warning("WorldEditOperationEvent from TF-WorldEdit is not a valid Event class.");
                return;
            }
            
            // Get the HandlerList
            java.lang.reflect.Method getHandlerListMethod = eventClass.getMethod("getHandlerList");
            org.bukkit.event.HandlerList handlerList = (org.bukkit.event.HandlerList) getHandlerListMethod.invoke(null);
            
            // Find the handleWorldEditOperation method which accepts Event (works with both stub and real classes)
            java.lang.reflect.Method handlerMethod = null;
            try
            {
                handlerMethod = listener.getClass().getMethod("handleWorldEditOperation", org.bukkit.event.Event.class);
            }
            catch (NoSuchMethodException ex)
            {
                FLog.warning("Could not find handleWorldEditOperation method in WorldEditListener.");
                return;
            }
            
            org.bukkit.event.EventPriority priority = org.bukkit.event.EventPriority.NORMAL;
            org.bukkit.plugin.Plugin pluginInstance = plugin;
            boolean ignoreCancelled = false;
            
            org.bukkit.event.Listener listenerInstance = listener;
            final java.lang.reflect.Method finalHandlerMethod = handlerMethod;
            final Class<?> finalEventClass = eventClass;
            org.bukkit.plugin.EventExecutor executor = (listener1, event) -> {
                try
                {
                    // Check if this is the right event type
                    if (!finalEventClass.isInstance(event))
                    {
                        return;
                    }
                    
                    // Invoke the handler method with the real event
                    // handleWorldEditOperation accepts Event, so it works with the real event class
                    finalHandlerMethod.invoke(listenerInstance, event);
                }
                catch (Exception ex)
                {
                    FLog.severe("Error handling WorldEditOperationEvent: " + ex.getMessage());
                    FLog.severe(ex);
                }
            };
            
            org.bukkit.plugin.RegisteredListener registeredListener = new org.bukkit.plugin.RegisteredListener(
                listenerInstance, executor, priority, pluginInstance, ignoreCancelled);
            
            handlerList.register(registeredListener);
            FLog.info("Successfully registered WorldEditOperationEvent handler.");
        }
        catch (ClassNotFoundException ex)
        {
            FLog.warning("Could not find WorldEditOperationEvent class in TF-WorldEdit. Protection may not work.");
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to register WorldEditOperationEvent handler: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    @Override
    protected void onStop()
    {
        listener.unregister();
    }

    public void undo(Player player, int count)
    {
        try
        {
            LocalSession session = getPlayerSession(player);
            if (session != null)
            {
                final BukkitPlayer bukkitPlayer = getBukkitPlayer(player);
                if (bukkitPlayer != null)
                {
                    for (int i = 0; i < count; i++)
                    {
                        session.undo(session.getBlockBag(bukkitPlayer), bukkitPlayer);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            FLog.severe(ex);
        }
    }

    private WorldEditPlugin getWorldEditPlugin()
    {
        if (worldedit == null)
        {
            try
            {
                Plugin we = server.getPluginManager().getPlugin("WorldEdit");
                if (we != null)
                {
                    if (we instanceof WorldEditPlugin)
                    {
                        worldedit = (WorldEditPlugin) we;
                    }
                }
            }
            catch (Exception ex)
            {
                FLog.severe(ex);
            }
        }

        return worldedit;
    }

    public void setLimit(Player player, int limit)
    {
        try
        {
            final LocalSession session = getPlayerSession(player);
            if (session != null)
            {
                session.setBlockChangeLimit(limit);
            }
        }
        catch (Exception ex)
        {
            FLog.severe(ex);
        }

    }

    private LocalSession getPlayerSession(Player player)
    {
        final WorldEditPlugin wep = getWorldEditPlugin();
        if (wep == null)
        {
            return null;
        }

        try
        {
            return wep.getSession(player);
        }
        catch (Exception ex)
        {
            FLog.severe(ex);
            return null;
        }
    }

    private BukkitPlayer getBukkitPlayer(Player player)
    {
        final WorldEditPlugin wep = getWorldEditPlugin();
        if (wep == null)
        {
            return null;
        }

        try
        {
            return wep.wrapPlayer(player);
        }
        catch (Exception ex)
        {
            FLog.severe(ex);
            return null;
        }
    }
}
