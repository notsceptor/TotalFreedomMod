package me.totalfreedom.totalfreedommod.rank;

import java.util.HashSet;
import java.util.Set;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Represents a configurable rank in the TFM permission system.
 * Unlike the built-in Rank enum, CustomRank instances can be created,
 * modified, and persisted at runtime.
 *
 * The permission system is internal and does NOT use Bukkit permission nodes,
 * because all players on TotalFreedom servers have OP status.
 */
public class CustomRank implements Displayable, Comparable<CustomRank>
{
    /**
     * Unique identifier for this rank (lowercase, no spaces).
     */
    private String id;
    
    /**
     * Display name shown in chat and menus.
     */
    private String name;
    
    /**
     * Grammatical determiner (a, an, the).
     */
    private String determiner = "a";
    
    /**
     * Short abbreviation shown in tag brackets.
     */
    private String abbreviation;
    
    /**
     * Rank level/weight for hierarchy comparison.
     * Higher values = more permissions.
     */
    private int level;
    
    /**
     * The color for this rank's display.
     */
    private NamedTextColor color = NamedTextColor.WHITE;
    
    /**
     * Whether this rank has admin privileges.
     */
    private boolean admin = false;
    
    /**
     * Whether this rank is a console-only variant.
     */
    private boolean consoleOnly = false;
    
    /**
     * Internal permissions granted to this rank.
     * These are TFM-specific permission strings, NOT Bukkit permission nodes.
     * Examples: "tfm.admin.ban", "tfm.fun.smite", "tfm.manage.ranks"
     */
    private Set<String> permissions = new HashSet<>();
    
    /**
     * Custom prefix string for chat display (e.g., "&8[&6SrA&8] ").
     */
    private String prefix = null;
    
    /**
     * ID of parent rank to inherit permissions from.
     */
    private String inheritFrom = null;
    
    /**
     * Flattened permissions including inherited permissions. Computed at runtime by
     * RankManager.resolveInheritance(), not raw stored data, so it's excluded from
     * JSON serialization.
     */
    private transient Set<String> resolvedPermissions = new HashSet<>();
    
    // Cached components for performance
    private transient Component cachedColoredTag;
    private transient Component cachedColoredName;
    private transient Component cachedColoredLoginMessage;
    
    /**
     * Creates a new CustomRank with default values.
     */
    public CustomRank(String id)
    {
        this.id = id.toLowerCase().replace(" ", "_");
        this.name = id;
        this.abbreviation = id.length() > 3 ? id.substring(0, 3).toUpperCase() : id.toUpperCase();
        this.level = 0;
        invalidateCache();
    }
    
    /**
     * Creates a CustomRank from an existing Rank enum (for migration).
     */
    public static CustomRank fromLegacyRank(Rank rank)
    {
        CustomRank custom = new CustomRank(rank.name().toLowerCase());
        custom.setName(rank.getName());
        custom.setDeterminer(rank.getDeterminer());
        custom.setAbbreviation(rank.getTag().replace("[", "").replace("]", ""));
        custom.setLevel(rank.getLevel());
        custom.setColor(rank.getColor());
        custom.setAdmin(rank.isAdmin());
        custom.setConsoleOnly(rank.isConsole());

        return custom;
    }
    
    /**
     * Load rank data from a configuration section.
     */
    public void loadFrom(ConfigurationSection cs)
    {
        this.name = cs.getString("name", id);
        this.determiner = cs.getString("determiner", "a");
        this.abbreviation = cs.getString("abbreviation", name.length() > 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase());
        this.level = cs.getInt("level", 0);
        
        String colorName = cs.getString("color", "WHITE");
        this.color = parseColor(colorName);
        
        this.admin = cs.getBoolean("admin", false);
        this.consoleOnly = cs.getBoolean("console_only", false);
        this.prefix = cs.getString("prefix", null);
        this.inheritFrom = cs.getString("inherit", null);
        
        this.permissions.clear();
        if (cs.contains("permissions"))
        {
            this.permissions.addAll(cs.getStringList("permissions"));
        }
        
        invalidateCache();
    }
    
    /**
     * Parse a color name to NamedTextColor.
     */
    private NamedTextColor parseColor(String name)
    {
        if (name == null) return NamedTextColor.WHITE;
        
        return switch (name.toUpperCase())
        {
            case "BLACK" -> NamedTextColor.BLACK;
            case "DARK_BLUE" -> NamedTextColor.DARK_BLUE;
            case "DARK_GREEN" -> NamedTextColor.DARK_GREEN;
            case "DARK_AQUA" -> NamedTextColor.DARK_AQUA;
            case "DARK_RED" -> NamedTextColor.DARK_RED;
            case "DARK_PURPLE" -> NamedTextColor.DARK_PURPLE;
            case "GOLD" -> NamedTextColor.GOLD;
            case "GRAY" -> NamedTextColor.GRAY;
            case "DARK_GRAY" -> NamedTextColor.DARK_GRAY;
            case "BLUE" -> NamedTextColor.BLUE;
            case "GREEN" -> NamedTextColor.GREEN;
            case "AQUA" -> NamedTextColor.AQUA;
            case "RED" -> NamedTextColor.RED;
            case "LIGHT_PURPLE" -> NamedTextColor.LIGHT_PURPLE;
            case "YELLOW" -> NamedTextColor.YELLOW;
            default -> NamedTextColor.WHITE;
        };
    }
    
