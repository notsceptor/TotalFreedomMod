package me.totalfreedom.totalfreedommod.blocking.item;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.blocking.sweep.EntityVisitor;
import me.totalfreedom.totalfreedommod.blocking.sweep.SweepContext;
import me.totalfreedom.totalfreedommod.blocking.sweep.TileEntityVisitor;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.DetectionReporter;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private static final long CLICK_SCAN_BUDGET_NANOS = 3_000_000L;
    private static final long CHUNK_ITEM_SCAN_BUDGET_NANOS = 2_000_000L;
    private static final long SWEEP_ITEM_SCAN_BUDGET_NANOS = 2_000_000L;
    private static final long CONTAINER_SCAN_BUDGET_NANOS = 10_000_000L;

    private volatile boolean panicMode;
    private volatile int maxPotionEffects = DEFAULT_MAX_POTION_EFFECTS;
    private volatile ContainerSweepPolicy containerSweepPolicy = ContainerSweepPolicy.CLEAR;

    private int sweepTaskId = -1;
    private int containerRadiusSweepTaskId = -1;

    private final TileEntityVisitor containerVisitor = new TileEntityVisitor()
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
        public Predicate<Block> blockFilter()
        {
            return b -> true;
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

    private final EntityVisitor containerEntityVisitor = new EntityVisitor()
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

    private final DetectionReporter reporter = new DetectionReporter(
            LOG_INTERVAL_TICKS, server::getCurrentTick,
            (count, reason, max, sample) -> "[ItemValidator] Blocked " + count
                    + " cursed item(s). Reason: " + reason
                    + " | max observed size: " + max
                    + " | sample: " + sample,
            DetectionReporter.warnAndBroadcastAdmins(plugin));

    public ItemValidator(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        panicMode = Boolean.TRUE.equals(ConfigEntry.CRASH_ITEMS_PANIC_MODE.getBoolean());
        Integer cap = ConfigEntry.CRASH_ITEMS_MAX_POTION_EFFECTS.getInteger();
        maxPotionEffects = cap != null ? cap : DEFAULT_MAX_POTION_EFFECTS;
        containerSweepPolicy = ContainerSweepPolicy.fromConfig(
                ConfigEntry.CRASH_CONTAINERS_SWEEP_MODE.getString());
        if (!RawNbtInspector.isAvailable())
        {
            FLog.warning("[ItemValidator] Raw NBT inspection is unavailable on this server runtime.");
        }
        scheduleEquipmentSweep();
        scheduleContainerRadiusSweep();
        plugin.sweepScheduler.register(containerVisitor);
        plugin.sweepScheduler.register(containerEntityVisitor);
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
        if (!enabled() || !chunkLoadScanEnabled())
        {
            return;
        }
        long ticks = ConfigEntry.CRASH_CONTAINERS_SWEEP_TICKS.getInteger();
        if (ticks <= 0)
        {
            return;
        }
        containerRadiusSweepTaskId = server.getScheduler()
                .runTaskTimer(plugin, this::sweepContainersAroundPlayers, ticks, ticks)
                .getTaskId();
    }

    private void sweepContainersAroundPlayers()
    {
        if (!enabled() || !chunkLoadScanEnabled())
        {
            return;
        }
        int radius = Math.max(0, ConfigEntry.CRASH_CONTAINERS_SWEEP_RADIUS.getInteger());
        Set<String> visited = new HashSet<>();
        int acted = 0;
        for (Player player : server.getOnlinePlayers())
        {
            World world = player.getWorld();
            Chunk center = player.getLocation().getChunk();
            int baseX = center.getX();
            int baseZ = center.getZ();
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    int cx = baseX + dx;
                    int cz = baseZ + dz;
                    if (!world.isChunkLoaded(cx, cz))
                    {
                        continue;
                    }
                    if (!visited.add(world.getUID() + ":" + cx + ":" + cz))
                    {
                        continue;
                    }
                    acted += sweepContainerChunk(world.getChunkAt(cx, cz), "radius sweep");
                }
            }
        }
        if (acted > 0)
        {
            FLog.warning("[ItemValidator] Radius sweep handled " + acted + " cursed container(s).", true);
        }
    }

    private int sweepContainerChunk(Chunk chunk, String context)
    {
        Collection<BlockState> tiles;
        try
        {
            tiles = chunk.getTileEntities(b -> true, false);
        }
        catch (Throwable ignored)
        {
            return 0;
        }
        int acted = 0;
        for (BlockState state : tiles)
        {
            if (state instanceof InventoryHolder holder)
            {
                if (sanitizeInventoryHolder(holder, context))
                {
                    acted++;
                }
            }
        }
        for (Entity entity : chunk.getEntities())
        {
            if (entity instanceof InventoryHolder holder)
            {
                if (sanitizeInventoryHolder(holder, context))
                {
                    acted++;
                }
            }
        }
        return acted;
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
                ItemScanner.Verdict v = scanWithBudget(item.getItemStack(), SWEEP_ITEM_SCAN_BUDGET_NANOS);
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
            ItemScanner.Verdict v = scanWithBudget(item, SWEEP_ITEM_SCAN_BUDGET_NANOS);
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
            ItemScanner.Verdict v = scanWithBudget(cursor, SWEEP_ITEM_SCAN_BUDGET_NANOS);
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

    private boolean chunkLoadScanEnabled()
    {
        return Boolean.TRUE.equals(ConfigEntry.CRASH_CONTAINERS_SCAN_CHUNK_LOAD.getBoolean());
    }

    private ItemScanner.Verdict scan(ItemStack item)
    {
        return ItemScanner.scan(item, panicMode, maxPotionEffects);
    }

    private ItemScanner.Verdict scanWithBudget(ItemStack item, long budgetNanos)
    {
        if (item == null || item.isEmpty() || budgetNanos <= 0L)
        {
            return scan(item);
        }
        return ItemScanner.scan(item, panicMode, maxPotionEffects, System.nanoTime() + budgetNanos);
    }

    private ItemScanner.Verdict scanUntil(ItemStack item, long deadlineNanos)
    {
        if (item == null || item.isEmpty())
        {
            return scan(item);
        }
        return ItemScanner.scan(item, panicMode, maxPotionEffects, deadlineNanos);
    }

    private ItemScanner.Verdict scanInventory(Inventory inv)
    {
        return scanInventory(inv, CHUNK_ITEM_SCAN_BUDGET_NANOS);
    }

    private ItemScanner.Verdict scanInventory(Inventory inv, long perItemBudgetNanos)
    {
        if (inv == null)
        {
            return ItemScanner.Verdict.CLEAN;
        }
        for (ItemStack item : inv.getContents())
        {
            ItemScanner.Verdict v = scanWithBudget(item, perItemBudgetNanos);
            if (v.isCursed())
            {
                return v;
            }
        }
        return ItemScanner.Verdict.CLEAN;
    }

    /**
     * Budgeted first-hit scan of an open world container during click/drag/open.
     * Escalates hang-class hits (or unreadable inventories) per {@link ContainerSweepPolicy}.
     * Escalation resolves the backing block or mobile entity via inventory location /
     * {@link #resolveEntityHolder(Inventory)} instead of {@link Inventory#getHolder()},
     * which re-deserializes block-entity NBT and can hang the server on exploit chests.
     *
     * @return {@code true} when the interaction should be cancelled (container handled)
     */
    private boolean probeOpenContainer(Inventory top, String context)
    {
        ItemScanner.Verdict v = scanContainerFirstHit(top);
        if (!v.isCursed())
        {
            return false;
        }
        ContainerSweepPolicy.Action action = containerSweepPolicy.actionFor(v.reason());
        if (action == ContainerSweepPolicy.Action.FILTER_SLOT)
        {
            return false;
        }
        return escalateResolvedContainer(top, v, action, context);
    }

    private ItemScanner.Verdict scanContainerFirstHit(Inventory top)
    {
        if (top == null)
        {
            return ItemScanner.Verdict.CLEAN;
        }
        ItemStack[] contents;
        try
        {
            contents = top.getContents();
        }
        catch (Throwable t)
        {
            return uninspectableVerdict();
        }
        long containerDeadline = System.nanoTime() + CONTAINER_SCAN_BUDGET_NANOS;
        for (ItemStack item : contents)
        {
            if (item == null)
            {
                continue;
            }
            long itemDeadline = Math.min(System.nanoTime() + CHUNK_ITEM_SCAN_BUDGET_NANOS, containerDeadline);
            ItemScanner.Verdict v = scanUntil(item, itemDeadline);
            if (v.isCursed())
            {
                return v;
            }
        }
        return ItemScanner.Verdict.CLEAN;
    }

    private boolean escalateResolvedContainer(Inventory top, ItemScanner.Verdict verdict,
            ContainerSweepPolicy.Action action, String context)
    {
        InventoryHolder holder = resolveEntityHolder(top);
        if (holder != null)
        {
            return escalateHolder(holder, top, action, verdict, context);
        }
        Block block = blockFromInventoryLocation(top);
        if (block != null)
        {
            return escalateBlockContainer(block, top, action, verdict, context);
        }
        if (action == ContainerSweepPolicy.Action.CLEAR_CONTAINER)
        {
            top.clear();
            recordDetection(verdict, context + " [container cleared]");
            return true;
        }
        return false;
    }

    private boolean sanitizeInventoryHolder(InventoryHolder holder, String context)
    {
        if (!isWorldContainerHolder(holder))
        {
            return false;
        }
        Inventory inv;
        try
        {
            inv = holder.getInventory();
        }
        catch (Throwable t)
        {
            ItemScanner.Verdict v = uninspectableVerdict();
            return escalateHolder(holder, null, containerSweepPolicy.actionFor(v.reason()), v,
                    context + " @ " + describeHolder(holder));
        }
        return sanitizeContainerInventory(inv, holder, context);
    }

    private boolean sanitizeContainerInventory(Inventory inv, InventoryHolder holder, String context)
    {
        if (!isWorldContainerHolder(holder) || inv == null)
        {
            return false;
        }
        String sample = context + " @ " + describeHolder(holder);

        ItemStack[] contents;
        try
        {
            contents = inv.getContents();
        }
        catch (Throwable t)
        {
            ItemScanner.Verdict v = uninspectableVerdict();
            return escalateHolder(holder, null, containerSweepPolicy.actionFor(v.reason()), v, sample);
        }

        long containerDeadline = System.nanoTime() + CONTAINER_SCAN_BUDGET_NANOS;
        boolean purged = false;
        for (int i = 0; i < contents.length; i++)
        {
            ItemStack item = contents[i];
            if (item == null)
            {
                continue;
            }
            long itemDeadline = Math.min(System.nanoTime() + CHUNK_ITEM_SCAN_BUDGET_NANOS, containerDeadline);
            ItemScanner.Verdict v = scanUntil(item, itemDeadline);
            if (!v.isCursed())
            {
                continue;
            }
            ContainerSweepPolicy.Action action = containerSweepPolicy.actionFor(v.reason());
            if (action != ContainerSweepPolicy.Action.FILTER_SLOT)
            {
                return escalateHolder(holder, inv, action, v, sample);
            }
            contents[i] = null;
            purged = true;
            recordDetection(v, sample);
        }
        if (purged)
        {
            inv.setContents(contents);
        }
        return purged;
    }

    /**
     * Applies a container-wide sweep action for a hang-class verdict. Empties the
     * container when asked to clear, removes the backing holder when asked to
     * destroy, and falls back to removal when the inventory cannot be read.
     */
    private boolean escalateHolder(InventoryHolder holder, Inventory inv, ContainerSweepPolicy.Action action,
            ItemScanner.Verdict verdict, String sample)
    {
        if (action == ContainerSweepPolicy.Action.FILTER_SLOT)
        {
            return false;
        }
        if (action == ContainerSweepPolicy.Action.CLEAR_CONTAINER && inv != null)
        {
            inv.clear();
            recordDetection(verdict, sample + " [container cleared]");
            return true;
        }
        destroyHolder(holder, sample, verdict);
        return true;
    }

    /**
     * Block-only escalation for click paths that avoid {@link Inventory#getHolder()}.
     */
    private boolean escalateBlockContainer(Block block, Inventory inv, ContainerSweepPolicy.Action action,
            ItemScanner.Verdict verdict, String sample)
    {
        if (action == ContainerSweepPolicy.Action.FILTER_SLOT)
        {
            return false;
        }
        if (action == ContainerSweepPolicy.Action.CLEAR_CONTAINER && inv != null)
        {
            inv.clear();
            recordDetection(verdict, sample + " [container cleared]");
            return true;
        }
        destroyContainerBlock(block, sample, verdict);
        return true;
    }

    private static ItemScanner.Verdict uninspectableVerdict()
    {
        return new ItemScanner.Verdict(ItemScanner.Reason.UNINSPECTABLE_NBT, -1L, 0);
    }

    private void destroyContainerBlock(Block block, String context, ItemScanner.Verdict verdict)
    {
        try
        {
            block.setType(Material.AIR, false);
        }
        catch (Throwable ignored)
        {
        }
        recordDetection(verdict, context + " [block destroyed]");
        FLog.warning("[ItemValidator] Destroyed container block at " + FUtil.formatLocation(block.getLocation())
                + " (" + verdict.reason() + ").", true);
    }

    private void destroyHolder(InventoryHolder holder, String context, ItemScanner.Verdict verdict)
    {
        if (holder instanceof BlockState state)
        {
            destroyContainerBlock(state.getBlock(), context, verdict);
            return;
        }
        if (holder instanceof Entity entity)
        {
            try
            {
                entity.remove();
            }
            catch (Throwable ignored)
            {
            }
            recordDetection(verdict, context + " [entity removed]");
            FLog.warning("[ItemValidator] Removed container entity at "
                    + FUtil.formatLocation(entity.getLocation())
                    + " (" + verdict.reason() + ").", true);
        }
    }

    private static boolean isWorldContainerHolder(InventoryHolder holder)
    {
        return holder != null && !(holder instanceof Player);
    }

    private static String describeHolder(InventoryHolder holder)
    {
        if (holder instanceof BlockState state)
        {
            return FUtil.formatLocation(state.getLocation());
        }
        if (holder instanceof Entity entity)
        {
            return FUtil.formatLocation(entity.getLocation());
        }
        Inventory inv = holder.getInventory();
        if (inv != null && inv.getLocation() != null)
        {
            return FUtil.formatLocation(inv.getLocation());
        }
        return "unknown";
    }

    /**
     * Locates a mobile {@link InventoryHolder} (minecart, chested horse, ...) for
     * an open inventory without calling {@link Inventory#getHolder()}, which would
     * re-load block-entity NBT for placed chests.
     */
    private static InventoryHolder resolveEntityHolder(Inventory inv)
    {
        if (inv == null)
        {
            return null;
        }
        org.bukkit.Location loc = inv.getLocation();
        if (loc == null || loc.getWorld() == null)
        {
            return null;
        }
        try
        {
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0))
            {
                if (!(entity instanceof InventoryHolder holder) || holder instanceof Player)
                {
                    continue;
                }
                if (holder.getInventory() == inv)
                {
                    return holder;
                }
            }
        }
        catch (Throwable ignored)
        {
        }
        return null;
    }

    private void recordDetection(ItemScanner.Verdict v, String context)
    {
        reporter.record(v.reason().name(), v.observedSize(), context);
    }

    // ===== Command-side gates =====

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        // Swept at join (not login): the player's inventory is reliably loaded
        // here, and setContents/updateInventory affect a fully-online player.
        if (!enabled())
        {
            return;
        }
        sweepPlayer(event.getPlayer());
    }

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

    // ===== Container choke points (LOWEST: act before CoreProtect / WorldGuard, which load AFTER TFM) =====

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event)
    {
        if (!enabled())
        {
            return;
        }
        Inventory top = event.getInventory();
        InventoryHolder holder = top.getHolder();
        if (isWorldContainerHolder(holder))
        {
            String context = event.getPlayer().getName() + " opened " + describeLocation(top);
            if (probeOpenContainer(top, context))
            {
                event.setCancelled(true);
            }
            else
            {
                ItemScanner.Verdict v = scanContainerFirstHit(top);
                if (!v.isCursed())
                {
                    return;
                }
                event.setCancelled(true);
                recordDetection(v, context);
                sanitizeInventoryHolder(holder, context);
            }
            if (event.getPlayer() instanceof Player p)
            {
                FUtil.playerMsg(p,
                        "That container holds a cursed item.",
                        NamedTextColor.RED);
            }
            return;
        }

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
        cleanInventory(top);
    }

    private boolean cleanInventory(Inventory inv)
    {
        return cleanInventory(inv, 0L, null);
    }

    private boolean cleanInventory(Inventory inv, long perItemBudgetNanos, String context)
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
            ItemScanner.Verdict v = scanWithBudget(item, perItemBudgetNanos);
            if (v.isCursed())
            {
                contents[i] = null;
                dirty = true;
                if (context != null)
                {
                    recordDetection(v, context);
                }
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
        String actor = event.getWhoClicked().getName();
        Inventory top = event.getView().getTopInventory();
        if (probeOpenContainer(top, "click by " + actor))
        {
            event.setCancelled(true);
            closeContainerView(event.getWhoClicked());
            return;
        }

        long deadline = System.nanoTime() + CLICK_SCAN_BUDGET_NANOS;
        ItemScanner.Verdict cur = ItemScanner.scan(event.getCurrentItem(), panicMode, maxPotionEffects, deadline);
        ItemScanner.Verdict csr = ItemScanner.scan(event.getCursor(), panicMode, maxPotionEffects, deadline);
        if (!cur.isCursed() && !csr.isCursed())
        {
            return;
        }

        event.setCancelled(true);
        ItemScanner.Verdict hang = hangClassVerdict(cur, csr);
        if (hang != null)
        {
            ContainerSweepPolicy.Action action = containerSweepPolicy.actionFor(hang.reason());
            if (action != ContainerSweepPolicy.Action.FILTER_SLOT)
            {
                escalateResolvedContainer(top, hang, action, "click by " + actor);
                closeContainerView(event.getWhoClicked());
                return;
            }
        }

        if (cur.isCursed())
        {
            event.setCurrentItem(null);
            recordDetection(cur, "click slot by " + actor);
        }
        if (csr.isCursed())
        {
            event.getView().setCursor(null);
            recordDetection(csr, "click cursor by " + actor);
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
        String actor = event.getWhoClicked().getName();
        Inventory top = event.getView().getTopInventory();
        if (probeOpenContainer(top, "drag by " + actor))
        {
            event.setCancelled(true);
            closeContainerView(event.getWhoClicked());
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
        if (v.reason().isHangClass())
        {
            ContainerSweepPolicy.Action action = containerSweepPolicy.actionFor(v.reason());
            if (action != ContainerSweepPolicy.Action.FILTER_SLOT)
            {
                escalateResolvedContainer(top, v, action, "drag by " + actor);
                closeContainerView(event.getWhoClicked());
                return;
            }
        }
        recordDetection(v, "drag by " + actor);
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

    private static Block blockFromInventoryLocation(Inventory inv)
    {
        if (inv == null)
        {
            return null;
        }
        org.bukkit.Location loc = inv.getLocation();
        if (loc == null || loc.getWorld() == null)
        {
            return null;
        }
        return loc.getBlock();
    }

    private static ItemScanner.Verdict hangClassVerdict(ItemScanner.Verdict a, ItemScanner.Verdict b)
    {
        if (a.isCursed() && a.reason().isHangClass())
        {
            return a;
        }
        if (b.isCursed() && b.reason().isHangClass())
        {
            return b;
        }
        return null;
    }

    private static void closeContainerView(org.bukkit.entity.HumanEntity viewer)
    {
        if (!(viewer instanceof Player p))
        {
            return;
        }
        p.closeInventory();
        FUtil.playerMsg(p, "That container was removed because it contained invalid data.", NamedTextColor.GRAY);
    }
}
