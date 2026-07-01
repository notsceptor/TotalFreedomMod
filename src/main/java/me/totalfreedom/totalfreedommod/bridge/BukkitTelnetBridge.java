package me.totalfreedom.totalfreedommod.bridge;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

public class BukkitTelnetBridge extends FreedomService
{

    private Plugin bukkitTelnetPlugin = null;
    private boolean eventsRegistered = false;
    private boolean detectionAttempted = false;

    public BukkitTelnetBridge(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        registerEventsIfAvailable();
		
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
        {
            if (!eventsRegistered)
            {
                registerEventsIfAvailable();
            }
        }, 40L);
    }

    @Override
    protected void onStop()
    {
    }

    private void registerEventsIfAvailable()
    {
        if (eventsRegistered || detectionAttempted)
        {
            return;
        }

        detectionAttempted = true;

        try
        {
            final Plugin btPlugin = server.getPluginManager().getPlugin("BukkitTelnet");
            if (btPlugin == null || !btPlugin.isEnabled())
            {
                FLog.warning("BukkitTelnet not detected. Telnet bridge features will be unavailable.");
                return;
            }

            bukkitTelnetPlugin = btPlugin;
            ClassLoader pluginClassLoader = btPlugin.getClass().getClassLoader();

            registerTelnetEvent("me.totalfreedom.bukkittelnet.api.TelnetPreLoginEvent", 
                "onTelnetPreLogin", pluginClassLoader, EventPriority.NORMAL);
            
            registerTelnetEvent("me.totalfreedom.bukkittelnet.api.TelnetCommandEvent", 
                "onTelnetCommand", pluginClassLoader, EventPriority.NORMAL);
            
            registerTelnetEvent("me.totalfreedom.bukkittelnet.api.TelnetRequestDataTagsEvent", 
                "onTelnetRequestDataTags", pluginClassLoader, EventPriority.NORMAL);

            eventsRegistered = true;
            FLog.info("BukkitTelnet bridge initialized successfully.");
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to initialize BukkitTelnet bridge: " + ex.getMessage());
            FLog.warning("Telnet bridge features will be unavailable.");
        }
    }

    private void registerTelnetEvent(String eventClassName, String handlerMethodName, 
                                     ClassLoader pluginClassLoader, EventPriority priority)
    {
        try
        {
            Class<?> eventClass = Class.forName(eventClassName, true, pluginClassLoader);
            
            if (!Event.class.isAssignableFrom(eventClass))
            {
                FLog.warning(eventClassName + " is not a valid Event class.");
                return;
            }

            Method getHandlerListMethod = eventClass.getMethod("getHandlerList");
            org.bukkit.event.HandlerList handlerList = (org.bukkit.event.HandlerList) getHandlerListMethod.invoke(null);

            Method handlerMethod = this.getClass().getMethod(handlerMethodName, Event.class);
            
            boolean ignoreCancelled = false;
            org.bukkit.plugin.Plugin pluginInstance = plugin;
            org.bukkit.event.Listener listenerInstance = (org.bukkit.event.Listener) this;
            final Method finalHandlerMethod = handlerMethod;
            final Class<?> finalEventClass = eventClass;
            
            org.bukkit.plugin.EventExecutor executor = (listener1, event) -> {
                try
                {
                    if (!finalEventClass.isInstance(event))
                    {
                        return;
                    }
                    finalHandlerMethod.invoke(listenerInstance, event);
                }
                catch (Exception ex)
                {
                    FLog.severe("Error handling " + eventClassName + ": " + ex.getMessage());
                    FLog.severe(ex);
                }
            };

            org.bukkit.plugin.RegisteredListener registeredListener = new org.bukkit.plugin.RegisteredListener(
                listenerInstance, executor, priority, pluginInstance, ignoreCancelled);

            handlerList.register(registeredListener);
        }
        catch (ClassNotFoundException ex)
        {
            FLog.warning("Could not find " + eventClassName + " class. Telnet bridge features may be limited.");
        }
        catch (NoSuchMethodException ex)
        {
            FLog.warning("Could not find handler method " + handlerMethodName + " for " + eventClassName);
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to register " + eventClassName + " handler: " + ex.getMessage());
        }
    }

    public void onTelnetPreLogin(Event event)
    {
        try
        {
            Method getIpMethod = event.getClass().getMethod("getIp");
            String ip = (String) getIpMethod.invoke(event);
            
            if (ip == null || ip.isEmpty())
            {
                return;
            }

            final Admin admin = plugin.al.getEntryByIpFuzzy(ip);

            if (admin == null || !admin.isActive())
            {
                return;
            }

            Method setBypassPasswordMethod = event.getClass().getMethod("setBypassPassword", boolean.class);
            setBypassPasswordMethod.invoke(event, true);
            
            Method setNameMethod = event.getClass().getMethod("setName", String.class);
            setNameMethod.invoke(event, admin.getName());
        }
        catch (Exception ex)
        {
            FLog.severe("Error in onTelnetPreLogin: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    public void onTelnetCommand(Event event)
    {
        try
        {
            Method getCommandMethod = event.getClass().getMethod("getCommand");
            String command = (String) getCommandMethod.invoke(event);
            
            Method getSenderMethod = event.getClass().getMethod("getSender");
            org.bukkit.command.CommandSender sender = (org.bukkit.command.CommandSender) getSenderMethod.invoke(event);
            
            if (plugin.cb.isCommandBlocked(command, sender))
            {
                Method setCancelledMethod = event.getClass().getMethod("setCancelled", boolean.class);
                setCancelledMethod.invoke(event, true);
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Error in onTelnetCommand: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    public void onTelnetRequestDataTags(Event event)
    {
        try
        {
            Method getDataTagsMethod = event.getClass().getMethod("getDataTags");
            Map<Player, Map<String, Object>> dataTags = (Map<Player, Map<String, Object>>) getDataTagsMethod.invoke(event);
            
            final Iterator<Map.Entry<Player, Map<String, Object>>> it = dataTags.entrySet().iterator();
            while (it.hasNext())
            {
                final Map.Entry<Player, Map<String, Object>> entry = it.next();
                final Player player = entry.getKey();
                final Map<String, Object> playerTags = entry.getValue();

                boolean isAdmin = false;
                boolean isSeniorAdmin = false;

                final Admin admin = plugin.al.getAdmin(player);
                if (admin != null)
                {
                    boolean active = admin.isActive();

                    isAdmin = active;
                    isSeniorAdmin = active && admin.getRank() == Rank.SENIOR_ADMIN;
                }

                playerTags.put("tfm.admin.isAdmin", isAdmin);
                playerTags.put("tfm.admin.isSeniorAdmin", isSeniorAdmin);

                playerTags.put("tfm.playerdata.getTag", plugin.pl.getPlayer(player).getTag());

                playerTags.put("tfm.essentialsBridge.getNickname", plugin.esb.getNickname(player.getName()));
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Error in onTelnetRequestDataTags: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    public Plugin getBukkitTelnetPlugin()
    {
        if (bukkitTelnetPlugin == null)
        {
            try
            {
                final Plugin btPlugin = server.getPluginManager().getPlugin("BukkitTelnet");
                if (btPlugin != null && btPlugin.isEnabled())
                {
                    bukkitTelnetPlugin = btPlugin;
                }
            }
            catch (Exception ex)
            {
                FLog.warning("Error getting BukkitTelnet plugin: " + ex.getMessage());
            }
        }

        return bukkitTelnetPlugin;
    }

    public void killTelnetSessions(final String name)
    {
        try
        {
            final Plugin telnet = getBukkitTelnetPlugin();
            if (telnet == null)
            {
                return;
            }

            Object appender = telnet.getClass().getMethod("getAppender").invoke(telnet);
            if (appender == null)
            {
                return;
            }

            Method getSessionsMethod = appender.getClass().getMethod("getSessions");
            List<?> sessions = (List<?>) getSessionsMethod.invoke(appender);
            
            final List<Object> sessionsToRemove = new ArrayList<>();

            final Iterator<?> it = sessions.iterator();
            while (it.hasNext())
            {
                Object session = it.next();
                Method getUserNameMethod = session.getClass().getMethod("getUserName");
                String userName = (String) getUserNameMethod.invoke(session);
                
                if (name != null && name.equalsIgnoreCase(userName))
                {
                    sessionsToRemove.add(session);
                }
            }

            for (final Object session : sessionsToRemove)
            {
                try
                {
                    Method removeSessionMethod = appender.getClass().getMethod("removeSession", session.getClass());
                    removeSessionMethod.invoke(appender, session);
                    
                    Method syncTerminateMethod = session.getClass().getMethod("syncTerminateSession");
                    syncTerminateMethod.invoke(session);
                }
                catch (Exception ex)
                {
                    FLog.warning("Error removing single telnet session: " + ex.getMessage());
                }
            }

            FLog.info(sessionsToRemove.size() + " telnet session(s) removed.");
        }
        catch (Exception ex)
        {
            FLog.warning("Error removing telnet sessions: " + ex.getMessage());
        }
    }
}
