package me.totalfreedom.totalfreedommod;

import me.totalfreedom.api.FreedomAPI;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import me.totalfreedom.totalfreedommod.player.FPlayer;

public class Orbiter extends FreedomService
{

    public Orbiter(FreedomAPI plugin)
    {
        super(plugin);
    }

    @Override
    public void onStart()
    {
    }

    @Override
    public void onStop()
    {
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        if (!event.hasChangedPosition())
        {
            return;
        }

        final Player player = event.getPlayer();
        final FPlayer fPlayer = plugin.players().getPlayer(player);

        if (!fPlayer.isOrbiting())
        {
            return;
        }

        if (player.getVelocity().length() < fPlayer.orbitStrength() * (2.0 / 3.0))
        {
            player.setVelocity(new Vector(0, fPlayer.orbitStrength(), 0));
        }
    }

}
