package me.totalfreedom.totalfreedommod.blocking.item;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import io.netty.buffer.ByteBuf;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSetGameRule;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.blocking.gamerule.GameRulePacketGuard;
import me.totalfreedom.totalfreedommod.blocking.sign.SignPacketGuard;
import me.totalfreedom.totalfreedommod.util.FLog;

final class ItemPacketListener extends PacketListenerAbstract
{

    private static final int MAX_PACKET_BYTES = 2_097_152;
    private static final int CHUNK_REENCODE_SAFE_BYTES = MAX_PACKET_BYTES - 256_000;

    private static final com.github.retrooper.packetevents.protocol.item.ItemStack EMPTY =
            com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY;

    private final TotalFreedomMod plugin;
    private final boolean sanitizeOutbound;
    private final PacketSpamLimiter spamLimiter;
    private final boolean signBlockEntityGuard;
    private final boolean signChunkGuard;
    private final boolean blockAllSignPackets;
    private final boolean gameRuleGuard;

    ItemPacketListener(TotalFreedomMod plugin, boolean sanitizeOutbound, PacketSpamLimiter spamLimiter,
                             boolean signBlockEntityGuard, boolean signChunkGuard, boolean blockAllSignPackets,
                             boolean gameRuleGuard)
    {
        super(PacketListenerPriority.HIGH);
        this.plugin = plugin;
        this.sanitizeOutbound = sanitizeOutbound;
        this.spamLimiter = spamLimiter;
        this.signBlockEntityGuard = signBlockEntityGuard;
        this.signChunkGuard = signChunkGuard;
        this.blockAllSignPackets = blockAllSignPackets;
        this.gameRuleGuard = gameRuleGuard;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event)
    {
        try
        {
            final PacketTypeCommon type = event.getPacketType();

            if (gameRuleGuard && type == PacketType.Play.Client.SET_GAME_RULE)
            {
                event.setCancelled(true);
                logBlockedGameRule(event);
                return;
            }

            if (spamLimiter == null)
            {
                return;
            }
            final UUID id = event.getUser().getUUID();
            if (id == null)
            {
                return;
            }
            if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                    || type == PacketType.Play.Client.USE_ITEM
                    || type == PacketType.Play.Client.PLAYER_DIGGING
                    || type == PacketType.Play.Client.INTERACT_ENTITY
                    || type == PacketType.Play.Client.ANIMATION
                    || type == PacketType.Play.Client.HELD_ITEM_CHANGE
                    || type == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION
                    || type == PacketType.Play.Client.ENTITY_ACTION)
            {
                if (!spamLimiter.allowInteraction(id))
                {
                    event.setCancelled(true);
                }
            }
            else if (type == PacketType.Play.Client.PLAYER_POSITION
                    || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                    || type == PacketType.Play.Client.PLAYER_ROTATION
                    || type == PacketType.Play.Client.PLAYER_FLYING
                    || type == PacketType.Play.Client.VEHICLE_MOVE)
            {
                if (!spamLimiter.allowMovement(id))
                {
                    event.setCancelled(true);
                }
            }
            else if (type == PacketType.Play.Client.CHAT_COMMAND
                    || type == PacketType.Play.Client.CHAT_COMMAND_UNSIGNED
                    || type == PacketType.Play.Client.CHAT_MESSAGE)
            {
                if (!spamLimiter.allowChat(id))
                {
                    event.setCancelled(true);
                }
            }
        }
        catch (Throwable ignored)
        {
        }
    }

    private void logBlockedGameRule(PacketReceiveEvent event)
    {
        String what;
        try
        {
            what = GameRulePacketGuard.describe(new WrapperPlayClientSetGameRule(event));
        }
        catch (Throwable t)
        {
            what = "?";
        }
        FLog.warning("[GameRuleGuard] Blocked an attempt to edit a gamerule from " + describeUser(event) + ": " + what);
    }

    private static String describeUser(PacketReceiveEvent event)
    {
        try
        {
            String name = event.getUser().getName();
            if (name != null)
            {
                return name;
            }
            UUID id = event.getUser().getUUID();
            return id == null ? "unknown" : id.toString();
        }
        catch (Throwable t)
        {
            return "unknown";
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event)
    {
        try
        {
            final PacketTypeCommon type = event.getPacketType();

            if (sanitizeOutbound)
            {
                if (type == PacketType.Play.Server.ENTITY_EQUIPMENT)
                {
                    handleEntityEquipment(event);
                    return;
                }
                if (type == PacketType.Play.Server.SET_SLOT)
                {
                    handleSetSlot(event);
                    return;
                }
                if (type == PacketType.Play.Server.WINDOW_ITEMS)
                {
                    handleWindowItems(event);
                    return;
                }
                if (type == PacketType.Play.Server.ENTITY_METADATA)
                {
                    handleEntityMetadata(event);
                    return;
                }
            }

            if ((blockAllSignPackets || signBlockEntityGuard) && type == PacketType.Play.Server.BLOCK_ENTITY_DATA)
            {
                handleBlockEntityData(event);
            }
            else if ((blockAllSignPackets || signChunkGuard) && type == PacketType.Play.Server.CHUNK_DATA)
            {
                handleChunkData(event);
            }
        }
        catch (Throwable ignored)
        {
        }
    }

    private void handleBlockEntityData(PacketSendEvent event)
    {
        WrapperPlayServerBlockEntityData wrapper = new WrapperPlayServerBlockEntityData(event);
        NBTCompound nbt = wrapper.getNBT();
        if (!SignPacketGuard.isSignBlockEntity(nbt))
        {
            return;
        }
        if (blockAllSignPackets || SignPacketGuard.isUnsafe(nbt))
        {
            event.setCancelled(true);
        }
    }

    private void handleChunkData(PacketSendEvent event)
    {
        // Decoding and re-encoding CHUNK_DATA is what blanks cursed sign tile-entities,
        // but it can also push an already-large chunk over the 2 MiB protocol cap and
        // kick the client (common on 1.12.x via ViaVersion). Other guards — the block-
        // entity packet filter, chunk-load scan, and proactive sweep — still cover these
        // players when we skip a packet here.
        if (!safeToTouchChunkPacket(event))
        {
            return;
        }

        WrapperPlayServerChunkData wrapper = new WrapperPlayServerChunkData(event);
        if (blockAllSignPackets)
        {
            int stripped = SignPacketGuard.stripAllSignsInColumn(wrapper.getColumn());
            if (stripped > 0)
            {
                event.markForReEncode(true);
            }
            return;
        }
        int neutralized = SignPacketGuard.sanitizeColumn(wrapper.getColumn());
        if (neutralized > 0)
        {
            event.markForReEncode(true);
        }
    }

    private static boolean safeToTouchChunkPacket(PacketSendEvent event)
    {
        ClientVersion client = event.getUser().getClientVersion();
        if (client != null && client.isOlderThanOrEquals(ClientVersion.V_1_12_2))
        {
            return false;
        }
        int bytes = packetByteLength(event);
        return bytes == 0 || bytes < CHUNK_REENCODE_SAFE_BYTES;
    }

    private static int packetByteLength(PacketSendEvent event)
    {
        Object buf = event.getByteBuf();
        if (buf instanceof ByteBuf byteBuf)
        {
            return byteBuf.readableBytes();
        }
        return 0;
    }

    private void handleEntityEquipment(PacketSendEvent event)
    {
        WrapperPlayServerEntityEquipment wrapper = new WrapperPlayServerEntityEquipment(event);
        List<Equipment> equipment = wrapper.getEquipment();
        boolean dirty = false;
        for (Equipment slot : equipment)
        {
            if (isCursed(slot.getItem()))
            {
                slot.setItem(EMPTY);
                dirty = true;
            }
        }
        if (dirty)
        {
            wrapper.setEquipment(equipment);
            event.markForReEncode(true);
        }
    }

    private void handleSetSlot(PacketSendEvent event)
    {
        WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
        if (isCursed(wrapper.getItem()))
        {
            wrapper.setItem(EMPTY);
            event.markForReEncode(true);
        }
    }

    private void handleWindowItems(PacketSendEvent event)
    {
        WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
        boolean dirty = false;

        List<com.github.retrooper.packetevents.protocol.item.ItemStack> items = wrapper.getItems();
        for (int i = 0; i < items.size(); i++)
        {
            if (isCursed(items.get(i)))
            {
                items.set(i, EMPTY);
                dirty = true;
            }
        }
        if (dirty)
        {
            wrapper.setItems(items);
        }

        Optional<com.github.retrooper.packetevents.protocol.item.ItemStack> carried = wrapper.getCarriedItem();
        if (carried.isPresent() && isCursed(carried.get()))
        {
            wrapper.setCarriedItem(EMPTY);
            dirty = true;
        }

        if (dirty)
        {
            event.markForReEncode(true);
        }
    }

    private void handleEntityMetadata(PacketSendEvent event)
    {
        WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
        List<EntityData<?>> metadata = wrapper.getEntityMetadata();
        boolean dirty = false;
        for (EntityData<?> data : metadata)
        {
            if (sanitizeMetadataEntry(data))
            {
                dirty = true;
            }
        }
        if (dirty)
        {
            wrapper.setEntityMetadata(metadata);
            event.markForReEncode(true);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean sanitizeMetadataEntry(EntityData<?> data)
    {
        Object value = data.getValue();
        if (value instanceof com.github.retrooper.packetevents.protocol.item.ItemStack peItem)
        {
            if (isCursed(peItem))
            {
                ((EntityData) data).setValue(EMPTY);
                return true;
            }
            return false;
        }
        if (value instanceof Optional<?> opt
                && opt.isPresent()
                && opt.get() instanceof com.github.retrooper.packetevents.protocol.item.ItemStack peItem
                && isCursed(peItem))
        {
            ((EntityData) data).setValue(Optional.empty());
            return true;
        }
        return false;
    }

    private boolean isCursed(com.github.retrooper.packetevents.protocol.item.ItemStack peItem)
    {
        if (peItem == null || peItem.isEmpty())
        {
            return false;
        }
        org.bukkit.inventory.ItemStack bukkit;
        try
        {
            bukkit = SpigotConversionUtil.toBukkitItemStack(peItem);
        }
        catch (Throwable t)
        {
            return false;
        }
        return plugin.iv.isCursed(bukkit);
    }

}
