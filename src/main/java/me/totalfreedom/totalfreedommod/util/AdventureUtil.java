package me.totalfreedom.totalfreedommod.util;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;

public class AdventureUtil
{

    private static LegacyComponentSerializer legacySerializer(char character)
    {
        return LegacyComponentSerializer.builder()
                .character(character)
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
    }

    private static final LegacyComponentSerializer LEGACY_AMPERSAND = legacySerializer('&');
    private static final LegacyComponentSerializer LEGACY_SECTION = legacySerializer('\u00A7');
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)[-a-z0-9+&@#/%?=~_|!:,.;]*[-a-z0-9+&@#/%=~_|]");

    private static final Map<ChatColor, NamedTextColor> CHAT_COLOR_TO_NAMED_TEXT_COLOR = new EnumMap<ChatColor, NamedTextColor>(ChatColor.class);
    private static final Map<NamedTextColor, ChatColor> NAMED_TEXT_COLOR_TO_CHAT_COLOR = new HashMap<NamedTextColor, ChatColor>();

    static
    {
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.BLACK, NamedTextColor.BLACK);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.DARK_BLUE, NamedTextColor.DARK_BLUE);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.DARK_GREEN, NamedTextColor.DARK_GREEN);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.DARK_AQUA, NamedTextColor.DARK_AQUA);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.DARK_RED, NamedTextColor.DARK_RED);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.DARK_PURPLE, NamedTextColor.DARK_PURPLE);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.GOLD, NamedTextColor.GOLD);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.GRAY, NamedTextColor.GRAY);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.DARK_GRAY, NamedTextColor.DARK_GRAY);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.BLUE, NamedTextColor.BLUE);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.GREEN, NamedTextColor.GREEN);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.AQUA, NamedTextColor.AQUA);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.RED, NamedTextColor.RED);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.LIGHT_PURPLE, NamedTextColor.LIGHT_PURPLE);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.YELLOW, NamedTextColor.YELLOW);
        CHAT_COLOR_TO_NAMED_TEXT_COLOR.put(ChatColor.WHITE, NamedTextColor.WHITE);

        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.BLACK, ChatColor.BLACK);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.DARK_BLUE, ChatColor.DARK_BLUE);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.DARK_GREEN, ChatColor.DARK_GREEN);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.DARK_AQUA, ChatColor.DARK_AQUA);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.DARK_RED, ChatColor.DARK_RED);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.DARK_PURPLE, ChatColor.DARK_PURPLE);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.GOLD, ChatColor.GOLD);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.GRAY, ChatColor.GRAY);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.DARK_GRAY, ChatColor.DARK_GRAY);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.BLUE, ChatColor.BLUE);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.GREEN, ChatColor.GREEN);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.AQUA, ChatColor.AQUA);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.RED, ChatColor.RED);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.LIGHT_PURPLE, ChatColor.LIGHT_PURPLE);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.YELLOW, ChatColor.YELLOW);
        NAMED_TEXT_COLOR_TO_CHAT_COLOR.put(NamedTextColor.WHITE, ChatColor.WHITE);
    }

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

    public static Component addLinks(Component component)
    {
        if (component == null)
        {
            return Component.empty();
        }
        return component.replaceText(builder -> builder
                .match(URL_PATTERN)
                .replacement((result, text) ->
                {
                    final String url = result.group();
                    final String href = url.regionMatches(true, 0, "http", 0, 4) ? url : "https://" + url;
                    return text.clickEvent(ClickEvent.openUrl(href))
                            .hoverEvent(HoverEvent.showText(Component.text("Open " + href)))
                            .decorate(TextDecoration.UNDERLINED);
                }));
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

    public static String componentToPlainText(Component component)
    {
        if (component == null)
        {
            return "";
        }
        return PLAIN_TEXT.serialize(component);
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
     * Translates ampersand (&) color codes to legacy section (§) codes.
     * Replacement for ChatColor.translateAlternateColorCodes('&', text).
     *
     * @param text The text containing &-codes
     * @return Text with section (§) color codes
     */
    public static String translateAlternateColorCodes(String text)
    {
        if (text == null)
        {
            return "";
        }
        return LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(text));
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

    private static String colorCodeName(char c)
    {
        switch (c)
        {
            case '0': return "black";
            case '1': return "dark_blue";
            case '2': return "dark_green";
            case '3': return "dark_aqua";
            case '4': return "dark_red";
            case '5': return "dark_purple";
            case '6': return "gold";
            case '7': return "gray";
            case '8': return "dark_gray";
            case '9': return "blue";
            case 'a': return "green";
            case 'b': return "aqua";
            case 'c': return "red";
            case 'd': return "light_purple";
            case 'e': return "yellow";
            case 'f': return "white";
            case 'k': return "obfuscated";
            case 'l': return "bold";
            case 'm': return "strikethrough";
            case 'n': return "underlined";
            case 'o': return "italic";
            case 'r': return "reset";
        }
        throw new IllegalArgumentException("Character supplied is not within range of valid character codes: '" + c + "'");
    }

    public static String translateAlternateColorCodesToMiniMessage(String text)
    {
        final Pattern pat = Pattern.compile("&([0-9a-fl-okr])");
        final Matcher m = pat.matcher(text);
        return m.replaceAll(result -> String.format("<%s>", colorCodeName(result.group(1).charAt(0))));
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
        return CHAT_COLOR_TO_NAMED_TEXT_COLOR.get(chatColor);
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
        return NAMED_TEXT_COLOR_TO_CHAT_COLOR.get(namedTextColor);
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