    /**
     * Invalidate cached components (call after any property change).
     */
    public void invalidateCache()
    {
        cachedColoredTag = null;
        cachedColoredName = null;
        cachedColoredLoginMessage = null;
    }
    
    // ========================================================================
    // Permission Methods
    // ========================================================================
    
    public boolean hasPermission(String permission)
    {
        return resolvedPermissions.contains(permission) || resolvedPermissions.contains("*");
    }
    
    /**
     * Add a permission to this rank.
     */
    public void addPermission(String permission)
    {
        permissions.add(permission.toLowerCase());
    }
    
    /**
     * Remove a permission from this rank.
     */
    public void removePermission(String permission)
    {
        permissions.remove(permission.toLowerCase());
    }
    
    /**
     * Check if this rank is at least as high as another rank.
     */
    public boolean isAtLeast(CustomRank other)
    {
        if (other == null) return true;
        return this.level >= other.level;
    }
    
    /**
     * Check if this rank is at least as high as a legacy Rank.
     */
    public boolean isAtLeast(Rank legacyRank)
    {
        if (legacyRank == null) return true;
        return this.level >= legacyRank.getLevel();
    }
    
    // ========================================================================
    // Displayable Implementation
    // ========================================================================
    
    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public String getTag()
    {
        if (abbreviation == null || abbreviation.isEmpty())
        {
            return "";
        }
        return "[" + abbreviation + "]";
    }
    
    @Override
    public NamedTextColor getColor()
    {
        return color;
    }
    
    @Override
    public Component getColoredName()
    {
        if (cachedColoredName == null)
        {
            cachedColoredName = AdventureUtil.format(name).colorIfAbsent(color);
        }
        return cachedColoredName;
    }
    
    @Override
    public Component getColoredTag()
    {
        if (cachedColoredTag == null)
        {
            if (prefix != null && !prefix.isEmpty())
            {
                cachedColoredTag = AdventureUtil.format(prefix.trim());
            }
            else if (abbreviation == null || abbreviation.isEmpty())
            {
                cachedColoredTag = Component.empty();
            }
            else
            {
                Component abbrevComponent = AdventureUtil.format(abbreviation).colorIfAbsent(color);

                cachedColoredTag = Component.text("[")
                        .color(NamedTextColor.DARK_GRAY)
                        .append(abbrevComponent)
                        .append(Component.text("]").color(NamedTextColor.DARK_GRAY));
            }
        }
        return cachedColoredTag;
    }
    
    @Override
    public Component getColoredLoginMessage()
    {
        if (cachedColoredLoginMessage == null)
        {
            Component nameComponent = AdventureUtil.format(name)
                    .colorIfAbsent(color)
                    .decorate(TextDecoration.ITALIC);
            cachedColoredLoginMessage = Component.text(determiner + " ").append(nameComponent);
        }
        return cachedColoredLoginMessage;
    }
    
    // ========================================================================
    // Comparable Implementation
    // ========================================================================
    
    @Override
    public int compareTo(CustomRank other)
    {
        return Integer.compare(this.level, other.level);
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!(obj instanceof CustomRank other)) return false;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode()
    {
        return id.hashCode();
    }
    
    @Override
    public String toString()
    {
        return "CustomRank{id=" + id + ", name=" + name + ", level=" + level + "}";
    }
    
    // ========================================================================
    // Getters that Lombok might miss
    // ========================================================================
    
    public String getId()
    {
        return id;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDeterminer()
    {
        return determiner;
    }

    public void setDeterminer(String determiner)
    {
        this.determiner = determiner;
    }

    public String getAbbreviation()
    {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation)
    {
        this.abbreviation = abbreviation;
    }

    public int getLevel()
    {
        return level;
    }

    public void setLevel(int level)
    {
        this.level = level;
    }

    public void setColor(NamedTextColor color)
    {
        this.color = color;
    }

    public boolean isAdmin()
    {
        return admin;
    }

    public void setAdmin(boolean admin)
    {
        this.admin = admin;
    }

    public boolean isConsoleOnly()
    {
        return consoleOnly;
    }

    public void setConsoleOnly(boolean consoleOnly)
    {
        this.consoleOnly = consoleOnly;
    }

    public Set<String> getPermissions()
    {
        return permissions;
    }
    
    public String getPrefix()
    {
        return prefix;
    }
    
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }
    
    public String getFormattedPrefix()
    {
        if (prefix == null || prefix.isEmpty())
        {
            return "";
        }
        return AdventureUtil.translateAlternateColorCodes(prefix);
    }
    
    public String getInheritFrom()
    {
        return inheritFrom;
    }
    
    public void setInheritFrom(String inheritFrom)
    {
        this.inheritFrom = inheritFrom;
    }
    
    public Set<String> getResolvedPermissions()
    {
        return resolvedPermissions;
    }
    
    public void setResolvedPermissions(Set<String> resolvedPermissions)
    {
        this.resolvedPermissions = resolvedPermissions;
    }
}
