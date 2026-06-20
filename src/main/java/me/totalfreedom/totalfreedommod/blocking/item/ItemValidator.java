package me.totalfreedom.totalfreedommod.blocking.item;

import java.util.List;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ItemValidator extends FreedomService
{

    private static final long LOG_INTERVAL_TICKS = 100L;
    private static final int MAX_COMMAND_LENGTH = 16384;
    private static final int MAX_COMMAND_BRACE_DEPTH = 8;

    private volatile boolean panicMode;

    private int sweepTaskId = -1;

    private long lastSummaryTick = 0L;
    private long detectionsSinceLastSummary = 0L;
    private ItemScanner.Reason dominantReason = ItemScanner.Reason.CLEAN;
    private long maxObservedSize = 0L;
    private String sampleContext = null;

    public ItemValidator(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        panicMode = Boolean.TRUE.equals(ConfigEntry.CRASH_ITEMS_PANIC_MODE.getBoolean());
        if (!RawNbtInspector.isAvailable())
        {
            FLog.warning("[ItemValidator] Raw NBT inspection is unavailable on this server runtime.");
        }
        scheduleEquipmentSweep();
    }

    @Override
    protected void onStop()
    {
        if (sweepTaskId != -1)
        {
            server.getScheduler().cancelTask(sweepTaskId);
            sweepTaskId = -1;
        }
    }

    /**
     * Periodically strips unwanted items from every online player's inventory,
     * armor, off-hand, and cursor.
     */
    private void scheduleEquipmentSweep()
    {
        long interval = ConfigEntry.CRASH_ITEMS_EQUIPMENT_SWEEP_TICKS.getInteger();
        if (interval <= 0)
        {
            return;
        }
        sweepTaskId = server.getScheduler().runTaskTimer(plugin, this::sweepOnlinePlayers, interval, interval).getTaskId();
    }

    private void sweepOnlinePlayers()
    {
        if (!enabled())
        {
            return;
        }
        for (Player player : server.getOnlinePlayers())
        {
            sweepPlayer(player);
        }
    }

    private void sweepPlayer(Player player)
    {
        boolean dirty = false;

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++)
        {
            ItemStack item = contents[i];
            if (item == null)
            {
                continue;
            }
            ItemScanner.Verdict v = scan(item);
            if (v.isCursed())
            {
                contents[i] = null;
                dirty = true;
                recordDetection(v, "equipment sweep on " + player.getName());
            }
        }
        if (dirty)
        {
            player.getInventory().setContents(contents);
        }

        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir())
        {
            ItemScanner.Verdict v = scan(cursor);
            if (v.isCursed())
            {
                player.setItemOnCursor(null);
                dirty = true;
                recordDetection(v, "cursor sweep on " + player.getName());
            }
        }

        if (dirty)
        {
            player.updateInventory();
        }
    }

    public boolean isCursed(ItemStack item)
    {
        return scan(item).isCursed();
    }

    private boolean enabled()
    {
        return Boolean.TRUE.equals(ConfigEntry.CRASH_ITEMS_PREVENT.getBoolean());
    }

    private ItemScanner.Verdict scan(ItemStack item)
    {
        return ItemScanner.scan(item, panicMode);
    }

    private ItemScanner.Verdict scanInventory(Inventory inv)
    {
        if (inv == null)
        {
            return ItemScanner.Verdict.CLEAN;
        }
        for (ItemStack item : inv.getContents())
        {
            ItemScanner.Verdict v = scan(item);
            if (v.isCursed())
            {
                return v;
            }
        }
        return ItemScanner.Verdict.CLEAN;
    }

    private void recordDetection(ItemScanner.Verdict v, String context)
    {
        detectionsSinceLastSummary++;
        if (v.observedSize() > maxObservedSize)
        {
            maxObservedSize = v.observedSize();
        }
        if (dominantReason == ItemScanner.Reason.CLEAN)
        {
            dominantReason = v.reason();
            sampleContext = context;
        }

        long nowTick = server.getCurrentTick();
        if (lastSummaryTick == 0L || nowTick - lastSummaryTick >= LOG_INTERVAL_TICKS)
        {
            String summary = "[ItemValidator] Blocked " + detectionsSinceLastSummary
                    + " cursed item(s). Reason: " + dominantReason
                    + " | max observed size: " + maxObservedSize
                    + " | sample: " + sampleContext;
            FLog.warning(summary);
            broadcastToAdmins(summary);

            lastSummaryTick = nowTick;
            detectionsSinceLastSummary = 0L;
            dominantReason = ItemScanner.Reason.CLEAN;
            maxObservedSize = 0L;
            sampleContext = null;
        }
    }

    private void broadcastToAdmins(String message)
    {
        Component component = Component.text(message, NamedTextColor.RED);
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (plugin.al.isAdmin(p))
            {
                p.sendMessage(component);
            }
        }
    }

    // ===== Command-side gates =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        if (!enabled())
        {
            return;
        }
        Player player = event.getPlayer();
        if (isCursedCommand(event.getMessage()))
        {
            event.setCancelled(true);
            FUtil.playerMsg(player, "Your command was rejected: it would create a cursed item.", NamedTextColor.RED);
            recordDetection(
                    new ItemScanner.Verdict(ItemScanner.Reason.OVERSIZED_TOTAL, event.getMessage().length(), 0),
                    "command by " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event)
    {
        if (!enabled())
        {
            return;
        }
        if (isCursedCommand(event.getCommand()))
        {
            event.setCancelled(true);
            recordDetection(
                    new ItemScanner.Verdict(ItemScanner.Reason.OVERSIZED_TOTAL, event.getCommand().length(), 0),
                    "console: " + event.getSender().getName());
        }
    }

    private boolean isCursedCommand(String raw)
    {
        if (raw == null || raw.isEmpty())
        {
            return false;
        }

        String command = raw.trim();
        if (command.startsWith("/"))
        {
            command = command.substring(1);
        }
        int sp = command.indexOf(' ');
        String head = (sp < 0 ? command : command.substring(0, sp)).toLowerCase();

        @SuppressWarnings("unchecked")
        List<String> inspected = (List<String>) ConfigEntry.CRASH_ITEMS_BASE_COMMANDS.getList();
        if (inspected.isEmpty())
        {
            return false;
        }

        boolean match = false;
        for (String name : inspected)
        {
            if (name == null || name.isEmpty())
            {
                continue;
            }
            String n = name.toLowerCase();
            if (head.equals(n) || head.equals("minecraft:" + n))
            {
                match = true;
                break;
            }
        }
        if (!match)
        {
            return false;
        }

        if (raw.length() > MAX_COMMAND_LENGTH)
        {
            return true;
        }

        int depth = 0;
        int peak = 0;
        for (int i = 0; i < raw.length(); i++)
        {
            char c = raw.charAt(i);
            if (c == '{' || c == '[')
            {
                depth++;
                if (depth > peak)
                {
                    peak = depth;
                }
            }
            else if (c == '}' || c == ']')
            {
                depth--;
            }
        }
        return peak > MAX_COMMAND_BRACE_DEPTH;
    }

    // ===== Container choke points (LOWEST: before CoreProtect's HIGHEST) =====

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event)
    {
        if (!enabled())
        {
            return;
        }
        Inventory top = event.getInventory();
        ItemScanner.Verdict v = scanInventory(top);
        if (!v.isCursed())
        {
            return;
        }

        event.setCancelled(true);
        recordDetection(v, event.getPlayer().getName() + " opened " + describeLocation(top));

        if (event.getPlayer() instanceof Player p)
        {
            FUtil.playerMsg(p,
                    "That container holds a cursed item.",
                    NamedTextColor.RED);
        }

        Bukkit.getScheduler().runTask(plugin, () -> cleanInventory(top));
    }

    private void cleanInventory(Inventory inv)
    {
        ItemStack[] contents = inv.getContents();
        boolean dirty = false;
        for (int i = 0; i < contents.length; i++)
        {
            ItemStack item = contents[i];
            if (item == null)
            {
                continue;
            }
            if (scan(item).isCursed())
            {
                contents[i] = null;
                dirty = true;
            }
        }
        if (dirty)
        {
            inv.setContents(contents);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        if (!enabled())
        {
            return;
        }
        ItemScanner.Verdict cur = scan(event.getCurrentItem());
        ItemScanner.Verdict csr = scan(event.getCursor());
        if (!cur.isCursed() && !csr.isCursed())
        {
            return;
        }

        event.setCancelled(true);
        if (cur.isCursed())
        {
            event.setCurrentItem(null);
            recordDetection(cur, "click slot by " + event.getWhoClicked().getName());
        }
        if (csr.isCursed())
        {
            event.getView().setCursor(null);
            recordDetection(csr, "click cursor by " + event.getWhoClicked().getName());
        }
        if (event.getWhoClicked() instanceof Player p)
        {
            p.updateInventory();
            FUtil.playerMsg(p, "A cursed item was removed.", NamedTextColor.RED);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryCreative(InventoryCreativeEvent event)
    {
        if (!enabled())
        {
            return;
        }
        ItemScanner.Verdict v = scan(event.getCursor());
        if (!v.isCursed())
        {
            return;
        }
        event.setCancelled(true);
        event.getView().setCursor(null);
        recordDetection(v, "creative pickup by " + event.getWhoClicked().getName());
        if (event.getWhoClicked() instanceof Player p)
        {
            p.updateInventory();
            FUtil.playerMsg(p, "A cursed item was removed.", NamedTextColor.RED);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event)
    {
        if (!enabled())
        {
            return;
        }
        ItemScanner.Verdict v = scan(event.getOldCursor());
        if (!v.isCursed())
        {
            for (ItemStack inner : event.getNewItems().values())
            {
                ItemScanner.Verdict iv = scan(inner);
                if (iv.isCursed())
                {
                    v = iv;
                    break;
                }
            }
        }
        if (!v.isCursed())
        {
            return;
        }
        event.setCancelled(true);
        recordDetection(v, "drag by " + event.getWhoClicked().getName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event)
    {
        if (!enabled())
        {
            return;
        }
        ItemScanner.Verdict v = scan(event.getItem());
        if (!v.isCursed())
        {
            return;
        }
        event.setCancelled(true);
        recordDetection(v, "hopper into " + describeLocation(event.getDestination()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryPickupItem(InventoryPickupItemEvent event)
    {
        if (!enabled())
        {
            return;
        }
        ItemScanner.Verdict v = scan(event.getItem().getItemStack());
        if (!v.isCursed())
        {
            return;
        }
        event.setCancelled(true);
        event.getItem().remove();
        recordDetection(v, "hopper pickup at " + describeLocation(event.getInventory()));
    }

    // ===== Lower-priority gates =====

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event)
    {
        if (!enabled())
        {
            return;
        }
        ItemScanner.Verdict v = scan(event.getItem().getItemStack());
        if (!v.isCursed())
        {
            return;
        }
        event.setCancelled(true);
        event.getItem().remove();
        recordDetection(v, "pickup by " + event.getEntity().getName());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if (!enabled())
        {
            return;
        }
        ItemScanner.Verdict v = scan(event.getItemInHand());
        if (!v.isCursed())
        {
            return;
        }
        event.setCancelled(true);
        recordDetection(v, "place by " + event.getPlayer().getName());
        FUtil.playerMsg(event.getPlayer(), "You cannot place a cursed container.", NamedTextColor.RED);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event)
    {
        if (!enabled())
        {
            return;
        }
        ItemScanner.Verdict v = scan(event.getItem());
        if (!v.isCursed())
        {
            return;
        }
        event.setCancelled(true);
        recordDetection(v, "dispense at " + FUtil.formatLocation(event.getBlock().getLocation()));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPrepareAnvil(PrepareAnvilEvent event)
    {
        if (!enabled())
        {
            return;
        }
        ItemScanner.Verdict v = scan(event.getResult());
        if (!v.isCursed())
        {
            return;
        }
        event.setResult(null);
        recordDetection(v, "anvil result");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPrepareItemCraft(PrepareItemCraftEvent event)
    {
        if (!enabled())
        {
            return;
        }
        ItemStack result = event.getInventory().getResult();
        ItemScanner.Verdict v = scan(result);
        if (!v.isCursed())
        {
            return;
        }
        event.getInventory().setResult(null);
        recordDetection(v, "craft result");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event)
    {
        if (!enabled())
        {
            return;
        }
        List<ItemStack> loot = event.getLoot();
        boolean dirty = false;
        for (int i = loot.size() - 1; i >= 0; i--)
        {
            ItemScanner.Verdict v = scan(loot.get(i));
            if (v.isCursed())
            {
                loot.remove(i);
                recordDetection(v, "loot table");
                dirty = true;
            }
        }
        if (dirty)
        {
            event.setLoot(loot);
        }
    }

    private String describeLocation(Inventory inv)
    {
        if (inv == null || inv.getLocation() == null)
        {
            return "unknown";
        }
        return FUtil.formatLocation(inv.getLocation());
    }
}
