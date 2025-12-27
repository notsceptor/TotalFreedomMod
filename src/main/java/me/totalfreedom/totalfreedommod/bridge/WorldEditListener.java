package me.totalfreedom.totalfreedommod.bridge;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;

import me.totalfreedom.totalfreedommod.framework.PluginListener;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.event.EventPriority;
import org.bukkit.util.Vector;

public class WorldEditListener extends PluginListener<TotalFreedomMod>
{

    public WorldEditListener(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    public void registerTFWorldEditEvents()
    {
        // Try to register for TF-WorldEdit events
        Plugin tfWorldEdit = plugin.getServer().getPluginManager().getPlugin("TF-WorldEdit");
        if (tfWorldEdit != null && tfWorldEdit.isEnabled())
        {
            doRegisterTFWorldEditEvents(tfWorldEdit);
        }
        else
        {
            // Schedule a delayed check
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Plugin tfwe = plugin.getServer().getPluginManager().getPlugin("TF-WorldEdit");
                if (tfwe != null && tfwe.isEnabled())
                {
                    doRegisterTFWorldEditEvents(tfwe);
                }
            }, 40L);
        }
    }

    private void doRegisterTFWorldEditEvents(Plugin tfWorldEdit)
    {
        ClassLoader loader = tfWorldEdit.getClass().getClassLoader();
        
        // Register SelectionChangedEvent handler
        registerEventHandler(loader, "me.totalfreedom.worldedit.SelectionChangedEvent", this::handleSelectionChange);
        
        // Register LimitChangedEvent handler
        registerEventHandler(loader, "me.totalfreedom.worldedit.LimitChangedEvent", this::handleLimitChanged);
    }

    private void registerEventHandler(ClassLoader loader, String eventClassName, java.util.function.Consumer<Event> handler)
    {
        try
        {
            Class<?> eventClass = Class.forName(eventClassName, true, loader);
            
            if (!Event.class.isAssignableFrom(eventClass))
            {
                FLog.warning(eventClassName + " is not a valid Event class.");
                return;
            }
            
            java.lang.reflect.Method getHandlerListMethod = eventClass.getMethod("getHandlerList");
            HandlerList handlerList = (HandlerList) getHandlerListMethod.invoke(null);
            
            EventExecutor executor = (listener, event) -> {
                if (eventClass.isInstance(event))
                {
                    handler.accept(event);
                }
            };
            
            RegisteredListener registeredListener = new RegisteredListener(
                this, executor, EventPriority.NORMAL, plugin, false);
            
            handlerList.register(registeredListener);
            FLog.info("Registered handler for " + eventClassName);
        }
        catch (ClassNotFoundException ex)
        {
            // Event class not available - expected if TF-WorldEdit is not installed
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to register handler for " + eventClassName + ": " + ex.getMessage());
        }
    }

    private void handleSelectionChange(Event event)
    {
        try
        {
            java.lang.reflect.Method getPlayerMethod = event.getClass().getMethod("getPlayer");
            Player player = (Player) getPlayerMethod.invoke(event);

            if (plugin.al.isAdmin(player))
            {
                return;
            }

            java.lang.reflect.Method getMinVectorMethod = event.getClass().getMethod("getMinVector");
            java.lang.reflect.Method getMaxVectorMethod = event.getClass().getMethod("getMaxVector");
            java.lang.reflect.Method getWorldMethod = event.getClass().getMethod("getWorld");

            Vector min = (Vector) getMinVectorMethod.invoke(event);
            Vector max = (Vector) getMaxVectorMethod.invoke(event);
            org.bukkit.World world = (org.bukkit.World) getWorldMethod.invoke(event);

            if (plugin.pa.isInProtectedArea(min, max, world.getName()))
            {
                player.sendMessage(ChatColor.RED + "The region that you selected contained a protected area. Selection cleared.");
                java.lang.reflect.Method setCancelledMethod = event.getClass().getMethod("setCancelled", boolean.class);
                setCancelledMethod.invoke(event, true);
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Error handling SelectionChangedEvent: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    private void handleLimitChanged(Event event)
    {
        try
        {
            java.lang.reflect.Method getPlayerMethod = event.getClass().getMethod("getPlayer");
            java.lang.reflect.Method getTargetMethod = event.getClass().getMethod("getTarget");
            java.lang.reflect.Method getLimitMethod = event.getClass().getMethod("getLimit");

            Player player = (Player) getPlayerMethod.invoke(event);
            Player target = (Player) getTargetMethod.invoke(event);
            int limit = (int) getLimitMethod.invoke(event);

            if (plugin.al.isAdmin(player))
            {
                return;
            }

            if (!player.equals(target))
            {
                player.sendMessage(ChatColor.RED + "Only admins can change the limit for other players!");
                java.lang.reflect.Method setCancelledMethod = event.getClass().getMethod("setCancelled", boolean.class);
                setCancelledMethod.invoke(event, true);
                return;
            }

            int maxLimit = ConfigEntry.WORLDEDIT_LIMIT_MAX.getInteger();
            if (maxLimit < 0)
            {
                return;
            }
            if (limit < 0 || limit > maxLimit)
            {
                if (ConfigEntry.WORLDEDIT_DEOP_ON_LIMIT_ABUSE.getBoolean())
                {
                    player.setOp(false);
                    FUtil.bcastMsg(player.getName() + " tried to set their WorldEdit limit to " + limit + " and has been de-opped", ChatColor.RED);
                }
                else
                {
                    FUtil.bcastMsg(player.getName() + " tried to set their WorldEdit limit to " + limit, ChatColor.RED);
                }
                java.lang.reflect.Method setCancelledMethod = event.getClass().getMethod("setCancelled", boolean.class);
                setCancelledMethod.invoke(event, true);
                player.sendMessage(ChatColor.RED + "You cannot set your limit higher than " + maxLimit + " or to -1!");
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Error handling LimitChangedEvent: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    /**
     * Handles WorldEditOperationEvent using reflection to work with the real event class from TF-WorldEdit.
     * This method can be called with the real event class.
     */
    public void handleWorldEditOperation(Event event)
    {
        try
        {
            // Use reflection to get event properties since we might have the real event class
            java.lang.reflect.Method getPlayerMethod = event.getClass().getMethod("getPlayer");
            Player player = (Player) getPlayerMethod.invoke(event);

            if (player == null)
            {
                return;
            }

            if (plugin.al.isAdmin(player))
            {
                return;
            }

            java.lang.reflect.Method getMinVectorMethod = event.getClass().getMethod("getMinVector");
            java.lang.reflect.Method getMaxVectorMethod = event.getClass().getMethod("getMaxVector");
            java.lang.reflect.Method getWorldMethod = event.getClass().getMethod("getWorld");

            Vector min = (Vector) getMinVectorMethod.invoke(event);
            Vector max = (Vector) getMaxVectorMethod.invoke(event);
            org.bukkit.World world = (org.bukkit.World) getWorldMethod.invoke(event);
            String worldName = world != null ? world.getName() : null;

            if (min == null || max == null || worldName == null)
            {
                return;
            }

            if (plugin.pa.isInProtectedArea(min, max, worldName))
            {
                java.lang.reflect.Method setCancelledMethod = event.getClass().getMethod("setCancelled", boolean.class);
                setCancelledMethod.invoke(event, true);
                player.sendMessage(ChatColor.RED + "You cannot perform WorldEdit operations in a protected area!");
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Error handling WorldEditOperationEvent: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

}
