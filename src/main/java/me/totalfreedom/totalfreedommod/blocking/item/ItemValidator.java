package me.totalfreedom.totalfreedommod.blocking.item;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.blocking.sweep.EntityVisitor;
import me.totalfreedom.totalfreedommod.blocking.sweep.SweepContext;
import me.totalfreedom.totalfreedommod.blocking.sweep.TileEntityVisitor;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.DetectionReporter;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class ItemValidator extends FreedomService
{

    private static final long LOG_INTERVAL_TICKS = 100L;
    private static final int MAX_COMMAND_LENGTH = 16384;
    private static final int MAX_COMMAND_BRACE_DEPTH = 8;
    private static final int DEFAULT_MAX_POTION_EFFECTS = 24;

    private volatile boolean panicMode;
    private volatile int maxPotionEffects = DEFAULT_MAX_POTION_EFFECTS;

    private volatile ContainerSweepPolicy containerSweepPolicy = ContainerSweepPolicy.fromConfig("clear");

    private int sweepTaskId = -1;
    private int containerRadiusSweepTaskId = -1;

    private final TileEntityVisitor containerVisitor;
    private final EntityVisitor containerEntityVisitor;
    private final DetectionReporter reporter;

    public ItemValidator(TotalFreedomMod plugin)
    {
        super(plugin);

        reporter = new DetectionReporter(LOG_INTERVAL_TICKS, server::getCurrentTick,
                (count, reason, max, sample) -> "[ItemValidator] Blocked " + count
                        + " cursed item(s). Reason: " + reason
                        + " | max observed size: " + max
                        + " | sample: " + sample,
                DetectionReporter.warnAndBroadcastAdmins(plugin));

        containerVisitor = new TileEntityVisitor()
        {
            @Override
            public boolean enabled()
            {
                return ItemValidator.this.enabled() && chunkLoadScanEnabled();
            }

            @Override
            public long sweepIntervalTicks()
            {
                return 0L;
            }

            @Override
            public java.util.function.Predicate<Block> blockFilter()
            {
                return block -> block.getState() instanceof InventoryHolder;
            }

            @Override
            public void visit(BlockState state, SweepContext context)
            {
                if (state instanceof InventoryHolder holder)
                {
                    sanitizeInventoryHolder(holder, context.label());
                }
            }
        };

        containerEntityVisitor = new EntityVisitor()
        {
            @Override
            public boolean enabled()
            {
                return ItemValidator.this.enabled() && chunkLoadScanEnabled();
            }

            @Override
            public long sweepIntervalTicks()
            {
                return 0L;
            }

            @Override
            public void visit(Entity entity, SweepContext context)
            {
                if (entity instanceof InventoryHolder holder)
                {
                    sanitizeInventoryHolder(holder, context.label());
                }
            }
        };

        if (plugin.sweepScheduler != null)
        {
            plugin.sweepScheduler.register(containerVisitor);
            plugin.sweepScheduler.register(containerEntityVisitor);
        }
    }

    @Override
    protected void onStart()
    {
        panicMode = Boolean.TRUE.equals(ConfigEntry.CRASH_ITEMS_PANIC_MODE.getBoolean());
        Integer cap = ConfigEntry.CRASH_ITEMS_MAX_POTION_EFFECTS.getInteger();
        maxPotionEffects = cap != null ? cap : DEFAULT_MAX_POTION_EFFECTS;
        if (!RawNbtInspector.isAvailable())
        {
            FLog.warning("[ItemValidator] Raw NBT inspection is unavailable on this server runtime.");
        }
        containerSweepPolicy = ContainerSweepPolicy.fromConfig(ConfigEntry.CRASH_ITEMS_CONTAINER_SWEEP.getString());
        scheduleEquipmentSweep();
        scheduleContainerRadiusSweep();
        FLog.info("[ItemValidator] container sweep policy: " + containerSweepPolicy);
    }

    @Override
    protected void onStop()
    {
        if (sweepTaskId != -1)
        {
            server.getScheduler().cancelTask(sweepTaskId);
            sweepTaskId = -1;
        }
        if (containerRadiusSweepTaskId != -1)
        {
            server.getScheduler().cancelTask(containerRadiusSweepTaskId);
            containerRadiusSweepTaskId = -1;
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

    private void scheduleContainerRadiusSweep()
    {
        Integer configured = ConfigEntry.CRASH_ITEMS_CONTAINER_SWEEP_TICKS.getInteger();
        long interval = configured == null ? 40L : configured;
        if (interval <= 0L)
        {
            return;
        }
        containerRadiusSweepTaskId = server.getScheduler()
                .runTaskTimer(plugin, this::sweepContainersAroundPlayers, interval, interval)
                .getTaskId();
    }

    private void sweepContainersAroundPlayers()
    {
        if (!enabled())
        {
            return;
        }
        Integer configured = ConfigEntry.CRASH_ITEMS_CONTAINER_SWEEP_RADIUS.getInteger();
        int radius = Math.max(0, configured == null ? 4 : configured);
        Set<String> visited = new HashSet<>();
        for (Player player : server.getOnlinePlayers())
        {
            Chunk center = player.getLocation().getChunk();
            for (int x = center.getX() - radius; x <= center.getX() + radius; x++)
            {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++)
                {
                    if (!center.getWorld().isChunkLoaded(x, z))
                    {
                        continue;
                    }
                    String key = center.getWorld().getUID() + ":" + x + ":" + z;
                    if (visited.add(key))
                    {
                        sweepContainerChunk(center.getWorld().getChunkAt(x, z), "container-radius-sweep");
                    }
                }
            }
        }
    }

    private int sweepContainerChunk(Chunk chunk, String context)
    {
        if (chunk == null || !chunk.isLoaded())
        {
            return 0;
        }
        int changed = 0;
        Collection<BlockState> states;
        try
        {
            states = chunk.getTileEntities(block -> block.getState() instanceof InventoryHolder, false);
        }
        catch (Throwable ignored)
        {
            states = Arrays.asList(chunk.getTileEntities(false));
        }
        for (BlockState state : states)
        {
            if (state instanceof InventoryHolder holder && sanitizeInventoryHolder(holder, context))
            {
                changed++;
            }
        }
        for (Entity entity : chunk.getEntities())
        {
            if (entity instanceof InventoryHolder holder && sanitizeInventoryHolder(holder, context))
            {
                changed++;
            }
        }
        return changed;
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
        sweepGroundItems();
    }

    private void sweepGroundItems()
    {
        for (World world : server.getWorlds())
        {
            for (Item item : world.getEntitiesByClass(Item.class))
            {
                ItemScanner.Verdict v = scan(item.getItemStack());
                if (v.isCursed())
                {
                    item.remove();
                    recordDetection(v, "ground sweep @ " + FUtil.formatLocation(item.getLocation()));
                }
            }
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

    public int getMaxPotionEffects()
    {
        return maxPotionEffects;
    }

    private boolean enabled()
    {
        return Boolean.TRUE.equals(ConfigEntry.CRASH_ITEMS_PREVENT.getBoolean());
    }

    private ItemScanner.Verdict scan(ItemStack item)
    {
        return ItemScanner.scan(item, panicMode, maxPotionEffects);
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

    private void recordDetection(ItemScanner.Verdict verdict, String context)
    {
        reporter.record(verdict.reason().name(), verdict.observedSize(), context);
    }

    private boolean chunkLoadScanEnabled()
    {
        return Boolean.TRUE.equals(ConfigEntry.CRASH_ITEMS_SCAN_CHUNK_LOAD.getBoolean());
    }

    private boolean sanitizeInventoryHolder(InventoryHolder holder, String context)
    {
        if (holder == null)
        {
            return false;
        }
        Inventory inventory;
        try
        {
            inventory = holder.getInventory();
        }
        catch (Throwable ignored)
        {
            return false;
        }
        if (inventory == null)
        {
            return false;
        }

        ItemStack[] contents = inventory.getContents();
        ItemScanner.Verdict strongest = ItemScanner.Verdict.CLEAN;
        boolean dirty = false;
        for (int i = 0; i < contents.length; i++)
        {
            ItemScanner.Verdict verdict = scan(contents[i]);
            if (!verdict.isCursed())
            {
                continue;
            }
            if (!strongest.isCursed() || (verdict.reason().isHangClass() && !strongest.reason().isHangClass()))
            {
                strongest = verdict;
            }
            contents[i] = null;
            dirty = true;
        }
        if (!dirty)
        {
            return false;
        }

        ContainerSweepPolicy.Action action = containerSweepPolicy.actionFor(strongest.reason());
        switch (action)
        {
            case FILTER_SLOT -> inventory.setContents(contents);
            case CLEAR_CONTAINER -> inventory.clear();
            case DESTROY_BLOCK ->
            {
                if (holder instanceof BlockState state)
                {
                    state.getBlock().setType(Material.AIR, false);
                }
                else if (holder instanceof Entity entity)
                {
                    entity.remove();
                }
                else
                {
                    inventory.clear();
                }
            }
        }
        recordDetection(strongest, context + " @ " + describeHolder(holder) + " [" + action + "]");
        return true;
    }

    private static String describeHolder(InventoryHolder holder)
    {
        if (holder instanceof BlockState state)
        {
            return FUtil.formatLocation(state.getLocation());
        }
        if (holder instanceof Entity entity)
        {
            return entity.getType() + "@" + FUtil.formatLocation(entity.getLocation());
        }
        return holder.getClass().getSimpleName();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        if (enabled())
        {
            server.getScheduler().runTask(plugin, () -> sweepPlayer(event.getPlayer()));
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

        InventoryHolder holder = top.getHolder();
        Bukkit.getScheduler().runTask(plugin, () ->
        {
            if (holder != null)
            {
                sanitizeInventoryHolder(holder, "inventory-open");
            }
            else
            {
                cleanInventory(top);
            }
        });
    }

    private boolean cleanInventory(Inventory inv)
    {
        ItemStack[] contents = inv.getContents();
        boolean dirty = false;
        for (int i = 0; i < contents.length; i++)
        {
            ItemStack item = contents[i];
            if (item != null && scan(item).isCursed())
            {
                contents[i] = null;
                dirty = true;
            }
        }
        if (dirty)
        {
            inv.setContents(contents);
        }
        return dirty;
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
    public void onPlayerDropItem(PlayerDropItemEvent event)
    {
        if (!enabled())
        {
            return;
        }
        ItemScanner.Verdict v = scan(event.getItemDrop().getItemStack());
        if (!v.isCursed())
        {
            return;
        }
        event.setCancelled(true);
        recordDetection(v, "drop by " + event.getPlayer().getName());
        FUtil.playerMsg(event.getPlayer(), "You cannot drop a cursed item.", NamedTextColor.RED);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event)
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
        recordDetection(v, "consume by " + event.getPlayer().getName());
        FUtil.playerMsg(event.getPlayer(), "You cannot consume a cursed item.", NamedTextColor.RED);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event)
    {
        if (!enabled())
        {
            return;
        }
        ItemScanner.Verdict v = scan(event.getEntity().getItemStack());
        if (!v.isCursed())
        {
            return;
        }
        event.setCancelled(true);
        recordDetection(v, "item spawn at " + FUtil.formatLocation(event.getLocation()));
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
