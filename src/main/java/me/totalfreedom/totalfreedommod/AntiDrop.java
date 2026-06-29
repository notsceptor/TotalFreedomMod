package me.totalfreedom.totalfreedommod;

import java.util.Iterator;
import java.util.List;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class AntiDrop extends FreedomService
{

    public AntiDrop(TotalFreedomMod plugin)
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

    private static boolean enabled()
    {
        return ConfigEntry.ANTIDROP_ENABLED.getBoolean(true);
    }

    private static long window()
    {
        final int v = ConfigEntry.ANTIDROP_TIME_WINDOW.getInteger(1000);
        return v <= 0 ? 1000L : v;
    }

    // Max drops per window before drops are cancelled; -1 disables the throttle.
    private static int dropLimit()
    {
        return ConfigEntry.ANTIDROP_DROP_LIMIT.getInteger(20);
    }

    // Drops per window that trigger an auto-eject; -1 throttles only.
    private static int dropEjectLimit()
    {
        return ConfigEntry.ANTIDROP_DROP_EJECT_LIMIT.getInteger(60);
    }

    // Max item quantity per window before drops are cancelled; -1 disables.
    private static int dropItemLimit()
    {
        return ConfigEntry.ANTIDROP_DROP_ITEM_LIMIT.getInteger(512);
    }

    // Item quantity per window that triggers an auto-eject; -1 throttles only.
    private static int dropItemEjectLimit()
    {
        return ConfigEntry.ANTIDROP_DROP_ITEM_EJECT_LIMIT.getInteger(-1);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDropItem(PlayerDropItemEvent event)
    {
        if (!enabled())
        {
            return;
        }

        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return;
        }

        final long window = window();
        final FPlayer fPlayer = plugin.pl.getPlayer(player);

        final ItemStack stack = event.getItemDrop().getItemStack();
        final int amount = (stack == null) ? 1 : Math.max(stack.getAmount(), 1);

        boolean cancel = false;
        boolean eject = false;
        boolean justCrossed = false;

        // Axis 1: number of drop events this window (entity-flood protection).
        // A dropped stack is one entity, so a 64-stack counts as a single drop.
        final int eventLimit = dropLimit();
        if (eventLimit >= 0)
        {
            final int before = fPlayer.incrementDropCount(window);
            final int ejectAt = dropEjectLimit();
            if (ejectAt >= 0 && before >= ejectAt)
            {
                eject = true;
            }
            else if (before >= eventLimit)
            {
                cancel = true;
                justCrossed |= (before == eventLimit);
            }
        }

        // Axis 2: total item quantity this window (mass-dump / over-stacked
        // crash-item protection). A 64-stack counts as 64.
        final int itemLimit = dropItemLimit();
        if (itemLimit >= 0)
        {
            final int before = fPlayer.incrementDropItemCount(amount, window);
            final int ejectAt = dropItemEjectLimit();
            if (ejectAt >= 0 && before + amount > ejectAt)
            {
                eject = true;
            }
            else if (before + amount > itemLimit)
            {
                cancel = true;
                justCrossed |= (before <= itemLimit);
            }
        }

        if (eject)
        {
            ejectForDropFlood(player, fPlayer);
            event.setCancelled(true);
            return;
        }

        if (cancel)
        {
            event.setCancelled(true);
            // Warn at most once per window — only on the drop that first crosses
            // a soft limit — so the throttle can't itself become a chat flood.
            if (justCrossed)
            {
                FUtil.playerMsg(player, "You are dropping items too quickly.", NamedTextColor.GRAY);
            }
        }
    }

    // Container breaks (chests, barrels, dispensers, etc.) spill their entire
    // contents as a batch of item entities in a single event, which never fires
    // PlayerDropItemEvent. We still throttle the spill so a stuffed container
    // can't lag-bomb the server, BUT this path never auto-ejects and never
    // touches the per-player drop counters used by drop_eject_limit /
    // drop_item_eject_limit: an unwitting player can break a chest that someone
    // else filled, and must not be punished (nor pushed toward a punishment on
    // their next manual drop) for contents they didn't create. Throttling is
    // therefore local to this single break. Shulker boxes drop as a single NBT
    // item and so naturally count as one. (Admins exempt.)
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event)
    {
        if (!enabled())
        {
            return;
        }

        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return;
        }

        final List<Item> items = event.getItems();
        if (items.isEmpty())
        {
            return;
        }

        final int eventLimit = dropLimit();
        final int itemLimit = dropItemLimit();
        if (eventLimit < 0 && itemLimit < 0)
        {
            return;
        }

        int events = 0;
        int quantity = 0;
        boolean warned = false;
        final Iterator<Item> it = items.iterator();
        while (it.hasNext())
        {
            final ItemStack stack = it.next().getItemStack();
            final int amount = (stack == null) ? 1 : Math.max(stack.getAmount(), 1);

            boolean cancelThis = false;

            if (eventLimit >= 0 && ++events > eventLimit)
            {
                cancelThis = true;
            }

            if (itemLimit >= 0 && (quantity += amount) > itemLimit)
            {
                cancelThis = true;
            }

            if (cancelThis)
            {
                it.remove();
                if (!warned)
                {
                    warned = true;
                    FUtil.playerMsg(player, "That dropped too many items at once.", NamedTextColor.GRAY);
                }
            }
        }
    }

    private void ejectForDropFlood(Player player, FPlayer fPlayer)
    {
        FUtil.bcastMsg(player.getName() + " was automatically kicked for dropping too many items.", NamedTextColor.RED);
        plugin.ae.autoEject(player, "Kicked for dropping too many items at once.");
        fPlayer.resetDropCount();
        fPlayer.resetDropItemCount();
    }

}
