package me.totalfreedom.totalfreedommod.freeze;

import me.totalfreedom.api.FreedomAPI;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.util.FUtil;

import lombok.Getter;

public class Freezer extends FreedomService
{

    @Getter
    private boolean globalFreeze = false;

    public Freezer(FreedomAPI plugin)
    {
        super(plugin);
    }

    @Override
    public void onStart()
    {
        globalFreeze = false;
    }

    @Override
    public void onStop()
    {
    }

    public void setGlobalFreeze(boolean frozen)
    {
        this.globalFreeze = frozen;
    }

    public void purge()
    {
        this.globalFreeze = false;

        for (Player player : server.getOnlinePlayers())
        {
            plugin.players().getPlayer(player).getFreezeData().setFrozen(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        if (!event.hasChangedPosition())
        {
            return;
        }

        final Player player = event.getPlayer();

        if (plugin.admins().isAdmin(player))
        {
            return;
        }

        final FreezeData fd = plugin.players().getPlayer(player).getFreezeData();
        if (!fd.isFrozen() && !globalFreeze)
        {
            return;
        }

        FUtil.setFlying(player, true);

        Location loc = fd.getLocation();
        if (loc == null)
        {
            loc = event.getFrom();
        }

        event.setTo(loc);
    }

}
