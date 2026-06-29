package me.totalfreedom.totalfreedommod.bridge;

import com.google.common.eventbus.Subscribe;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Loaded only after WorldEditBridge has confirmed the WorldEdit plugin is
 * present — never reference this class from a code path that could run
 * without WorldEdit on the classpath.
 */
public final class WorldEditHook implements Listener
{

    private static final Pattern LIMIT_COMMAND = Pattern.compile(
        "^/(?:limit|/limit)\\s+(\\d+|-1)(?:\\s+(.+))?$", Pattern.CASE_INSENSITIVE);

    private static final Pattern BLOCK_TOKEN = Pattern.compile("[a-z0-9_]+(?::[a-z0-9_]+)?");

    private static final Map<String, Integer> RADIUS_COMMANDS = new HashMap<>();

    static
    {
        RADIUS_COMMANDS.put("replacenear", 0);
        RADIUS_COMMANDS.put("drain", 0);
        RADIUS_COMMANDS.put("fixwater", 0);
        RADIUS_COMMANDS.put("fixlava", 0);
        RADIUS_COMMANDS.put("snow", 0);
        RADIUS_COMMANDS.put("thaw", 0);
        RADIUS_COMMANDS.put("ex", 0);
        RADIUS_COMMANDS.put("butcher", 0);
        RADIUS_COMMANDS.put("removeabove", 0);
        RADIUS_COMMANDS.put("removebelow", 0);
        RADIUS_COMMANDS.put("forestgen", 0);
        RADIUS_COMMANDS.put("forest", 0);
        RADIUS_COMMANDS.put("pumpkins", 0);
        // Radius is the second arg (after a pattern/type).
        RADIUS_COMMANDS.put("sphere", 1);
        RADIUS_COMMANDS.put("hsphere", 1);
        RADIUS_COMMANDS.put("cyl", 1);
        RADIUS_COMMANDS.put("hcyl", 1);
        RADIUS_COMMANDS.put("pyramid", 1);
        RADIUS_COMMANDS.put("removenear", 1);
    }

    private static final String[] BYPASS_NODES = {
        "fawe.bypass",
        "fawe.bypass.regions",
        "fawe.limit.unlimited",
        "fawe.admin",
        "worldedit.limit.unrestricted"
    };

    private static final Set<String> SIZE_SENSITIVE_LABELS = Set.of(
        "copy", "cut", "paste", "stack", "set", "replace", "regen",
        "sphere", "cyl", "pyramid", "smooth", "hsphere", "hcyl", "hpyramid",
        // relighting doesn't call setBlock so we have to guard their volume
        "fixlighting", "fixlight", "removelighting", "removelight"
    );

    private final TotalFreedomMod plugin;
    private final Map<UUID, RegionSnapshot> lastSelections = new HashMap<>();
    private final Map<UUID, Integer> playerLimits = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionAttachment> bypassAttachments = new HashMap<>();
    private final Map<UUID, OpThrottle> opThrottles = new ConcurrentHashMap<>();

    private BukkitTask selectionPollTask;
    private Object editSessionSubscriber;
    private WorldEditPlugin worldEditPlugin;

    public WorldEditHook(TotalFreedomMod plugin)
    {
        this.plugin = plugin;
    }

    public void register()
    {
        worldEditPlugin = resolveWorldEditPlugin();
        if (worldEditPlugin == null)
        {
            // Should not happen — WorldEditBridge gates on provider presence —
            // but bail out cleanly rather than NPE on the first poll tick.
            FLog.warning("WorldEditHook.register(): no WorldEdit / FAWE plugin instance available.");
            return;
        }

        // /limit interception + quit cleanup.
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Wrap every editing session's extent so we can detect operations.
        editSessionSubscriber = new Object()
        {
            @Subscribe
            public void onEditSession(EditSessionEvent event)
            {
                if (event.getStage() != EditSession.Stage.BEFORE_CHANGE)
                {
                    return;
                }
                if (!(event.getActor() instanceof com.sk89q.worldedit.entity.Player))
                {
                    return;
                }
                final com.sk89q.worldedit.entity.Player wePlayer =
                    (com.sk89q.worldedit.entity.Player) event.getActor();

                Extent wrapped = new ProtectedAreaExtent(event.getExtent(), wePlayer, event.getWorld());

                final Player bukkitPlayer = Bukkit.getPlayer(wePlayer.getUniqueId());
                if (bukkitPlayer != null && !plugin.al.isAdmin(bukkitPlayer))
                {
                    wrapped = new LimitExtent(wrapped, wePlayer.getUniqueId(), getLimitFor(wePlayer.getUniqueId()));
                }

                event.setExtent(wrapped);
            }
        };
        WorldEdit.getInstance().getEventBus().register(editSessionSubscriber);

        selectionPollTask = Bukkit.getScheduler().runTaskTimer(plugin, this::pollSelections, 1L, 1L);

        // Catch up on already-online players (covers /reload and the 20-tick attach delay).
        for (Player online : Bukkit.getOnlinePlayers())
        {
            refreshBypassNegation(online);
        }

        FLog.info("WorldEdit hook registered.");
    }

