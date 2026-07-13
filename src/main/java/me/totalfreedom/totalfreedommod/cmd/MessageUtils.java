package me.totalfreedom.totalfreedommod.cmd;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Utility class for formatting messages using Kyori Adventure MiniMessage.
 * Supports named colors, hex colors, click/hover events, and placeholders.
 * <p>
 * Color tags:
 * <p>
 * {@literal 
 *          <gold>text</gold>
 * 
 *          <color:#FF0000>text</color>
 *   }
 * <p>
 * Decoration tags:
 * <p>
 * {@literal
 *          <bold>text</bold>
 * 
 *          <italic>text</italic>
 * 
 *          <underlined>text</underlined>
 * 
 *          <strikethrough>text</strikethrough>
 *   }
 * <p>
 * Click/hover tags:
 * <p>
 * {@literal
 *          <click:run_command:'/stats'>text</click>
 * 
 *          <click:suggest_command:'/character '>text</click>
 * 
 *          <click:open_url:'https://example.com'>text</click>
 * 
 *          <click:copy_to_clipboard:'some value'>text</click>
 * 
 *          <hover:show_text:'<gold>Tooltip text</gold>'>text</hover>
 *   }
 * <p>
 * Placeholders (resolved at parse time):
 * <p>
 * {@literal
 *          MessageUtils.send(player, "<gold><name></gold>", Placeholder.unparsed("name", "Alice"));
 * 
 *          MessageUtils.unparsed("name", input)   → safe for player input
 * 
 *          MessageUtils.parsed("label", "<gold>trusted</gold>") → parses MiniMessage in the value
 *   }
 */
