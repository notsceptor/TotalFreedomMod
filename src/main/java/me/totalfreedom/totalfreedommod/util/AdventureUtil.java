package me.totalfreedom.totalfreedommod.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;

public class AdventureUtil
{

    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private AdventureUtil()
    {
    }

    /**
     * Converts legacy color codes (&a, §a) to Component.
     *
     * @param legacy The legacy string with color codes
     * @return Component representation
     */
    public static Component legacyToComponent(String legacy)
    {
        if (legacy == null)
        {
            return Component.empty();
        }
        return LEGACY_AMPERSAND.deserialize(legacy);
    }

    /**
     * Converts Component back to legacy format for compatibility.
     *
     * @param component The Component to convert
     * @return Legacy string with color codes
     */
    public static String componentToLegacy(Component component)
    {
        if (component == null)
        {
            return "";
        }
        return LEGACY_AMPERSAND.serialize(component);
    }

    /**
     * Converts Component back to legacy format using § codes (for console/chat format).
     * Use this when the output needs § codes instead of & codes.
     *
     * @param component The Component to convert
     * @return Legacy string with § color codes
     */
    public static String componentToLegacySection(Component component)
    {
        if (component == null)
        {
            return "";
        }
        return LEGACY_SECTION.serialize(component);
    }

    /**
     * Creates a colored Component from text and color.
     *
     * @param text The text content
     * @param color The NamedTextColor
     * @return Component with color applied
     */
    public static Component colorize(String text, NamedTextColor color)
    {
        if (text == null)
        {
            return Component.empty();
        }
        Component component = Component.text(text);
        if (color != null)
        {
            component = component.color(color);
        }
        return component;
    }

    /**
     * Strips color codes from a string, returning plain text.
     * Replacement for ChatColor.stripColor()
     *
     * @param text The text with color codes
     * @return Plain text without color codes
     */
    public static String stripColor(String text)
    {
        if (text == null)
        {
            return "";
        }
        Component component = legacyToComponent(text);
        return PLAIN_TEXT.serialize(component);
    }

    /**
     * Translates alternate color codes (e.g., &a) to Component.
     * Replacement for ChatColor.translateAlternateColorCodes()
     *
     * @param altColorChar The alternate color character (usually '&')
     * @param text The text to translate
     * @return Component with colors applied
     */
    public static Component translateAlternateColorCodes(char altColorChar, String text)
    {
        if (text == null)
        {
            return Component.empty();
        }
        if (altColorChar == '&')
        {
            return LEGACY_AMPERSAND.deserialize(text);
        }
        else if (altColorChar == '§')
        {
            return LEGACY_SECTION.deserialize(text);
        }
        else
        {
            // For other characters, replace them with & first
            String replaced = text.replace(altColorChar, '&');
            return LEGACY_AMPERSAND.deserialize(replaced);
        }
    }

    /**
     * Converts ChatColor to NamedTextColor.
     *
     * @param chatColor The ChatColor to convert
     * @return Equivalent NamedTextColor, or null if no match
     */
    public static NamedTextColor chatColorToNamedTextColor(ChatColor chatColor)
    {
        if (chatColor == null)
        {
            return null;
        }

        // Map ChatColor to NamedTextColor
        if (chatColor == ChatColor.BLACK) return NamedTextColor.BLACK;
        if (chatColor == ChatColor.DARK_BLUE) return NamedTextColor.DARK_BLUE;
        if (chatColor == ChatColor.DARK_GREEN) return NamedTextColor.DARK_GREEN;
        if (chatColor == ChatColor.DARK_AQUA) return NamedTextColor.DARK_AQUA;
        if (chatColor == ChatColor.DARK_RED) return NamedTextColor.DARK_RED;
        if (chatColor == ChatColor.DARK_PURPLE) return NamedTextColor.DARK_PURPLE;
        if (chatColor == ChatColor.GOLD) return NamedTextColor.GOLD;
        if (chatColor == ChatColor.GRAY) return NamedTextColor.GRAY;
        if (chatColor == ChatColor.DARK_GRAY) return NamedTextColor.DARK_GRAY;
        if (chatColor == ChatColor.BLUE) return NamedTextColor.BLUE;
        if (chatColor == ChatColor.GREEN) return NamedTextColor.GREEN;
        if (chatColor == ChatColor.AQUA) return NamedTextColor.AQUA;
        if (chatColor == ChatColor.RED) return NamedTextColor.RED;
        if (chatColor == ChatColor.LIGHT_PURPLE) return NamedTextColor.LIGHT_PURPLE;
        if (chatColor == ChatColor.YELLOW) return NamedTextColor.YELLOW;
        if (chatColor == ChatColor.WHITE) return NamedTextColor.WHITE;
        return null;
    }