    /**
     * Resolves whichever WorldEdit-compatible plugin is loaded. FAWE's main
     * plugin class extends {@link WorldEditPlugin}, so casting works for both
     * stock WorldEdit and FAWE-only installs.
     */
    private static WorldEditPlugin resolveWorldEditPlugin()
    {
        org.bukkit.plugin.Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (we instanceof WorldEditPlugin && we.isEnabled())
        {
            return (WorldEditPlugin) we;
        }
        we = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        if (we instanceof WorldEditPlugin && we.isEnabled())
        {
            return (WorldEditPlugin) we;
        }
        return null;
    }

    public void unregister()
    {
        if (selectionPollTask != null)
        {
            selectionPollTask.cancel();
            selectionPollTask = null;
        }
        HandlerList.unregisterAll(this);
        // WorldEdit's EventBus offers no public remove(); dropping our reference
        // lets the subscriber be GC'd once the bus releases it on shutdown.
        editSessionSubscriber = null;
        lastSelections.clear();
        opThrottles.clear();

        for (Map.Entry<UUID, PermissionAttachment> e : bypassAttachments.entrySet())
        {
            try
            {
                e.getValue().remove();
            }
            catch (Throwable ignored)
            {
            }
        }
        bypassAttachments.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        refreshBypassNegation(event.getPlayer());
    }