public class MessageUtils {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?)([ a-zA-Z_][a-zA-Z0-9_-]*)([^>]*)>");

    private MessageUtils() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Creates an unparsed placeholder.
     * 
     * @param key Placeholder key to be used in the MiniMessage string
     * @param value Value to be used as the placeholder
     * @return A TagResolver for the unparsed placeholder
     */
    public static TagResolver unparsed(String key, String value) {
        return Placeholder.unparsed(key, value);
    }

    /**
     * For internal use only. 
     * Parses the value as MiniMessage, so it can contain formatting tags. 
     * Use with trusted input only.
     * 
     * @param key Placeholder key to be used in the MiniMessage string
     * @param miniMessageValue MiniMessage string to be parsed as the placeholder value
     * @return A TagResolver for the parsed placeholder
     */
    public static TagResolver parsed(String key, String miniMessageValue) {
        return Placeholder.parsed(key, miniMessageValue);
    }

    /**
     * For internal use only. 
     * Accepts a Component as the placeholder value, allowing for complex formatting and events.
     * Use with trusted input only, as the Component may contain unescaped player input.
     * 
     * @param key Placeholder key to be used in the MiniMessage string
     * @param value Component to be used as the placeholder value
     * @return A TagResolver for the component placeholder
     */
    public static TagResolver component(String key, Component value) {
        return Placeholder.component(key, value);
    }

    /**
     * Parses a MiniMessage string into a Component. 
     * Automatically escapes angle brackets that are not valid MiniMessage tags (e.g., <exp>, <user1>).
     * 
     * @param miniMessage MiniMessage string to be parsed
     * @return Parsed Component
     */
    public static Component parse(String miniMessage) {
        if (miniMessage == null) {
            throw new IllegalArgumentException("MiniMessage string cannot be null");
        }
        String escaped = escapeUnknownTags(miniMessage);
        return MM.deserialize(autoCloseTags(escaped));
    }

    /**
     * Parses a MiniMessage string with additional TagResolvers.
     * Automatically escapes angle brackets that are not valid MiniMessage tags (e.g., <exp>, <user1>).
     * Useful for including placeholders in the MiniMessage string.
     * 
     * @param miniMessage MiniMessage string to be parsed
     * @param resolvers Additional TagResolvers to resolve placeholders in the MiniMessage
     * @return Parsed Component with placeholders resolved
     */
    public static Component parse(String miniMessage, TagResolver... resolvers) {
        if (miniMessage == null) {
            throw new IllegalArgumentException("MiniMessage string cannot be null");
        }
        String escaped = escapeUnknownTags(miniMessage, resolvers);
        return MM.deserialize(autoCloseTags(escaped), resolvers);
    }

    /**
     * Automatically closes any unclosed tags in a MiniMessage string.
     * Useful for ensuring that all tags are properly closed, even if the input is malformed.
     * Note that this method does not validate the tags or their syntax, it simply ensures that
     * for every opening tag there is a corresponding closing tag, and that they are properly nested.
     * 
     * @param input MiniMessage string to be processed
     * @return MiniMessage string with automatically closed tags
     */
    private static String autoCloseTags(String input) {
        if (input == null || input.isEmpty()) return input;

        StringBuilder out = new StringBuilder();
        Deque<String> openTags = new ArrayDeque<>();

        Matcher matcher = TAG_PATTERN.matcher(input);

        int last = 0;
        while (matcher.find()) {
            out.append(input, last, matcher.start());

            String slash = matcher.group(1);
            String tag = matcher.group(2).toLowerCase();
            String args = matcher.group(3);
            boolean selfClosing = args != null && args.trim().endsWith("/");

            if (slash.isEmpty() && !selfClosing) {
                out.append(matcher.group(0));

                if (!"reset".equals(tag)) {
                    openTags.push(tag);
                }
            } else if (!slash.isEmpty()) {
                if (openTags.contains(tag)) {
                    while (!openTags.isEmpty()) {
                        String top = openTags.pop();

                        if (isSpecialTag(top) && !top.equals(tag)) {
                            throw new IllegalStateException(
                                "You must close special tags properly. Found </" + tag + "> before closing <" + top + ">."
                            );
                        }

                        out.append("</").append(top).append(">");
                        if (top.equals(tag)) break;
                    }
                } else {
                    // Unmatched closing tag - append it as-is for validation by MiniMessage
                    out.append(matcher.group(0));
                }
            } else {
                out.append(matcher.group(0));
            }

            last = matcher.end();
        }

        out.append(input.substring(last));

        while (!openTags.isEmpty()) {
            String top = openTags.pop();
            if (isSpecialTag(top)) {
                throw new IllegalStateException(
                    "You must close special tags properly. Unclosed special tag <" + top + ">."
                );
            }
            out.append("</").append(top).append(">");
        }

        return out.toString();
    }

    /**
     * Sends a MiniMessage string to a CommandSender.
     * 
     * @param sender CommandSender to send the message to
     * @param miniMessage MiniMessage string to be sent
     */
    public static void send(CommandSender sender, String miniMessage) {
        sender.sendMessage(parse(miniMessage));
    }

    /**
     * Sends a MiniMessage string with placeholders to a CommandSender.
     * 
     * @param sender CommandSender to send the message to
     * @param miniMessage MiniMessage string to be sent
     * @param resolvers Additional TagResolvers to resolve placeholders in the MiniMessage
     */
    public static void send(CommandSender sender, String miniMessage, TagResolver... resolvers) {
        sender.sendMessage(parse(miniMessage, resolvers));
    }

    /**
     * Broadcasts a MiniMessage string to all players on the server.
     * 
     * @param miniMessage MiniMessage string to be broadcasted
     */
    public static void broadcast(String miniMessage) {
        Bukkit.broadcast(parse(miniMessage));
    }

    /**
     * Broadcasts a MiniMessage string with placeholders to all players on the server.
     * 
     * @param miniMessage MiniMessage string to be broadcasted
     * @param resolvers Additional TagResolvers to resolve placeholders in the MiniMessage
     */
    public static void broadcast(String miniMessage, TagResolver... resolvers) {
        Bukkit.broadcast(parse(miniMessage, resolvers));
    }

    /**
     * Sends a MiniMessage string to the console.
     * 
     * @param miniMessage MiniMessage string to be sent
     */
    public static void console(String miniMessage) {
        Bukkit.getConsoleSender().sendMessage(parse(miniMessage));
    }

    /**
     * Converts a Component into a plain text string, stripping all formatting and events.
     * 
     * @param component Component to be converted
     * @return Plain text string
     */
    public static String toPlainText(Component component) {
        return PLAIN.serialize(component);
    }

    /**
     * Converts a MiniMessage string into a plain text string, stripping all formatting and events.
     * 
     * @param miniMessage MiniMessage string to be converted
     * @return Plain text string
     */
    public static String toPlainText(String miniMessage) {
        return PLAIN.serialize(parse(miniMessage));
    }

    /**
     * Wraps text in a hex color tag. Accepts with or without '#'.
     * 
     * @param hex Hex color code
     * @param text Text to be wrapped in the color
     * @return Text wrapped in a hex color tag
     */
    public static String hex(String hex, String text) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        return "<color:#" + clean + ">" + text + "</color>";
    }

    /**
     * Escapes angle bracket sequences that are not valid MiniMessage tags.
     * Converts <exp> to \\<exp\\>, <user1> to \\<user1\\>, etc.
     * Valid MiniMessage tags are preserved.
     * 
     * @param input String potentially containing unknown tags
     * @return String with unknown tags escaped
     */
    private static String escapeUnknownTags(String input, TagResolver... tags) {
        if (input == null || input.isEmpty()) return input;

        StringBuilder out = new StringBuilder();
        Matcher matcher = TAG_PATTERN.matcher(input);

        int last = 0;
        while (matcher.find()) {
            out.append(input, last, matcher.start());

            String slash = matcher.group(1);
            String tagName = matcher.group(2);
            String args = matcher.group(3) != null ? matcher.group(3) : "";
            boolean selfClosing = args.trim().endsWith("/");

            // Check if this is a valid MiniMessage tag
            boolean isValid = isValidMiniMessageTag(slash, tagName, args, selfClosing, tags);

            if (isValid) {
                out.append(matcher.group(0));
            } else {
                out.append("\\<").append(slash).append(tagName).append(args).append("\\>");
            }

            last = matcher.end();
        }

        out.append(input.substring(last));
        return out.toString();
    }

    /**
     * Checks if a tag is a valid MiniMessage tag. 
     * This is to make sure that if someone is for example sending command usage that has /<command> in it, it doesn't get parsed.
     * 
     * @param slash Slash indicating if it's a closing tag
     * @param tagName Name of the tag
     * @param args Arguments of the tag (e.g., :#FF0000 for color)
     * @param selfClosing Whether the tag is self-closing (ends with /)
     * @return True if the tag is a valid MiniMessage tag, false otherwise
     */
    private static boolean isValidMiniMessageTag(String slash, String tagName, String args, boolean selfClosing, TagResolver... tags) {
        if (!slash.isEmpty()) {
            return true;
        }

        String lower = tagName.toLowerCase();
        if (isStyleTag(lower) || isSpecialTag(lower) || isManuallySpecifiedTag(lower, tags)) return true;

        return false;
    }

    /**
     * Checks if a tag is a named color (e.g., red, gold, gray).
     * 
     * @param tag Tag name
     * @return True if the tag is a named color, false otherwise
     */
    private static boolean isNamedColor(String tag) {
        final String[] namedColors = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white"
        };

        return Stream.of(namedColors).anyMatch(tag::equals);
    }

    /**
     * Checks if a tag is a text decoration (bold, italic, underlined, strikethrough, obfuscated).
     * 
     * @param tag Decoration tag name
     * @return True if the tag is a decoration, false otherwise
     */
    private static boolean isDecoration(String tag) {
        final String[] decorations = {
            "bold", "italic", "underlined", "strikethrough", "obfuscated"
        };

        return Stream.of(decorations).anyMatch(tag::equals);
    }

    /**
     * Checks if a tag is a special interactive tag that must be explicitly closed.
     * 
     * @param tag Tag name
     * @return True if the tag is a special tag, false otherwise
     */
    private static boolean isSpecialTag(String tag) {
        final String[] specialTags = {
            "click", "hover", "reset"
        };

        return Stream.of(specialTags).anyMatch(tag::equals);
    }

    /**
     * Checks if a tag is a style-only tag (colors, decorations).
     * Style tags can be nested and auto-closed when unclosed.
     * 
     * @param tag Tag name
     * @return True if the tag is a style tag, false otherwise
     */
    private static boolean isStyleTag(String tag) {
        return isNamedColor(tag) || isDecoration(tag) || "color".equals(tag);
    }

    /**
     * Checks if a tag is a special interactive tag that must be explicitly closed.
     *
     * @param tag Tag name
     * @return True if the tag is a special tag, false otherwise
     */
    private static boolean isManuallySpecifiedTag(String tag, TagResolver[] resolvers) {
        return Stream.of(resolvers).anyMatch(resolver -> resolver.has(tag));
    }

}
