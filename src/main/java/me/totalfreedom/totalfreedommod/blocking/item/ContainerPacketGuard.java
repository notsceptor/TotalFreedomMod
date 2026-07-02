package me.totalfreedom.totalfreedommod.blocking.item;

import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;

public final class ContainerPacketGuard
{

    private static final int MAX_NODES = 2048;
    private static final int MAX_DEPTH = 16;
    private static final int MAX_STRING_LENGTH = 4096;
    private static final String[] ITEM_BEARING_TAGS = {"Items", "item", "Book", "RecordItem"};

    private ContainerPacketGuard()
    {
    }

    public static boolean isItemBearingBlockEntity(NBTCompound nbt)
    {
        if (nbt == null)
        {
            return false;
        }
        for (String tag : ITEM_BEARING_TAGS)
        {
            if (nbt.getTags().containsKey(tag))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isUnsafe(NBTCompound nbt)
    {
        if (nbt == null)
        {
            return false;
        }
        Budget budget = new Budget();
        for (String tag : ITEM_BEARING_TAGS)
        {
            NBT child = nbt.getTags().get(tag);
            if (child != null && !walk(child, budget, 0))
            {
                return true;
            }
        }
        return false;
    }

    public static int sanitizeColumn(Column column)
    {
        if (column == null || column.getTileEntities() == null)
        {
            return 0;
        }
        int changed = 0;
        for (TileEntity tileEntity : column.getTileEntities())
        {
            if (tileEntity == null)
            {
                continue;
            }
            NBTCompound nbt = tileEntity.getNBT();
            if (isItemBearingBlockEntity(nbt) && isUnsafe(nbt))
            {
                neutralize(nbt);
                changed++;
            }
        }
        return changed;
    }

    static void neutralize(NBTCompound nbt)
    {
        if (nbt == null)
        {
            return;
        }
        for (String tag : ITEM_BEARING_TAGS)
        {
            nbt.removeTag(tag);
        }
    }

    private static boolean walk(NBT nbt, Budget budget, int depth)
    {
        if (nbt == null)
        {
            return true;
        }
        if (depth > MAX_DEPTH || !budget.consume())
        {
            return false;
        }
        if (nbt instanceof NBTCompound compound)
        {
            if (compound.getTags().containsKey("click_event"))
            {
                return false;
            }
            for (NBT child : compound.getTags().values())
            {
                if (!walk(child, budget, depth + 1))
                {
                    return false;
                }
            }
        }
        else if (nbt instanceof NBTList<?> list)
        {
            for (Object child : list.getTags())
            {
                if (child instanceof NBT childNbt && !walk(childNbt, budget, depth + 1))
                {
                    return false;
                }
            }
        }
        else if (nbt instanceof NBTString string)
        {
            return !stringUnsafe(string.getValue());
        }
        return true;
    }

    private static boolean stringUnsafe(String value)
    {
        if (value == null)
        {
            return false;
        }
        if (value.length() > MAX_STRING_LENGTH)
        {
            return true;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);
            if (escaped)
            {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString)
            {
                escaped = true;
                continue;
            }
            if (c == '"')
            {
                inString = !inString;
                continue;
            }
            if (inString)
            {
                continue;
            }
            if (c == '{' || c == '[')
            {
                if (++depth > MAX_DEPTH)
                {
                    return true;
                }
            }
            else if (c == '}' || c == ']')
            {
                depth = Math.max(0, depth - 1);
            }
        }
        return false;
    }

    private static final class Budget
    {
        private int remaining = MAX_NODES;

        private boolean consume()
        {
            return --remaining >= 0;
        }
    }
}
