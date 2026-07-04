package me.totalfreedom.totalfreedommod.blocking.spawner;

import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;

public final class SpawnerPacketGuard
{

    private static final int MAX_NODES = 1024;
    private static final int MAX_DEPTH = 16;
    private static final int MAX_STRING_LENGTH = 4096;

    private SpawnerPacketGuard()
    {
    }

    public static boolean isSpawnerBlockEntity(NBTCompound nbt)
    {
        if (nbt == null)
        {
            return false;
        }
        NBT id = nbt.getTags().get("id");
        if (id instanceof NBTString string)
        {
            return string.getValue().endsWith(":mob_spawner");
        }
        return nbt.getTags().containsKey("SpawnData") || nbt.getTags().containsKey("SpawnPotentials");
    }

    public static boolean isUnsafe(NBTCompound nbt)
    {
        if (nbt == null)
        {
            return false;
        }
        return previewsHanging(nbt) || !walk(nbt, new Budget(), 0);
    }

    public static int stripAllSpawnersInColumn(Column column)
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
            if (isSpawnerBlockEntity(nbt))
            {
                neutralize(nbt);
                changed++;
            }
        }
        return changed;
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
            if (nbt == null || !isSpawnerBlockEntity(nbt))
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

    private static void neutralize(NBTCompound nbt)
    {
        nbt.removeTag("SpawnData");
        nbt.removeTag("SpawnPotentials");
    }

    private static boolean previewsHanging(NBTCompound nbt)
    {
        NBT spawnData = nbt.getTags().get("SpawnData");
        if (spawnData instanceof NBTCompound compound && entityIsHanging(compound.getTags().get("entity")))
        {
            return true;
        }
        NBT potentials = nbt.getTags().get("SpawnPotentials");
        if (potentials instanceof NBTList<?> list)
        {
            for (Object child : list.getTags())
            {
                if (child instanceof NBTCompound entry
                        && entry.getTags().get("data") instanceof NBTCompound data
                        && entityIsHanging(data.getTags().get("entity")))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean entityIsHanging(NBT entityTag)
    {
        if (!(entityTag instanceof NBTCompound entity))
        {
            return false;
        }
        if (!(entity.getTags().get("id") instanceof NBTString string))
        {
            return false;
        }
        return isHangingId(string.getValue());
    }

    private static boolean isHangingId(String id)
    {
        if (id == null)
        {
            return false;
        }
        String s = id.toLowerCase();
        int colon = s.lastIndexOf(':');
        if (colon >= 0)
        {
            s = s.substring(colon + 1);
        }
        return switch (s)
        {
            case "painting", "item_frame", "glow_item_frame", "leash_knot" -> true;
            default -> false;
        };
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

        /** @return {@code true} while the node budget still has capacity. */
        private boolean consume()
        {
            return --remainingNodes >= 0;
        }
    }
}
