package me.totalfreedom.totalfreedommod.bridge;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldEditBridge extends FreedomService
{

    private final WorldEditListener listener;
    //
    private Plugin worldedit = null;

    public WorldEditBridge(TotalFreedomMod plugin)
    {
        super(plugin);
        listener = new WorldEditListener(plugin);
    }

    @Override
    protected void onStart()
    {
        // Register TF-WorldEdit event handlers using reflection
        listener.registerTFWorldEditEvents();
        
        Plugin tfWorldEdit = server.getPluginManager().getPlugin("TF-WorldEdit");
        if (tfWorldEdit != null)
        {
            FLog.info("TF-WorldEdit detected. WorldEdit protection enabled.");
            // Try to manually register the WorldEditOperationEvent handler
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
        // Listener uses dynamic registration, unregistration is handled by Bukkit
    }

    public void undo(Player player, int count)
    {
        try
        {
            Object session = getPlayerSession(player);
            if (session != null)
            {
                final Object bukkitPlayer = getBukkitPlayer(player);
                if (bukkitPlayer != null)
                {
                    // Get getBlockBag method via reflection
                    java.lang.reflect.Method getBlockBagMethod = session.getClass().getMethod("getBlockBag", bukkitPlayer.getClass().getSuperclass());
                    Object blockBag = getBlockBagMethod.invoke(session, bukkitPlayer);
                    
                    // Get undo method via reflection
                    java.lang.reflect.Method undoMethod = null;
                    for (java.lang.reflect.Method m : session.getClass().getMethods())
                    {
                        if (m.getName().equals("undo") && m.getParameterCount() == 2)
                        {
                            undoMethod = m;
                            break;
                        }
                    }
                    
                    if (undoMethod != null)
                    {
                        for (int i = 0; i < count; i++)
                        {
                            undoMethod.invoke(session, blockBag, bukkitPlayer);
                        }
                    }
                }
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
                Plugin we = server.getPluginManager().getPlugin("WorldEdit");
                if (we != null && we.isEnabled())
                {
                    worldedit = we;
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
            final Object session = getPlayerSession(player);
            if (session != null)
            {
                // Call setBlockChangeLimit(int) via reflection
                java.lang.reflect.Method setLimitMethod = session.getClass().getMethod("setBlockChangeLimit", int.class);
                setLimitMethod.invoke(session, limit);
            }
        }
        catch (Exception ex)
        {
            FLog.severe(ex);
        }

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
            // Call getSession(Player) via reflection
            java.lang.reflect.Method getSessionMethod = wep.getClass().getMethod("getSession", Player.class);
            return getSessionMethod.invoke(wep, player);
        }
        catch (Exception ex)
        {
            FLog.severe(ex);
            return null;
        }
    }

    private Object getBukkitPlayer(Player player)
    {
        final Plugin wep = getWorldEditPlugin();
        if (wep == null)
        {
            return null;
        }

        try
        {
            // Call wrapPlayer(Player) via reflection
            java.lang.reflect.Method wrapPlayerMethod = wep.getClass().getMethod("wrapPlayer", Player.class);
            return wrapPlayerMethod.invoke(wep, player);
        }
        catch (Exception ex)
        {
            FLog.severe(ex);
            return null;
        }
    }
}
