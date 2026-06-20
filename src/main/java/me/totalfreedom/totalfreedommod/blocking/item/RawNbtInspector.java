package me.totalfreedom.totalfreedommod.blocking.item;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded, codec-free inspection of an item's raw NBT components
 * ({@code minecraft:custom_data} and {@code minecraft:block_entity_data}).
 */
final class RawNbtInspector
{

    enum Result
    {
        CLEAN,
        OVERSIZED,
        ERROR,
        UNAVAILABLE
    }

    private static final String NBT_PACKAGE = "net.minecraft.nbt.";

    private static final boolean AVAILABLE;
    private static final Method AS_NMS_COPY;
    private static final Method ITEMSTACK_GET;
    private static final Object CUSTOM_DATA_TYPE;
    private static final Object BLOCK_ENTITY_DATA_TYPE;
    private static final Field CUSTOM_DATA_TAG_FIELD;

    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();

    static
    {
        boolean ok = false;
        Method asNmsCopy = null;
        Method itemStackGet = null;
        Object customDataType = null;
        Object blockEntityDataType = null;
        Field customDataTagField = null;

        try
        {
            String craftBase = org.bukkit.Bukkit.getServer().getClass().getPackage().getName();
            Class<?> craftItemStack = Class.forName(craftBase + ".inventory.CraftItemStack");
            asNmsCopy = craftItemStack.getMethod("asNMSCopy", org.bukkit.inventory.ItemStack.class);
            asNmsCopy.setAccessible(true);

            Class<?> nmsItemStack = asNmsCopy.getReturnType();
            Class<?> dataComponentType = Class.forName("net.minecraft.core.component.DataComponentType");
            itemStackGet = nmsItemStack.getMethod("get", dataComponentType);
            itemStackGet.setAccessible(true);

            Class<?> dataComponents = Class.forName("net.minecraft.core.component.DataComponents");
            customDataType = dataComponents.getField("CUSTOM_DATA").get(null);
            blockEntityDataType = dataComponents.getField("BLOCK_ENTITY_DATA").get(null);

            Class<?> customData = Class.forName("net.minecraft.world.item.component.CustomData");
            for (Field f : customData.getDeclaredFields())
            {
                if (f.getType().getName().equals("net.minecraft.nbt.CompoundTag"))
                {
                    f.setAccessible(true);
                    customDataTagField = f;
                    break;
                }
            }

            ok = customDataType != null
                    && blockEntityDataType != null
                    && customDataTagField != null;
        }
        catch (Throwable ignored)
        {
            ok = false;
        }

        AVAILABLE = ok;
        AS_NMS_COPY = asNmsCopy;
        ITEMSTACK_GET = itemStackGet;
        CUSTOM_DATA_TYPE = customDataType;
        BLOCK_ENTITY_DATA_TYPE = blockEntityDataType;
        CUSTOM_DATA_TAG_FIELD = customDataTagField;
    }

    private RawNbtInspector()
    {
    }

    static boolean isAvailable()
    {
        return AVAILABLE;
    }

    /**
     * @param maxNodes hard cap on total tag nodes (compounds, list entries,
     *                 collection/array elements) before the item is rejected
     * @param maxDepth hard cap on nesting depth; also bounds recursion so a
     *                 depth-bomb cannot overflow the stack
     */
    static Result inspect(org.bukkit.inventory.ItemStack item, int maxNodes, int maxDepth)
    {
        if (!AVAILABLE)
        {
            return Result.UNAVAILABLE;
        }
        try
        {
            Object nms = AS_NMS_COPY.invoke(null, item);
            if (nms == null)
            {
                return Result.CLEAN;
            }
            if (overBudget(ITEMSTACK_GET.invoke(nms, CUSTOM_DATA_TYPE), maxNodes, maxDepth)
                    || overBudget(ITEMSTACK_GET.invoke(nms, BLOCK_ENTITY_DATA_TYPE), maxNodes, maxDepth))
            {
                return Result.OVERSIZED;
            }
            return Result.CLEAN;
        }
        catch (Throwable t)
        {
            return Result.ERROR;
        }
    }

    private static boolean overBudget(Object customData, int maxNodes, int maxDepth) throws IllegalAccessException
    {
        if (customData == null)
        {
            return false;
        }
        Object tag = CUSTOM_DATA_TAG_FIELD.get(customData);
        if (tag == null)
        {
            return false;
        }
        Budget budget = new Budget(maxNodes, maxDepth);
        // walk() returns false the moment a limit is crossed.
        return !walk(tag, 0, budget);
    }

    private static boolean walk(Object node, int depth, Budget budget)
    {
        if (node == null)
        {
            return true;
        }
        if (depth > budget.maxDepth)
        {
            return false;
        }
        if (++budget.nodes > budget.maxNodes)
        {
            return false;
        }

        if (node instanceof Map<?, ?> map)
        {
            return walkValues(map.values(), map.size(), depth, budget);
        }
        if (node instanceof Collection<?> collection)
        {
            return walkValues(collection, collection.size(), depth, budget);
        }
        if (node instanceof CharSequence text)
        {
            budget.nodes += text.length();
            return budget.nodes <= budget.maxNodes;
        }

        for (Field field : tagFields(node.getClass()))
        {
            Object value;
            try
            {
                value = field.get(node);
            }
            catch (Throwable ignored)
            {
                continue;
            }
            if (value == null)
            {
                continue;
            }
            if (value instanceof Map<?, ?> map)
            {
                if (!walkValues(map.values(), map.size(), depth, budget))
                {
                    return false;
                }
            }
            else if (value instanceof Collection<?> collection)
            {
                if (!walkValues(collection, collection.size(), depth, budget))
                {
                    return false;
                }
            }
            else if (value.getClass().isArray())
            {
                budget.nodes += Array.getLength(value);
                if (budget.nodes > budget.maxNodes)
                {
                    return false;
                }
            }
            else if (value instanceof CharSequence text)
            {
                budget.nodes += text.length();
                if (budget.nodes > budget.maxNodes)
                {
                    return false;
                }
            }
            else if (isTag(value))
            {
                if (!walk(value, depth + 1, budget))
                {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean walkValues(Collection<?> values, int size, int depth, Budget budget)
    {
        budget.nodes += size;
        if (budget.nodes > budget.maxNodes)
        {
            return false;
        }
        for (Object value : values)
        {
            if (isTag(value) && !walk(value, depth + 1, budget))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isTag(Object value)
    {
        return value != null && value.getClass().getName().startsWith(NBT_PACKAGE);
    }

    private static Field[] tagFields(Class<?> type)
    {
        return FIELD_CACHE.computeIfAbsent(type, RawNbtInspector::collectFields);
    }

    private static Field[] collectFields(Class<?> type)
    {
        ArrayList<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass())
        {
            for (Field f : c.getDeclaredFields())
            {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive())
                {
                    continue;
                }
                try
                {
                    f.setAccessible(true);
                    fields.add(f);
                }
                catch (Throwable ignored)
                {
                    // Inaccessible field (module restriction); skip it.
                }
            }
        }
        return fields.toArray(new Field[0]);
    }

    private static final class Budget
    {
        private final int maxNodes;
        private final int maxDepth;
        private int nodes;

        private Budget(int maxNodes, int maxDepth)
        {
            this.maxNodes = maxNodes;
            this.maxDepth = maxDepth;
        }
    }
}
