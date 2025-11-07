package me.totalfreedom.totalfreedommod.bridge;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.worldedit.LimitChangedEvent;
import me.totalfreedom.worldedit.SelectionChangedEvent;
import me.totalfreedom.worldedit.WorldEditOperationEvent;

import net.pravian.aero.component.PluginListener;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;

public class WorldEditListener extends PluginListener<TotalFreedomMod>
{

    public WorldEditListener(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @EventHandler
    public void onSelectionChange(final SelectionChangedEvent event)
    {
        final Player player = event.getPlayer();

        if (plugin.al.isAdmin(player))
        {
            return;
        }

        if (plugin.pa.isInProtectedArea(
                event.getMinVector(),
                event.getMaxVector(),
                event.getWorld().getName()))
        {

            player.sendMessage(ChatColor.RED + "The region that you selected contained a protected area. Selection cleared.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLimitChanged(LimitChangedEvent event)
    {
        final Player player = event.getPlayer();

        if (plugin.al.isAdmin(player))
        {
            return;
        }

        if (!event.getPlayer().equals(event.getTarget()))
        {
            player.sendMessage(ChatColor.RED + "Only admins can change the limit for other players!");
            event.setCancelled(true);
        }

        if (event.getLimit() < 0 || event.getLimit() > 10000)
        {
            player.setOp(false);
            FUtil.bcastMsg(event.getPlayer().getName() + " tried to set their WorldEdit limit to " + event.getLimit() + " and has been de-opped", ChatColor.RED);
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot set your limit higher than 10000 or to -1!");
        }
    }

    @EventHandler
    public void onWorldEditOperation(final WorldEditOperationEvent event)
    {
        handleWorldEditOperation(event);
    }

    /**
     * Handles WorldEditOperationEvent using reflection to work with the real event class from TF-WorldEdit.
     * This method can be called with either the stub class or the real event class.
     */
    public void handleWorldEditOperation(org.bukkit.event.Event event)
    {
        try
        {
            // Use reflection to get event properties since we might have the real event class
            java.lang.reflect.Method getPlayerMethod = event.getClass().getMethod("getPlayer");
            Player player = (Player) getPlayerMethod.invoke(event);

            if (player == null)
            {
                return;
            }

            if (plugin.al.isAdmin(player))
            {
                return;
            }

            java.lang.reflect.Method getMinVectorMethod = event.getClass().getMethod("getMinVector");
            java.lang.reflect.Method getMaxVectorMethod = event.getClass().getMethod("getMaxVector");
            java.lang.reflect.Method getWorldMethod = event.getClass().getMethod("getWorld");

            Vector min = (Vector) getMinVectorMethod.invoke(event);
            Vector max = (Vector) getMaxVectorMethod.invoke(event);
            org.bukkit.World world = (org.bukkit.World) getWorldMethod.invoke(event);
            String worldName = world != null ? world.getName() : null;

            if (min == null || max == null || worldName == null)
            {
                return;
            }

            if (plugin.pa.isInProtectedArea(min, max, worldName))
            {
                java.lang.reflect.Method setCancelledMethod = event.getClass().getMethod("setCancelled", boolean.class);
                setCancelledMethod.invoke(event, true);
                player.sendMessage(ChatColor.RED + "You cannot perform WorldEdit operations in a protected area!");
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Error handling WorldEditOperationEvent: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

}
