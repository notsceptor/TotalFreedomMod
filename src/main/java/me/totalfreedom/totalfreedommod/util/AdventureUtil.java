package me.totalfreedom.totalfreedommod.util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
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

    private static final TagResolver COLOR_TAGS = TagResolver.resolver(
            StandardTags.color(),
            StandardTags.gradient(),
            StandardTags.rainbow(),
            StandardTags.transition(),
            StandardTags.pride());
    private static final TagResolver DECORATION_TAGS = StandardTags.decorations();

    private static final MiniMessage MM_FULL = MiniMessage.builder()
            .tags(TagResolver.resolver(COLOR_TAGS, DECORATION_TAGS, StandardTags.reset())).build();
    private static final MiniMessage MM_COLORS = MiniMessage.builder()
            .tags(TagResolver.resolver(COLOR_TAGS, StandardTags.reset())).build();
    private static final MiniMessage MM_DECORATIONS = MiniMessage.builder()
            .tags(TagResolver.resolver(DECORATION_TAGS, StandardTags.reset())).build();

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

    /**
     * Shared rich text formatter for display text (chat prefixes, tags, nicknames,
     * ...). Accepts legacy &/§ codes — including {@code &#rrggbb} and {@code &x&r&r&g&g&b&b}
     * (legacy hex) as well as MiniMessage.  Only non-interactive text is accepted.
     * 
     * @param input The raw text
     * @return The formatted Component
     */
    public static Component format(String input)
    {
        if (input == null)
        {
            return Component.empty();
        }
        return MM_FULL.deserialize(legacyToMiniMessage(input, true, true));
    }

    /**
     * Like {@link #format(String)} but for untrusted chat, gated by config: colours
     * (incl. hex/gradient/rainbow) and decorations are each enabled independently.
     *
     * @param input        The raw text
     * @param allowColors  Whether colour/hex/gradient/rainbow tags are permitted
     * @param allowSpecial Whether decoration tags are permitted
     * @return The formatted Component
     */
    public static Component formatChat(String input, boolean allowColors, boolean allowSpecial)
    {
        if (input == null)
        {
            return Component.empty();
        }
        if (!allowColors && !allowSpecial)
        {
            return Component.text(stripColor(input));
        }
        final String mini = legacyToMiniMessage(input, allowColors, allowSpecial);
        if (allowColors && allowSpecial)
        {
            return MM_FULL.deserialize(mini);
        }
        return (allowColors ? MM_COLORS : MM_DECORATIONS).deserialize(mini);
    }

    /**
     * Converts legacy &/§ colour and format codes (including {@code &#rrggbb} and the
     * {@code &x&r&r&g&g&b&b} hex form) into their MiniMessage equivalents, leaving any
     * literal text and existing MiniMessage tags untouched.  Codes whose category is
     * disabled are ignored.
     *
     * @param text         The text containing legacy codes
     * @param allowColors  Whether colour/hex codes are converted (otherwise dropped)
     * @param allowSpecial Whether decoration codes are converted (otherwise dropped)
     * @return Text with legacy codes converted to MiniMessage tags
     */
    public static String legacyToMiniMessage(String text, boolean allowColors, boolean allowSpecial)
    {
        if (text == null)
        {
            return "";
        }
        final StringBuilder out = new StringBuilder(text.length());
        final int len = text.length();
        for (int i = 0; i < len; i++)
        {
            final char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < len)
            {
                final char next = text.charAt(i + 1);
                // &x&r&r&g&g&b&b hex form
                if (next == 'x' || next == 'X')
                {
                    final String hex = readSpigotHex(text, i);
                    if (hex != null)
                    {
                        if (allowColors)
                        {
                            out.append("<#").append(hex).append('>');
                        }
                        i += 13;
                        continue;
                    }
                }
                // &#rrggbb hex form
                if (next == '#' && i + 8 <= len && isHex(text, i + 2, 6))
                {
                    if (allowColors)
                    {
                        out.append("<#").append(text, i + 2, i + 8).append('>');
                    }
                    i += 7;
                    continue;
                }
                final char code = Character.toLowerCase(next);
                if (isColorChar(code))
                {
                    if (allowColors)
                    {
                        out.append('<').append(colorCodeName(code)).append('>');
                    }
                    i += 1;
                    continue;
                }
                if (isDecorationChar(code))
                {
                    if (allowSpecial)
                    {
                        out.append('<').append(colorCodeName(code)).append('>');
                    }
                    i += 1;
                    continue;
                }
                if (code == 'r')
                {
                    if (allowColors || allowSpecial)
                    {
                        out.append("<reset>");
                    }
                    i += 1;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * Recursively strips the obfuscated (magic) decoration from a component
     * tree. Used where obfuscated text must never be allowed (e.g. nicknames).
     *
     * @param component The component to clean
     * @return A component with obfuscation removed everywhere
     */
    public static Component removeObfuscation(Component component)
    {
        if (component == null)
        {
            return Component.empty();
        }
        Component result = component;
        if (result.decoration(TextDecoration.OBFUSCATED) == TextDecoration.State.TRUE)
        {
            result = result.decoration(TextDecoration.OBFUSCATED, TextDecoration.State.FALSE);
        }
        final List<Component> children = result.children();
        if (!children.isEmpty())
        {
            final List<Component> newChildren = new ArrayList<>(children.size());
            for (Component child : children)
            {
                newChildren.add(removeObfuscation(child));
            }
            result = result.children(newChildren);
        }
        return result;
    }

    private static boolean isColorChar(char c)
    {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }

    private static boolean isDecorationChar(char c)
    {
        return c == 'k' || c == 'l' || c == 'm' || c == 'n' || c == 'o';
    }

    private static boolean isHex(String s, int start, int count)
    {
        if (start + count > s.length())
        {
            return false;
        }
        for (int i = start; i < start + count; i++)
        {
            if (Character.digit(s.charAt(i), 16) < 0)
            {
                return false;
            }
        }
        return true;
    }

    // Reads a &x&r&r&g&g&b&b hex sequence beginning at index i (the & before x);
    // returns 6 hex digits (rrggbb) or NULL if the full pattern is not present.
    private static String readSpigotHex(String text, int i)
    {
        if (i + 13 >= text.length())
        {
            return null;
        }
        final StringBuilder hex = new StringBuilder(6);
        for (int p = 0; p < 6; p++)
        {
            final int base = i + 2 + p * 2;
            final char sym = text.charAt(base);
            final char dig = text.charAt(base + 1);
            if (sym != '&' && sym != '§')
            {
                return null;
            }
            if (Character.digit(dig, 16) < 0)
            {
                return null;
            }
            hex.append(dig);
        }
        return hex.toString();
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

