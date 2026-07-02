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
            NBT value = nbt.getTags().get(tag);
            if (value != null && !walk(value, budget, 0))
            {
                return true;
            }
        }
        return false;
    }

    public static int sanitizeColumn(Column column)
    {
        if (column == null)
        {
            return 0;
        }
        TileEntity[] tileEntities = column.getTileEntities();
        if (tileEntities == null)
        {
            return 0;
        }
        int changed = 0;
        for (TileEntity tileEntity : tileEntities)
        {
            if (tileEntity == null)
            {
                continue;
            }
            NBTCompound nbt = tileEntity.getNBT();
            if (nbt == null || !isItemBearingBlockEntity(nbt))
            {
                continue;
            }
            if (isUnsafe(nbt))
            {
                neutralize(nbt);
                changed++;
            }
        }
        return changed;
    }

    static void neutralize(NBTCompound nbt)
    {
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
            if (inString)
            {
                if (escaped)
                {
                    escaped = false;
                }
                else if (c == '\\')
                {
                    escaped = true;
                }
                else if (c == '"')
                {
                    inString = false;
                }
                continue;
            }
            if (c == '"')
            {
                inString = true;
            }
            else if (c == '{' || c == '[')
            {
                if (++depth > MAX_DEPTH)
                {
                    return true;
                }
            }
            else if (c == '}' || c == ']')
            {
                if (depth > 0)
                {
                    depth--;
                }
            }
        }
        return false;
    }

    private static final class Budget
    {
        private int remainingNodes = MAX_NODES;

        private boolean consume()
        {
            return --remainingNodes >= 0;
        }
    }
}