    public void refreshBypassNegation(Player player)
    {
        if (player == null || !player.isOnline())
        {
            return;
        }
        final UUID uuid = player.getUniqueId();
        final boolean shouldNegate = !plugin.al.isAdmin(player);
        final PermissionAttachment existing = bypassAttachments.get(uuid);

        if (!shouldNegate)
        {
            if (existing != null)
            {
                try
                {
                    existing.remove();
                }
                catch (Throwable ignored)
                {
                }
                bypassAttachments.remove(uuid);
            }
            return;
        }

        if (existing != null)
        {
            return;
        }
        try
        {
            final PermissionAttachment att = player.addAttachment(plugin);
            for (String node : BYPASS_NODES)
            {
                att.setPermission(node, false);
            }
            bypassAttachments.put(uuid, att);
        }
        catch (Throwable t)
        {
            FLog.warning("Failed to apply WorldEdit bypass negation for " + player.getName() + ": " + t.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event)
    {
        if (checkOpThrottle(event))
        {
            return;
        }

        if (checkPatternComplexity(event))
        {
            return;
        }

        if (checkPatternTypes(event))
        {
            return;
        }

        if (checkRadiusCommand(event))
        {
            return;
        }

        final Matcher m = LIMIT_COMMAND.matcher(event.getMessage());
        if (!m.matches())
        {
            return;
        }

        final int limit;
        try
        {
            limit = Integer.parseInt(m.group(1));
        }
        catch (NumberFormatException ex)
        {
            return;
        }
        final String targetName = m.group(2);

        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return;
        }

        if (targetName != null && !targetName.equalsIgnoreCase(player.getName()))
        {
            player.sendMessage(Component.text("Only admins can change the limit for other players!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        final Integer maxLimitObj = ConfigEntry.WORLDEDIT_LIMIT_MAX.getInteger();
        final int maxLimit = (maxLimitObj == null) ? -1 : maxLimitObj;
        if (maxLimit < 0)
        {
            return;
        }

        if (limit < 0 || limit > maxLimit)
        {
            if (ConfigEntry.WORLDEDIT_DEOP_ON_LIMIT_ABUSE.getBoolean())
            {
                player.setOp(false);
                FUtil.bcastMsg(player.getName() + " tried to set their WorldEdit limit to "
                    + limit + " and has been de-opped", NamedTextColor.RED);
            }
            else
            {
                FUtil.bcastMsg(player.getName() + " tried to set their WorldEdit limit to "
                    + limit, NamedTextColor.RED);
            }
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot set your limit higher than "
                + maxLimit + " or to -1!", NamedTextColor.RED));
            return;
        }

        setPlayerLimit(player.getUniqueId(), limit);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        final UUID uuid = event.getPlayer().getUniqueId();
        lastSelections.remove(uuid);
        playerLimits.remove(uuid);
        opThrottles.remove(uuid);
        final PermissionAttachment att = bypassAttachments.remove(uuid);
        if (att != null)
        {
            try
            {
                att.remove();
            }
            catch (Throwable ignored)
            {
            }
        }
    }

    public void setPlayerLimit(UUID uuid, int limit)
    {
        final Integer maxLimitObj = ConfigEntry.WORLDEDIT_LIMIT_MAX.getInteger();
        final int maxLimit = (maxLimitObj == null) ? -1 : maxLimitObj;
        if (limit < 0)
        {
            return;
        }
        final int effective = (maxLimit < 0) ? limit : Math.min(limit, maxLimit);
        playerLimits.put(uuid, effective);
    }

    public int getLimitFor(UUID uuid)
    {
        final Integer maxLimitObj = ConfigEntry.WORLDEDIT_LIMIT_MAX.getInteger();
        final int fallback = (maxLimitObj == null) ? Integer.MAX_VALUE : maxLimitObj;
        final Integer stored = playerLimits.get(uuid);
        return (stored == null) ? fallback : stored;
    }

    private static boolean throttleEnabled()
    {
        final Boolean v = ConfigEntry.WORLDEDIT_THROTTLE_ENABLED.getBoolean();
        return v == null || v;
    }

    private static int throttleMaxOps()
    {
        final Integer v = ConfigEntry.WORLDEDIT_THROTTLE_MAX_OPS.getInteger();
        return (v == null || v < 0) ? 5 : v;
    }

    private static long throttleWindowMs()
    {
        final Integer v = ConfigEntry.WORLDEDIT_THROTTLE_TIME_WINDOW.getInteger();
        return (v == null || v <= 0) ? 1000L : v.longValue();
    }

    private static int throttleEjectThreshold()
    {
        // Negative disables auto-eject (throttle only).
        final Integer v = ConfigEntry.WORLDEDIT_THROTTLE_MAX_CANCELLED_OPS.getInteger();
        return (v == null) ? 5 : v;
    }

    private static boolean isWorldEditOp(String message)
    {
        if (message == null || message.length() < 2 || message.charAt(0) != '/')
        {
            return false;
        }
        if (message.charAt(1) == '/')
        {
            return true;
        }
        String label = message.substring(1);
        final int sp = label.indexOf(' ');
        if (sp >= 0)
        {
            label = label.substring(0, sp);
        }
        final int colon = label.indexOf(':');
        if (colon <= 0)
        {
            return false;
        }
        final String ns = label.substring(0, colon).toLowerCase(Locale.ROOT);
        return ns.equals("worldedit") || ns.equals("fawe") || ns.equals("we");
    }

    private boolean checkOpThrottle(PlayerCommandPreprocessEvent event)
    {
        if (!throttleEnabled())
        {
            return false;
        }
        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return false;
        }
        if (!isWorldEditOp(event.getMessage()))
        {
            return false;
        }

        final UUID uuid = player.getUniqueId();
        final OpThrottle throttle = opThrottles.computeIfAbsent(uuid, k -> new OpThrottle());
        final long now = System.currentTimeMillis();
        final long window = throttleWindowMs();

        // Roll the rate window.
        if (now - throttle.windowStart > window)
        {
            throttle.windowStart = now;
            throttle.count = 0;
        }
        throttle.count++;
        if (throttle.count <= throttleMaxOps())
        {
            return false;
        }

        event.setCancelled(true);

        if (now - throttle.lastViolation > window * 5L)
        {
            throttle.violations = 0;
        }
        throttle.lastViolation = now;
        throttle.violations++;

        final int ejectThreshold = throttleEjectThreshold();
        if (ejectThreshold >= 0 && throttle.violations >= ejectThreshold)
        {
            opThrottles.remove(uuid);
            FUtil.bcastMsg(player.getName()
                + " was automatically ejected for spamming WorldEdit operations.", NamedTextColor.RED);
            plugin.ae.autoEject(player, "Kicked for spamming WorldEdit operations.");
        }
        else if (now - throttle.lastWarn > window)
        {
            throttle.lastWarn = now;
            player.sendMessage(Component.text(
                "You are issuing WorldEdit operations too quickly. Slow down.",
                NamedTextColor.RED));
        }
        return true;
    }

    private boolean checkPatternTypes(PlayerCommandPreprocessEvent event)
    {
        final List<String> blocked = ConfigEntry.WORLDEDIT_BLOCKED_BLOCK_TYPES.getStringList();
        if (blocked == null || blocked.isEmpty())
        {
            return false;
        }

        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return false;
        }

        final String message = event.getMessage();
        if (!isWorldEditOp(message))
        {
            return false;
        }

        final int sp = message.indexOf(' ');
        if (sp < 0)
        {
            return false;
        }
        final String args = message.substring(sp + 1).toLowerCase(Locale.ROOT);

        final Matcher m = BLOCK_TOKEN.matcher(args);
        while (m.find())
        {
            String id = m.group();
            final int colon = id.indexOf(':');
            if (colon >= 0)
            {
                id = id.substring(colon + 1);
            }
            for (String entry : blocked)
            {
                if (entry == null || entry.isEmpty())
                {
                    continue;
                }
                if (blockedIdMatches(id, entry.toLowerCase(Locale.ROOT)))
                {
                    event.setCancelled(true);
                    player.sendMessage(Component.text(
                        "The block type '" + id + "' cannot be used in your operation.",
                        NamedTextColor.RED));
                    FLog.warning("Operator " + player.getName() + " tried use a disallowed W/E block type (" + id
                        + "): " + message);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean blockedIdMatches(String id, String entry)
    {
        if (entry.indexOf('*') >= 0)
        {
            final String needle = entry.replace("*", "");
            return !needle.isEmpty() && id.contains(needle);
        }
        return id.equals(entry);
    }

    private boolean checkPatternComplexity(PlayerCommandPreprocessEvent event)
    {
        final Integer maxObj = ConfigEntry.WORLDEDIT_MAX_PATTERN_BLOCKS.getInteger();
        if (maxObj == null || maxObj < 0)
        {
            return false;
        }
        final int max = maxObj;

        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return false;
        }

        final String message = event.getMessage();
        if (!isWorldEditOp(message))
        {
            return false;
        }

        String msg = message.substring(1);
        if (!msg.isEmpty() && msg.charAt(0) == '/')
        {
            msg = msg.substring(1);
        }
        final String[] tokens = msg.trim().split("\\s+");
        // Skip token 0 (the command label); pattern/mask args follow.
        for (int i = 1; i < tokens.length; i++)
        {
            final int components = countPatternComponents(tokens[i]);
            if (components > max)
            {
                event.setCancelled(true);
                player.sendMessage(Component.text(
                    "You must use less than " + max
                        + " different kinds of block types in your operation.",
                    NamedTextColor.RED));
                return true;
            }
        }
        return false;
    }

    /**
     * groups "(...)", or "{...}" — do not count, so "oak_log[axis=x]" is one
     * block, while "a,b,c" is three.
     */
    private static int countPatternComponents(String token)
    {
        if (token == null || token.isEmpty())
        {
            return 0;
        }
        int depth = 0;
        int components = 1;
        for (int i = 0; i < token.length(); i++)
        {
            final char c = token.charAt(i);
            if (c == '[')
            {
                depth++;
            }
            else if (c == ']')
            {
                if (depth > 0)
                {
                    depth--;
                }
            }
            else if ((c == ',' || c == '&') && depth == 0)
            {
                components++;
            }
        }
        return components;
    }

    private boolean checkRadiusCommand(PlayerCommandPreprocessEvent event)
    {
        final Integer maxObj = ConfigEntry.WORLDEDIT_RADIUS_MAX.getInteger();
        if (maxObj == null || maxObj < 0)
        {
            return false;
        }
        final int max = maxObj;

        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return false;
        }

        String msg = event.getMessage();
        if (msg.length() < 2 || msg.charAt(0) != '/')
        {
            return false;
        }
        msg = msg.substring(1);
        if (msg.charAt(0) == '/')
        {
            msg = msg.substring(1);
        }
        final String[] tokens = msg.trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isEmpty())
        {
            return false;
        }

        final String cmd = tokens[0].toLowerCase();
        final Integer argIdxObj = RADIUS_COMMANDS.get(cmd);
        if (argIdxObj == null)
        {
            return false;
        }
        final int radiusTokenIdx = 1 + argIdxObj;
        if (tokens.length <= radiusTokenIdx)
        {
            return false;
        }

        final int radius = parseRadius(tokens[radiusTokenIdx]);
        if (radius < 0 || radius <= max)
        {
            return false;
        }

        event.setCancelled(true);
        player.sendMessage(Component.text(
            "You must use a radius of " + max + " or lesser.",
            NamedTextColor.RED));
        return true;
    }

    private static int parseRadius(String token)
    {
        if (token == null || token.isEmpty())
        {
            return -1;
        }
        int max = -1;
        for (String part : token.split(","))
        {
            try
            {
                final int v = Integer.parseInt(part.trim());
                if (v > max)
                {
                    max = v;
                }
            }
            catch (NumberFormatException ignored)
            {
                return -1;
            }
        }
        return max;
    }

    /**
     * @return the configured max selection volume, or -1 if disabled / unset.
     */
    private static long getMaxSelectionVolume()
    {
        final Integer raw = ConfigEntry.WORLDEDIT_MAX_SELECTION_VOLUME.getInteger();
        if (raw == null || raw <= 0)
        {
            return -1L;
        }
        return raw.longValue();
    }

    /**
     * Calculates the theoretical amount of modified blocks within a cuboid bound.
     * Prevents the exploitation of maths along extreme coordinates (e.g. "Infinity").
     */
    private static long safeSelectionVolume(BlockVector3 min, BlockVector3 max)
    {
        final long dx = (long) max.x() - min.x() + 1L;
        final long dy = (long) max.y() - min.y() + 1L;
        final long dz = (long) max.z() - min.z() + 1L;
        // normalized min/max always yield spans >= 1
        if (dx <= 0L || dy <= 0L || dz <= 0L)
        {
            return Long.MAX_VALUE;
        }
        try
        {
            return Math.multiplyExact(Math.multiplyExact(dx, dy), dz);
        }
        catch (ArithmeticException overflow)
        {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Strip leading slashes and any `worldedit:` / `fawe:` namespace prefix,
     * lowercased. Used to match command labels against {@link #SIZE_SENSITIVE_LABELS}.
     */
    private static String normalizeCommandLabel(String firstToken)
    {
        int i = 0;
        while (i < firstToken.length() && firstToken.charAt(i) == '/')
        {
            i++;
        }
        String s = firstToken.substring(i).toLowerCase(Locale.ROOT);
        final int colon = s.indexOf(':');
        if (colon >= 0)
        {
            s = s.substring(colon + 1);
        }
        return s;
    }

    /**
     * Runs at LOWEST priority so we cancel before WorldEdit allocates a
     * clipboard / region copy from {@code region.getVolume()}.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSizeSensitiveCommand(PlayerCommandPreprocessEvent event)
    {
        final long cap = getMaxSelectionVolume();
        if (cap <= 0)
        {
            return;
        }
        final String msg = event.getMessage();
        if (msg == null || msg.isEmpty())
        {
            return;
        }
        final int sp = msg.indexOf(' ');
        final String firstToken = (sp < 0) ? msg : msg.substring(0, sp);
        final String label = normalizeCommandLabel(firstToken);
        if (!SIZE_SENSITIVE_LABELS.contains(label))
        {
            return;
        }

        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return;
        }
        if (worldEditPlugin == null)
        {
            return;
        }

        try
        {
            final com.sk89q.worldedit.entity.Player wePlayer = worldEditPlugin.wrapPlayer(player);
            final LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
            final Region region;
            try
            {
                region = session.getSelection(wePlayer.getWorld());
            }
            catch (IncompleteRegionException ex)
            {
                return;
            }
            final long volume = safeSelectionVolume(
                region.getMinimumPoint(), region.getMaximumPoint());
            if (volume <= cap)
            {
                return;
            }

            event.setCancelled(true);
            player.sendMessage(Component.text(
                "Your WorldEdit selection (" + volume + " blocks) exceeds the max of "
                    + cap + ". Operation blocked.",
                NamedTextColor.RED));
            FLog.warning("Blocked oversized WorldEdit op from " + player.getName()
                + " (volume=" + volume + ", cap=" + cap + "): " + msg);
            try
            {
                session.getRegionSelector(wePlayer.getWorld()).clear();
                lastSelections.remove(player.getUniqueId());
            }
            catch (Throwable ignored)
            {
            }
        }
        catch (Throwable t)
        {
        }
    }

    /**
     * Roll back the last {@code count} WorldEdit operations from {@code player}.
     */
    public void undo(Player player, int count)
    {
        if (worldEditPlugin == null || player == null || count <= 0)
        {
            return;
        }
        try
        {
            final com.sk89q.worldedit.entity.Player wePlayer = worldEditPlugin.wrapPlayer(player);
            final LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
            for (int i = 0; i < count; i++)
            {
                if (session.undo(null, wePlayer) == null)
                {
                    break;
                }
            }
        }
        catch (Throwable t)
        {
            FLog.severe(t);
        }
    }

    public int cancel(Player player)
    {
        if (worldEditPlugin == null || player == null)
        {
            return 0;
        }
        try
        {
            final com.sk89q.worldedit.entity.Player wePlayer = worldEditPlugin.wrapPlayer(player);
            final java.lang.reflect.Method cancelMethod =
                wePlayer.getClass().getMethod("cancel", boolean.class);
            final Object result = cancelMethod.invoke(wePlayer, true);
            return (result instanceof Integer) ? (Integer) result : 0;
        }
        catch (NoSuchMethodException ex)
        {
            return 0;
        }
        catch (Throwable t)
        {
            FLog.warning("Failed to cancel WorldEdit operations for "
                + player.getName() + ": " + t.getMessage());
            return 0;
        }
    }

    private void pollSelections()
    {
        final SessionManager sessions = WorldEdit.getInstance().getSessionManager();
        for (Player bukkitPlayer : Bukkit.getOnlinePlayers())
        {
            if (plugin.al.isAdmin(bukkitPlayer))
            {
                continue;
            }
            try
            {
                final com.sk89q.worldedit.entity.Player wePlayer = worldEditPlugin.wrapPlayer(bukkitPlayer);
                final UUID uuid = wePlayer.getUniqueId();
                final LocalSession session = sessions.get(wePlayer);

                final Region region;
                try
                {
                    region = session.getSelection(wePlayer.getWorld());
                }
                catch (IncompleteRegionException ex)
                {
                    lastSelections.remove(uuid);
                    continue;
                }

                final BlockVector3 min = region.getMinimumPoint();
                final BlockVector3 max = region.getMaximumPoint();
                final RegionSnapshot snap = new RegionSnapshot(min, max);
                final RegionSnapshot prev = lastSelections.get(uuid);
                if (prev != null && prev.equals(snap))
                {
                    continue;
                }

                lastSelections.put(uuid, snap);

                final long cap = getMaxSelectionVolume();
                if (cap > 0)
                {
                    final long volume = safeSelectionVolume(min, max);
                    if (volume > cap)
                    {
                        bukkitPlayer.sendMessage(Component.text(
                            "Your WorldEdit selection (" + volume + " blocks) exceeds the max of "
                                + cap + ". Selection cleared.",
                            NamedTextColor.RED));
                        session.getRegionSelector(wePlayer.getWorld()).clear();
                        lastSelections.remove(uuid);
                        continue;
                    }
                }

                final World world = Bukkit.getWorld(wePlayer.getWorld().getName());
                if (world == null)
                {
                    continue;
                }

                final Vector minV = new Vector(min.x(), min.y(), min.z());
                final Vector maxV = new Vector(max.x(), max.y(), max.z());

                if (plugin.pa.isInProtectedArea(minV, maxV, world.getName()))
                {
                    bukkitPlayer.sendMessage(Component.text("The region that you selected contained a protected area. Selection cleared.", NamedTextColor.RED));
                    session.getRegionSelector(wePlayer.getWorld()).clear();
                    lastSelections.remove(uuid);
                }
            }
            catch (Exception ignored)
            {
            }
        }
    }

    private final class ProtectedAreaExtent extends AbstractDelegateExtent
    {

        private final com.sk89q.worldedit.entity.Player wePlayer;
        private final com.sk89q.worldedit.world.World weWorld;
        private boolean checked = false;
        private boolean denied = false;
        private BlockVector3 minPos;
        private BlockVector3 maxPos;

        ProtectedAreaExtent(Extent parent,
                            com.sk89q.worldedit.entity.Player wePlayer,
                            com.sk89q.worldedit.world.World weWorld)
        {
            super(parent);
            this.wePlayer = wePlayer;
            this.weWorld = weWorld;
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pos, T block)
            throws WorldEditException
        {
            if (minPos == null)
            {
                minPos = pos;
            }
            else if (pos.x() < minPos.x() || pos.y() < minPos.y() || pos.z() < minPos.z())
            {
                minPos = BlockVector3.at(
                    Math.min(minPos.x(), pos.x()),
                    Math.min(minPos.y(), pos.y()),
                    Math.min(minPos.z(), pos.z()));
            }

            if (maxPos == null)
            {
                maxPos = pos;
            }
            else if (pos.x() > maxPos.x() || pos.y() > maxPos.y() || pos.z() > maxPos.z())
            {
                maxPos = BlockVector3.at(
                    Math.max(maxPos.x(), pos.x()),
                    Math.max(maxPos.y(), pos.y()),
                    Math.max(maxPos.z(), pos.z()));
            }

            if (!checked)
            {
                checked = true;
                denied = shouldDeny();
            }
            if (denied)
            {
                return false;
            }
            return super.setBlock(pos, block);
        }

        private boolean shouldDeny()
        {
            final Player bukkitPlayer = Bukkit.getPlayer(wePlayer.getUniqueId());
            if (bukkitPlayer == null)
            {
                return false;
            }
            if (plugin.al.isAdmin(bukkitPlayer))
            {
                return false;
            }

            final World world = Bukkit.getWorld(weWorld.getName());
            if (world == null || minPos == null || maxPos == null)
            {
                return false;
            }

            final Vector min = new Vector(minPos.x(), minPos.y(), minPos.z());
            final Vector max = new Vector(maxPos.x(), maxPos.y(), maxPos.z());
            if (plugin.pa.isInProtectedArea(min, max, world.getName()))
            {
                bukkitPlayer.sendMessage(Component.text("You cannot perform WorldEdit operations in a protected area!", NamedTextColor.RED));
                return true;
            }
            return false;
        }
    }

    private final class LimitExtent extends AbstractDelegateExtent
    {

        private final UUID uuid;
        private final int limit;
        private final AtomicInteger count = new AtomicInteger();
        private final AtomicBoolean warned = new AtomicBoolean();

        LimitExtent(Extent parent, UUID uuid, int limit)
        {
            super(parent);
            this.uuid = uuid;
            this.limit = limit;
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pos, T block)
            throws WorldEditException
        {
            if (count.incrementAndGet() > limit)
            {
                if (warned.compareAndSet(false, true))
                {
                    Bukkit.getScheduler().runTask(plugin, () ->
                    {
                        final Player p = Bukkit.getPlayer(uuid);
                        if (p != null)
                        {
                            p.sendMessage(Component.text(
                                "WorldEdit limit reached (" + limit + " blocks). Operation halted.",
                                NamedTextColor.RED));
                        }
                    });
                }
                throw new MaxChangedBlocksException(limit);
            }
            return super.setBlock(pos, block);
        }
    }

    private static final class OpThrottle
    {
        long windowStart;
        int count;
        long lastViolation;
        int violations;
        long lastWarn;
    }

    private static final class RegionSnapshot
    {
        final BlockVector3 min;
        final BlockVector3 max;

        RegionSnapshot(BlockVector3 min, BlockVector3 max)
        {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof RegionSnapshot))
            {
                return false;
            }
            RegionSnapshot r = (RegionSnapshot) o;
            return min.equals(r.min) && max.equals(r.max);
        }

        @Override
        public int hashCode()
        {
            return min.hashCode() * 31 + max.hashCode();
        }
    }
}
