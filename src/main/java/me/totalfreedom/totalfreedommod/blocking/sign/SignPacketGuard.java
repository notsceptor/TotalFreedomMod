package me.totalfreedom.totalfreedommod.blocking.sign;

import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;

/**
 * Outbound block-entity / chunk sanitizer for crash signs operating directly on
 * the packetevents NBT model.  Designed to guard against a client-side crash.
 * that hangs the server).
 * {@link ItemPacketListener} (itself PacketEvents-gated) is the
 * only caller.
 */
public final class SignPacketGuard
{

    private static final int MAX_NODES = 1024;
    private static final int MAX_DEPTH = 16;
    private static final int MAX_STRING_LENGTH = 4096;

    private SignPacketGuard()
    {
    }

    public static boolean isSignBlockEntity(NBTCompound nbt)
    {
        if (nbt == null)
        {
            return false;
        }
        NBT id = nbt.getTags().get("id");
        if (id instanceof NBTString string)
        {
            String value = string.getValue();
            return value.endsWith(":sign") || value.endsWith(":hanging_sign");
        }
        return nbt.getTags().containsKey("front_text") || nbt.getTags().containsKey("Text1");
    }

    public static int stripAllSignsInColumn(Column column)
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
            if (nbt == null)
            {
                continue;
            }
            if (isSignBlockEntity(nbt))
            {
                neutralize(nbt);
                changed++;
            }
        }
        return changed;
    }

    public static boolean isUnsafe(NBTCompound nbt)
    {
        if (nbt == null)
        {
            return false;
        }
        return !walk(nbt, new Budget(), 0);
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
            if (nbt == null || !isSignBlockEntity(nbt))
            {
                continue;
            }
            if (!walk(nbt, new Budget(), 0))
            {
                neutralize(nbt);
                changed++;
            }
        }
        return changed;
    }

    private static void neutralize(NBTCompound nbt)
    {
        nbt.removeTag("front_text");
        nbt.removeTag("back_text");
        nbt.removeTag("messages");
        nbt.removeTag("filtered_messages");
        nbt.removeTag("Text1");
        nbt.removeTag("Text2");
        nbt.removeTag("Text3");
        nbt.removeTag("Text4");
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

        /** @return {@code true} while the node budget still has capacity. */
        private boolean consume()
        {
            return --remainingNodes >= 0;
        }
    }
}
