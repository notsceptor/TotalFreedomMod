package me.totalfreedom.totalfreedommod.blocking.entity;

import com.github.retrooper.packetevents.protocol.attribute.Attribute;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataType;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import java.util.List;
import java.util.Optional;
import me.totalfreedom.totalfreedommod.util.ComponentScanner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public final class EntityMetaPacketGuard
{

    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    private EntityMetaPacketGuard()
    {
    }

    public record Limits(int maxNameLength, int maxComponentNodes, double maxScale)
    {
    }

    public static boolean sanitizeMetadata(List<EntityData<?>> metadata, Limits limits)
    {
        if (metadata == null || metadata.isEmpty() || limits == null)
        {
            return false;
        }
        boolean dirty = false;
        for (EntityData<?> data : metadata)
        {
            if (sanitizeMetadataEntry(data, limits))
            {
                dirty = true;
            }
        }
        return dirty;
    }

    public static boolean sanitizeAttributes(List<WrapperPlayServerUpdateAttributes.Property> properties, double maxScale)
    {
        if (properties == null || properties.isEmpty() || maxScale <= 0.0D)
        {
            return false;
        }
        boolean dirty = false;
        for (WrapperPlayServerUpdateAttributes.Property property : properties)
        {
            if (property == null || !isScaleAttribute(property.getAttribute()))
            {
                continue;
            }
            double value = clampScaleMagnitude(property.getValue(), maxScale);
            if (value != property.getValue())
            {
                property.setValue(value);
                dirty = true;
            }
            for (WrapperPlayServerUpdateAttributes.PropertyModifier modifier : property.getModifiers())
            {
                if (modifier == null)
                {
                    continue;
                }
                double amount = clampScaleMagnitude(modifier.getAmount(), maxScale);
                if (amount != modifier.getAmount())
                {
                    modifier.setAmount(amount);
                    dirty = true;
                }
            }
        }
        return dirty;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean sanitizeMetadataEntry(EntityData<?> data, Limits limits)
    {
        if (data == null)
        {
            return false;
        }
        EntityDataType<?> type = data.getType();
        if (!isNameType(type))
        {
            return false;
        }
        Object value = data.getValue();
        if (value == null)
        {
            return false;
        }
        if (value instanceof Optional<?> optional)
        {
            if (optional.isEmpty() || !isCursedNameValue(optional.get(), limits))
            {
                return false;
            }
            ((EntityData) data).setValue(emptyOptional(type));
            return true;
        }
        if (!isCursedNameValue(value, limits))
        {
            return false;
        }
        ((EntityData) data).setValue(emptyValueFor(value));
        return true;
    }

    private static boolean isNameType(EntityDataType<?> type)
    {
        return type == EntityDataTypes.OPTIONAL_ADV_COMPONENT
                || type == EntityDataTypes.OPTIONAL_COMPONENT
                || type == EntityDataTypes.ADV_COMPONENT
                || type == EntityDataTypes.COMPONENT
                || type == EntityDataTypes.STRING;
    }

    private static boolean isCursedNameValue(Object value, Limits limits)
    {
        if (value instanceof Component component)
        {
            return isCursedComponent(component, limits);
        }
        if (value instanceof String string)
        {
            return isCursedRawName(string, limits);
        }
        return false;
    }

    private static boolean isCursedComponent(Component component, Limits limits)
    {
        if (ComponentScanner.isCursed(component, limits.maxComponentNodes()))
        {
            return true;
        }
        return ComponentScanner.safePlainTextLength(component, limits.maxComponentNodes()) > limits.maxNameLength();
    }

    private static boolean isCursedRawName(String value, Limits limits)
    {
        if (value == null || value.isEmpty())
        {
            return false;
        }
        if (value.length() > limits.maxNameLength() || containsSuspiciousJson(value))
        {
            return true;
        }
        try
        {
            return isCursedComponent(GSON.deserialize(value), limits);
        }
        catch (Throwable ignored)
        {
            return value.length() > limits.maxNameLength();
        }
    }

    private static boolean containsSuspiciousJson(String value)
    {
        return value.contains("click_event") || value.contains("hover_event") || value.contains("%1$");
    }

    private static Object emptyOptional(EntityDataType<?> type)
    {
        return Optional.empty();
    }

    private static Object emptyValueFor(Object value)
    {
        if (value instanceof Component)
        {
            return Component.empty();
        }
        if (value instanceof String)
        {
            return "";
        }
        return null;
    }

    private static boolean isScaleAttribute(Attribute attribute)
    {
        return attribute == Attributes.SCALE || attribute == Attributes.GENERIC_SCALE;
    }

    private static double clampScaleMagnitude(double value, double max)
    {
        if (Math.abs(value) <= max)
        {
            return value;
        }
        return Math.copySign(max, value == 0.0D ? 1.0D : value);
    }
}
