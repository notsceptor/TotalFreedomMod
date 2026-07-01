package me.totalfreedom.totalfreedommod;

import java.util.List;
import java.util.Locale;
import me.totalfreedom.totalfreedommod.bridge.CoreProtectBridge;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.coreprotect.CoreProtectAPI;
import net.coreprotect.CoreProtectAPI.ParseResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class LogstickManager extends FreedomService
{

    public LogstickManager(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
    }

    @Override
    protected void onStop()
    {
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
        {
            return;
        }

        final Player player = event.getPlayer();
        if (!plugin.rm.getRank(player).isAtLeast(me.totalfreedom.totalfreedommod.rank.Rank.SUPER_ADMIN) || !player.hasPermission("customplugin.logstick"))
        {
            return;
        }

        final ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.STICK)
        {
            return;
        }

        final ItemMeta meta = item.getItemMeta();
        if (meta == null)
        {
            return;
        }

        final NamespacedKey key = new NamespacedKey(plugin, "logstick");
        boolean isLogStick = meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);

        if (!isLogStick)
        {
            final Component displayName = meta.displayName();
            if (displayName != null)
            {
                String plainName = AdventureUtil.componentToPlainText(displayName);
                if (plainName.equalsIgnoreCase("Logstick"))
                {
                    isLogStick = true;
                }
            }
        }

        if (!isLogStick)
        {
            return;
        }

        event.setCancelled(true);

        if (plugin.cpb == null || !plugin.cpb.isEnabled())
        {
            player.sendMessage(Component.text("CoreProtect integration is not enabled on this server.", NamedTextColor.RED));
            return;
        }

        final CoreProtectAPI cpAPI = plugin.cpb.getCoreProtectAPI();
        if (cpAPI == null)
        {
            player.sendMessage(Component.text("CoreProtect API is not available.", NamedTextColor.RED));
            return;
        }

        final Block block = event.getClickedBlock();
        if (block == null)
        {
            return;
        }

        player.sendMessage(Component.text("Inspecting block history...", NamedTextColor.AQUA));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
        {
            try
            {
                // 30 days lookup = 2,592,000 seconds
                final List<String[]> lookup = cpAPI.blockLookup(block, 2592000);

                if (lookup == null || lookup.isEmpty())
                {
                    player.sendMessage(Component.text("No history found for this block.", NamedTextColor.GRAY));
                    return;
                }

                for (final String[] row : lookup)
                {
                    final ParseResult result = cpAPI.parseResult(row);
                    if (result == null)
                    {
                        continue;
                    }

                    final long diffSeconds = (System.currentTimeMillis() / 1000L) - result.getTimestamp();
                    final String timeStr = formatTimeDiff(diffSeconds);

                    String actionStr = "interacted";
                    String actionColor = "&e";
                    if (result.getActionId() == 0)
                    {
                        actionStr = "broke";
                        actionColor = "&c";
                    }
                    else if (result.getActionId() == 1)
                    {
                        actionStr = "placed";
                        actionColor = "&a";
                    }

                    final String blockType = result.getType().name().toLowerCase();
                    final String message = "&7" + timeStr + " - &3" + result.getPlayer() + " " + actionColor + actionStr + " &3" + blockType;

                    player.sendMessage(AdventureUtil.legacyToComponent(message));
                }
            }
            catch (Exception ex)
            {
                FLog.severe("Error looking up CoreProtect logs for logstick: " + ex.getMessage());
                FLog.severe(ex);
                player.sendMessage(Component.text("An error occurred while looking up block history.", NamedTextColor.RED));
            }
        });
    }

    private String formatTimeDiff(long diffSeconds)
    {
        if (diffSeconds < 0)
        {
            diffSeconds = 0;
        }
        if (diffSeconds < 60)
        {
            return diffSeconds + "s ago";
        }
        final long diffMinutes = diffSeconds / 60;
        if (diffMinutes < 60)
        {
            return diffMinutes + "m ago";
        }
        final double diffHours = (double) diffSeconds / 3600.0;
        if (diffHours < 24.0)
        {
            return String.format(Locale.ENGLISH, "%.2fh ago", diffHours);
        }
        final double diffDays = diffHours / 24.0;
        return String.format(Locale.ENGLISH, "%.2fd ago", diffDays);
    }
}
