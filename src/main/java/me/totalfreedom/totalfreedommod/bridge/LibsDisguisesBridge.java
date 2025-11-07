package me.totalfreedom.totalfreedommod.bridge;

import java.lang.reflect.Method;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.disguise.DisallowedDisguises;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Bridge to LibsDisguises plugin.
 * Supports modern LibsDisguises 11.0.0+ API using reflection for compatibility.
 * Based on TF-LibsDisguises functionality.
 */
public class LibsDisguisesBridge extends FreedomService
{

    private Plugin libsDisguisesPlugin = null;
    private Class<?> disguiseAPI = null;
    private Method isDisguisedMethod = null;
    private Method undisguiseToAllMethod = null;
    private DisallowedDisguises disallowedDisguises = null;

    public LibsDisguisesBridge(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        // Get DisallowedDisguises service
        disallowedDisguises = plugin.services.getService(DisallowedDisguises.class);

        // Initialize API lazily - LibsDisguises might not be loaded yet
        // We'll try to initialize it when first needed
        initializeAPI();

        // Schedule a delayed retry in case LibsDisguises loads after TFM
        // This handles the case where LibsDisguises is enabled but not fully initialized yet
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (disguiseAPI == null || isDisguisedMethod == null || undisguiseToAllMethod == null)
                {
                    initializeAPI();
                    if (disguiseAPI != null && isDisguisedMethod != null && undisguiseToAllMethod != null)
                    {
                        FLog.info("LibsDisguises bridge initialized successfully (delayed initialization).");
                    }
                }
            }
        }.runTaskLater(plugin, 40L); // Run 2 seconds after server start (40 ticks)
    }

    /**
     * Initializes the LibsDisguises API connection.
     * Can be called multiple times safely.
     */
    private void initializeAPI()
    {
        // If already initialized, don't try again
        if (disguiseAPI != null && isDisguisedMethod != null && undisguiseToAllMethod != null)
        {
            return;
        }

        try
        {
            final Plugin ldPlugin = server.getPluginManager().getPlugin("LibsDisguises");
            if (ldPlugin == null || !ldPlugin.isEnabled())
            {
                // Plugin not available yet, will retry later
                return;
            }

            libsDisguisesPlugin = ldPlugin;

            // Use the plugin's classloader to load the API class
            // This is more reliable than Class.forName() which uses the default classloader
            ClassLoader pluginClassLoader = ldPlugin.getClass().getClassLoader();
            
            try
            {
                // Try to get DisguiseAPI class using the plugin's classloader
                disguiseAPI = Class.forName("me.libraryaddict.disguise.DisguiseAPI", true, pluginClassLoader);
                
                // LibsDisguises 11.0.13 uses Entity instead of Player
                // Methods: isDisguised(Entity), undisguiseToAll(Entity)
                try
                {
                    Class<?> entityClass = Class.forName("org.bukkit.entity.Entity", true, pluginClassLoader);
                    isDisguisedMethod = disguiseAPI.getMethod("isDisguised", entityClass);
                    undisguiseToAllMethod = disguiseAPI.getMethod("undisguiseToAll", entityClass);
                    FLog.info("LibsDisguises bridge initialized successfully.");
                }
                catch (NoSuchMethodException | ClassNotFoundException ex1)
                {
                    // Fallback: try with Player (which extends Entity, so this should work too)
                    try
                    {
                        isDisguisedMethod = disguiseAPI.getMethod("isDisguised", Player.class);
                        undisguiseToAllMethod = disguiseAPI.getMethod("undisguiseToAll", Player.class);
                        FLog.info("LibsDisguises bridge initialized successfully (Player-based methods).");
                    }
                    catch (NoSuchMethodException ex2)
                    {
                        // List all available methods for debugging
                        FLog.warning("LibsDisguises API found but methods not accessible. Available methods:");
                        for (Method method : disguiseAPI.getMethods())
                        {
                            if (method.getName().contains("isDisguised") || method.getName().contains("undisguise"))
                            {
                                StringBuilder params = new StringBuilder();
                                for (Class<?> param : method.getParameterTypes())
                                {
                                    if (params.length() > 0) params.append(", ");
                                    params.append(param.getSimpleName());
                                }
                                FLog.warning("  - " + method.getName() + "(" + params + ")");
                            }
                        }
                        FLog.warning("Expected methods: isDisguised(Entity), undisguiseToAll(Entity)");
                        disguiseAPI = null;
                    }
                }
            }
            catch (ClassNotFoundException ex)
            {
                // Try alternative class name (some versions use different package)
                try
                {
                    disguiseAPI = Class.forName("me.libraryaddict.disguise.api.DisguiseAPI", true, pluginClassLoader);
                    isDisguisedMethod = disguiseAPI.getMethod("isDisguised", Player.class);
                    undisguiseToAllMethod = disguiseAPI.getMethod("undisguiseToAll", Player.class);
                    FLog.info("LibsDisguises bridge initialized successfully (alternative API path).");
                }
                catch (Exception ex2)
                {
                    FLog.warning("LibsDisguises API not found. Tried: me.libraryaddict.disguise.DisguiseAPI and me.libraryaddict.disguise.api.DisguiseAPI");
                    FLog.warning("LibsDisguises plugin is loaded but API class not accessible. Disguise features will be limited.");
                    disguiseAPI = null;
                }
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Error initializing LibsDisguises bridge: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    @Override
    protected void onStop()
    {
        libsDisguisesPlugin = null;
        disguiseAPI = null;
        isDisguisedMethod = null;
        undisguiseToAllMethod = null;
    }

    /**
     * Checks if LibsDisguises plugin is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isPluginEnabled()
    {
        // Try to initialize if not already done
        if (libsDisguisesPlugin == null)
        {
            initializeAPI();
        }
        
        return libsDisguisesPlugin != null && libsDisguisesPlugin.isEnabled();
    }

    /**
     * Checks if a player is disguised.
     * 
     * @param player The player to check
     * @return true if disguised, false otherwise, null if plugin not available
     */
    public Boolean isDisguised(Player player)
    {
        // Try to initialize API if not already done
        if (disguiseAPI == null || isDisguisedMethod == null)
        {
            initializeAPI();
        }
        
        if (!isPluginEnabled() || disguiseAPI == null || isDisguisedMethod == null)
        {
            return null;
        }

        try
        {
            // Player extends Entity, so we can pass Player to Entity methods
            Object result = isDisguisedMethod.invoke(null, (Entity) player);
            
            if (result instanceof Boolean)
            {
                return (Boolean) result;
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Error checking if player is disguised: " + ex.getMessage());
            FLog.severe(ex);
        }

        return null;
    }

    /**
     * Undisguises all players (optionally excluding admins).
     * 
     * @param includeAdmins If true, undisguises admins too; if false, skips admins
     */
    public void undisguiseAll(boolean includeAdmins)
    {
        // Try to initialize API if not already done
        if (disguiseAPI == null || undisguiseToAllMethod == null)
        {
            initializeAPI();
        }
        
        if (!isPluginEnabled() || disguiseAPI == null || undisguiseToAllMethod == null)
        {
            return;
        }

        try
        {
            for (Player player : server.getOnlinePlayers())
            {
                Boolean disguised = isDisguised(player);
                if (disguised != null && disguised)
                {
                    if (!includeAdmins && plugin.al.isAdmin(player))
                    {
                        continue;
                    }

                    // Player extends Entity, so we can pass Player to Entity methods
                    undisguiseToAllMethod.invoke(null, (Entity) player);
                }
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Error undisguising players: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    /**
     * Enables or disables disguises globally.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setDisguisesEnabled(boolean enabled)
    {
        if (disallowedDisguises != null)
        {
            disallowedDisguises.setDisabled(!enabled);
        }
    }

    /**
     * Checks if disguises are enabled globally.
     * 
     * @return true if enabled, false if disabled
     */
    public boolean isDisguisesEnabled()
    {
        if (disallowedDisguises != null)
        {
            return !disallowedDisguises.isDisabled();
        }
        return false;
    }

    /**
     * Checks if a disguise type is allowed.
     * 
     * @param disguiseTypeName The disguise type name
     * @return true if allowed, false if forbidden
     */
    public boolean isDisguiseAllowed(String disguiseTypeName)
    {
        if (disallowedDisguises != null)
        {
            return disallowedDisguises.isAllowed(disguiseTypeName);
        }
        return true; // Default to allowed if service not available
    }

    /**
     * Gets the DisallowedDisguises service.
     * 
     * @return The DisallowedDisguises service, or null if not available
     */
    public DisallowedDisguises getDisallowedDisguises()
    {
        return disallowedDisguises;
    }
}
