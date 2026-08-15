package me.totalfreedom.totalfreedommod.disguise;

import me.totalfreedom.api.FreedomAPI;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.EntityType;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;

/**
 * Manages forbidden disguise types and global disguise state.
 * Based on TF-LibsDisguises functionality.
 */
public class DisallowedDisguises extends FreedomService
{

    // Default forbidden disguise types (from TF-LibsDisguises)
    private static final String[] DEFAULT_FORBIDDEN = new String[]
    {
        "FISHING_HOOK", "ITEM_FRAME", "ENDER_DRAGON", "PLAYER", "GIANT",
        "GHAST", "MAGMA_CUBE", "SLIME", "DROPPED_ITEM", "ENDER_CRYSTAL",
        "AREA_EFFECT_CLOUD", "WITHER"
    };

    private final Set<String> forbiddenDisguiseTypes = new HashSet<>();
    private boolean disabled = false;

    public DisallowedDisguises(FreedomAPI plugin)
    {
        super(plugin);
    }

    @Override
    public void onStart()
    {
        loadForbiddenDisguises();
    }

    @Override
    public void onStop()
    {
        forbiddenDisguiseTypes.clear();
    }

    private void loadForbiddenDisguises()
    {
        forbiddenDisguiseTypes.clear();

        // Load from config
        List<?> configList = ConfigEntry.DISGUISES_FORBIDDEN_TYPES.getList();
        if (configList != null && !configList.isEmpty())
        {
            for (Object item : configList)
            {
                if (item instanceof String)
                {
                    forbiddenDisguiseTypes.add(((String) item).toUpperCase());
                }
            }
        }
        else
        {
            // Use defaults if config is empty
            for (String type : DEFAULT_FORBIDDEN)
            {
                forbiddenDisguiseTypes.add(type);
            }
        }

        FLog.info("Loaded " + forbiddenDisguiseTypes.size() + " forbidden disguise types.");
    }

    /**
     * Checks if a disguise type is allowed.
     * 
     * @param disguiseTypeName The disguise type name (e.g., "ZOMBIE", "CREEPER")
     * @return true if allowed, false if forbidden
     */
    public boolean isAllowed(String disguiseTypeName)
    {
        if (disguiseTypeName == null)
        {
            return false;
        }

        return !forbiddenDisguiseTypes.contains(disguiseTypeName.toUpperCase());
    }

    /**
     * Checks if a disguise type is allowed.
     * 
     * @param entityType The EntityType to check
     * @return true if allowed, false if forbidden
     */
    public boolean isAllowed(EntityType entityType)
    {
        if (entityType == null)
        {
            return false;
        }

        return isAllowed(entityType.name());
    }

    /**
     * Checks if disguises are globally disabled.
     * 
     * @return true if disabled, false if enabled
     */
    public boolean isDisabled()
    {
        return disabled;
    }

    /**
     * Sets the global disabled state.
     * 
     * @param disabled true to disable all disguises, false to enable
     */
    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    /**
     * Gets the list of forbidden disguise types.
     * 
     * @return A copy of the forbidden disguise types set
     */
    public Set<String> getForbiddenDisguiseTypes()
    {
        return new HashSet<>(forbiddenDisguiseTypes);
    }

    /**
     * Adds a disguise type to the forbidden list.
     * 
     * @param disguiseTypeName The disguise type name to forbid
     */
    public void addForbidden(String disguiseTypeName)
    {
        if (disguiseTypeName != null)
        {
            forbiddenDisguiseTypes.add(disguiseTypeName.toUpperCase());
        }
    }

    /**
     * Removes a disguise type from the forbidden list.
     * 
     * @param disguiseTypeName The disguise type name to allow
     */
    public void removeForbidden(String disguiseTypeName)
    {
        if (disguiseTypeName != null)
        {
            forbiddenDisguiseTypes.remove(disguiseTypeName.toUpperCase());
        }
    }

    /**
     * Reloads the forbidden disguise types from config.
     */
    public void reload()
    {
        loadForbiddenDisguises();
    }
}

