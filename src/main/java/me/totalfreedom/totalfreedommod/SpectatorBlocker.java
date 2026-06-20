package me.totalfreedom.totalfreedommod;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class SpectatorBlocker extends FreedomService
{

    public SpectatorBlocker(TotalFreedomMod plugin)
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerStartSpectatingEntity(PlayerStartSpectatingEntityEvent event)
    {
        if (!ConfigEntry.BLOCK_SPECTATOR_TELEPORT.getBoolean())
        {
            return;
        }

        Entity newTarget = event.getNewSpectatorTarget();
        if (!(newTarget instanceof Player))
        {
            return;
        }

        Player player = event.getPlayer();
        if (newTarget.equals(player))
        {
            return;
        }

        if (plugin.al.isAdmin(player))
        {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(Component.text("Spectating other players is restricted to admins.", NamedTextColor.GRAY));
    }

}
