package me.totalfreedom.totalfreedommod.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Bounded-cost inspection of Adventure {@link Component} graphs.
 */
public final class ComponentScanner
{

    private ComponentScanner()
    {
    }

    public static int safeNodeCount(Component root, int maxNodes)
    {
        if (root == null)
        {
            return 0;
        }
        IdentityHashMap<Component, Boolean> seen = new IdentityHashMap<>();
        Deque<Component> stack = new ArrayDeque<>();
        stack.push(root);
        int count = 0;
        while (!stack.isEmpty())
        {
            Component current = stack.pop();
            if (seen.put(current, Boolean.TRUE) != null)
            {
                return -1;
            }
            count++;
            if (count > maxNodes)
            {
                return -1;
            }
            if (current instanceof TranslatableComponent translatable
                    && (!translatable.arguments().isEmpty() || containsFormatSpecifier(translatable.key())))
            {
                return -1;
            }
            for (Component child : current.children())
            {
                stack.push(child);
            }
            if (current instanceof TranslatableComponent translatable)
            {
                for (TranslationArgument arg : translatable.arguments())
                {
                    Object value = arg.value();
                    if (value instanceof Component vc)
                    {
                        stack.push(vc);
                    }
                }
            }
        }
        return count;
    }

    public static boolean isUnsafe(Component root, int maxNodes)
    {
        return safeNodeCount(root, maxNodes) < 0;
    }

    public static boolean isCursed(Component root, int maxNodes)
    {
        return isUnsafe(root, maxNodes) || hasClickEvent(root, maxNodes);
    }

    /**
     * Returns true when the component graph contains a {@code clickEvent}, which
     * lets clients run commands as the interacting player.
     */
    public static boolean hasClickEvent(Component root, int maxNodes)
    {
        if (root == null)
        {
            return false;
        }
        if (safeNodeCount(root, maxNodes) < 0)
        {
            return true;
        }
        IdentityHashMap<Component, Boolean> seen = new IdentityHashMap<>();
        Deque<Component> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty())
        {
            Component current = stack.pop();
            if (seen.put(current, Boolean.TRUE) != null)
            {
                continue;
            }
            if (current.clickEvent() != null)
            {
                return true;
            }
            for (Component child : current.children())
            {
                stack.push(child);
            }
            if (current instanceof TranslatableComponent translatable)
            {
                for (TranslationArgument arg : translatable.arguments())
                {
                    Object value = arg.value();
                    if (value instanceof Component vc)
                    {
                        stack.push(vc);
                    }
                }
            }
        }
        return false;
    }

    private static boolean containsFormatSpecifier(String key)
    {
        if (key == null || key.indexOf('%') < 0)
        {
            return false;
        }
        return key.replace("%%", "").indexOf('%') >= 0;
    }

    public static int safePlainTextLength(Component root, int maxNodes)
    {
        if (safeNodeCount(root, maxNodes) < 0)
        {
            return -1;
        }
        return PlainTextComponentSerializer.plainText().serialize(root).length();
    }
}