    /**
     * Converts NamedTextColor to legacy ChatColor for backward compatibility.
     *
     * @param namedTextColor The NamedTextColor to convert
     * @return Equivalent ChatColor, or null if no match
     */
    public static ChatColor namedTextColorToChatColor(NamedTextColor namedTextColor)
    {
        if (namedTextColor == null)
        {
            return null;
        }

        // Map NamedTextColor to ChatColor
        if (namedTextColor == NamedTextColor.BLACK) return ChatColor.BLACK;
        if (namedTextColor == NamedTextColor.DARK_BLUE) return ChatColor.DARK_BLUE;
        if (namedTextColor == NamedTextColor.DARK_GREEN) return ChatColor.DARK_GREEN;
        if (namedTextColor == NamedTextColor.DARK_AQUA) return ChatColor.DARK_AQUA;
        if (namedTextColor == NamedTextColor.DARK_RED) return ChatColor.DARK_RED;
        if (namedTextColor == NamedTextColor.DARK_PURPLE) return ChatColor.DARK_PURPLE;
        if (namedTextColor == NamedTextColor.GOLD) return ChatColor.GOLD;
        if (namedTextColor == NamedTextColor.GRAY) return ChatColor.GRAY;
        if (namedTextColor == NamedTextColor.DARK_GRAY) return ChatColor.DARK_GRAY;
        if (namedTextColor == NamedTextColor.BLUE) return ChatColor.BLUE;
        if (namedTextColor == NamedTextColor.GREEN) return ChatColor.GREEN;
        if (namedTextColor == NamedTextColor.AQUA) return ChatColor.AQUA;
        if (namedTextColor == NamedTextColor.RED) return ChatColor.RED;
        if (namedTextColor == NamedTextColor.LIGHT_PURPLE) return ChatColor.LIGHT_PURPLE;
        if (namedTextColor == NamedTextColor.YELLOW) return ChatColor.YELLOW;
        if (namedTextColor == NamedTextColor.WHITE) return ChatColor.WHITE;
        return null;
    }

    /**
     * Applies text decoration to a Component.
     *
     * @param component The Component to decorate
     * @param decoration The TextDecoration to apply
     * @return Component with decoration applied
     */
    public static Component decorate(Component component, TextDecoration decoration)
    {
        if (component == null)
        {
            return Component.empty();
        }
        return component.decorate(decoration);
    }

    /**
     * Checks if a ChatColor represents a formatting code (bold, italic, etc.).
     *
     * @param chatColor The ChatColor to check
     * @return True if it's a formatting code
     */
    public static boolean isFormat(ChatColor chatColor)
    {
        if (chatColor == null)
        {
            return false;
        }
        return chatColor == ChatColor.BOLD
                || chatColor == ChatColor.ITALIC
                || chatColor == ChatColor.UNDERLINE
                || chatColor == ChatColor.STRIKETHROUGH
                || chatColor == ChatColor.MAGIC
                || chatColor == ChatColor.RESET;
    }

    /**
     * Converts ChatColor formatting codes to TextDecoration.
     *
     * @param chatColor The ChatColor formatting code
     * @return Equivalent TextDecoration, or null if not a format code
     */
    public static TextDecoration chatColorToTextDecoration(ChatColor chatColor)
    {
        if (chatColor == null)
        {
            return null;
        }

        switch (chatColor)
        {
            case BOLD:
                return TextDecoration.BOLD;
            case ITALIC:
                return TextDecoration.ITALIC;
            case UNDERLINE:
                return TextDecoration.UNDERLINED;
            case STRIKETHROUGH:
                return TextDecoration.STRIKETHROUGH;
            case MAGIC:
                return TextDecoration.OBFUSCATED;
            default:
                return null;
        }
    }
}

