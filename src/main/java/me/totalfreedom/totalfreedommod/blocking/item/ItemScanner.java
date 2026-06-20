package me.totalfreedom.totalfreedommod.blocking.item;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BundleContents;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import io.papermc.paper.datacomponent.item.ItemLore;
import java.util.List;
import me.totalfreedom.totalfreedommod.util.ComponentScanner;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

final class ItemScanner
{

    private ItemScanner()
    {
    }

    enum Reason
    {
        CLEAN,
        CONTAINER_TOO_DEEP,
        OVERSIZED_NAME,
        OVERSIZED_LORE,
        OVERSIZED_TOTAL,
        OVERSIZED_NBT,
        UNINSPECTABLE_NBT,
        CURSED_COMPONENT,
        PANIC_BLANKET_REJECT
    }

    record Verdict(Reason reason, long observedSize, int depth)
    {
        static final Verdict CLEAN = new Verdict(Reason.CLEAN, 0L, 0);

        boolean isCursed()
        {
            return reason != Reason.CLEAN;
        }
    }

    record Config(
            boolean panicMode,
            int maxNbtNodes,
            int maxNbtDepth,
            int maxContainerDepth,
            int maxNameLength,
            int maxLoreLines,
            int maxLoreLineLength,
            int maxComponentNodes)
    {
    }

    static Verdict scan(ItemStack item, Config cfg)
    {
        return scan(item, cfg, 0);
    }

    private static Verdict scan(ItemStack item, Config cfg, int depth)
    {
        if (item == null || item.isEmpty())
        {
            return Verdict.CLEAN;
        }

        if (depth > cfg.maxContainerDepth())
        {
            return new Verdict(Reason.CONTAINER_TOO_DEEP, 0L, depth);
        }

        if (cfg.panicMode())
        {
            if (item.hasData(DataComponentTypes.CONTAINER)
                    || item.hasData(DataComponentTypes.BUNDLE_CONTENTS)
                    || item.hasData(DataComponentTypes.CONTAINER_LOOT)
                    || item.hasData(DataComponentTypes.CHARGED_PROJECTILES))
            {
                return new Verdict(Reason.PANIC_BLANKET_REJECT, 0L, depth);
            }
        }

        if (item.hasData(DataComponentTypes.CUSTOM_NAME))
        {
            Component name = item.getData(DataComponentTypes.CUSTOM_NAME);
            if (name != null)
            {
                int len = ComponentScanner.safePlainTextLength(name, cfg.maxComponentNodes());
                if (len < 0)
                {
                    return new Verdict(Reason.CURSED_COMPONENT, -1L, depth);
                }
                if (len > cfg.maxNameLength())
                {
                    return new Verdict(Reason.OVERSIZED_NAME, len, depth);
                }
            }
        }

        if (item.hasData(DataComponentTypes.LORE))
        {
            ItemLore lore = item.getData(DataComponentTypes.LORE);
            if (lore != null)
            {
                List<Component> lines = lore.lines();
                if (lines.size() > cfg.maxLoreLines())
                {
                    return new Verdict(Reason.OVERSIZED_LORE, lines.size(), depth);
                }
                for (Component line : lines)
                {
                    int len = ComponentScanner.safePlainTextLength(line, cfg.maxComponentNodes());
                    if (len < 0)
                    {
                        return new Verdict(Reason.CURSED_COMPONENT, -1L, depth);
                    }
                    if (len > cfg.maxLoreLineLength())
                    {
                        return new Verdict(Reason.OVERSIZED_LORE, len, depth);
                    }
                }
            }
        }

        if (item.hasData(DataComponentTypes.CONTAINER))
        {
            ItemContainerContents container = item.getData(DataComponentTypes.CONTAINER);
            if (container != null)
            {
                for (ItemStack inner : container.contents())
                {
                    Verdict v = scan(inner, cfg, depth + 1);
                    if (v.isCursed())
                    {
                        return v;
                    }
                }
            }
        }

        if (item.hasData(DataComponentTypes.BUNDLE_CONTENTS))
        {
            BundleContents bundle = item.getData(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundle != null)
            {
                for (ItemStack inner : bundle.contents())
                {
                    Verdict v = scan(inner, cfg, depth + 1);
                    if (v.isCursed())
                    {
                        return v;
                    }
                }
            }
        }

        if (item.hasItemMeta())
        {
            switch (RawNbtInspector.inspect(item, cfg.maxNbtNodes(), cfg.maxNbtDepth()))
            {
                case OVERSIZED -> {
                    return new Verdict(Reason.OVERSIZED_NBT, cfg.maxNbtNodes(), depth);
                }
                case ERROR -> {
                    return new Verdict(Reason.UNINSPECTABLE_NBT, -1L, depth);
                }
                default -> {
                }
            }
        }

        return Verdict.CLEAN;
    }
}
