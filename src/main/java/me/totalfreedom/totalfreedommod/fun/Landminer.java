package me.totalfreedom.totalfreedommod.fun;

import me.totalfreedom.api.FreedomAPI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;

public class Landminer extends FreedomService
{

    private final List<Landmine> landmines = new ArrayList<>();

    public Landminer(FreedomAPI plugin)
    {
        super(plugin);
    }

    @Override
    public void onStart()
    {
        landmines.clear();
    }

    @Override
    public void onStop()
    {
    }

    public void add(Landmine landmine)
    {
        landmines.add(landmine);
    }

    public void remove(Landmine landmine)
    {
        landmines.remove(landmine);
    }

    public List<Landmine> getLandmines()
    {
        return landmines;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        if (landmines.isEmpty() || !event.hasChangedPosition())
            return;

        if (!(ConfigEntry.LANDMINES_ENABLED.getBoolean() && ConfigEntry.ALLOW_EXPLOSIONS.getBoolean()))
            return;

        final Player player = event.getPlayer();

        final Iterator<Landmine> lit = landmines.iterator();
        while (lit.hasNext())
        {
            final Landmine landmine = lit.next();

            final Location location = landmine.location;
            if (location.getBlock().getType() != Material.TNT || !player.getWorld().equals(location.getWorld()))
            {
                lit.remove();
                continue;
            }

            if (player.getLocation().distanceSquared(location) >= (landmine.radius * landmine.radius) || landmine.planter.equals(player))
                break;

            landmine.location.getBlock().setType(Material.AIR);

            final TNTPrimed tnt1 = location.getWorld().spawn(location, TNTPrimed.class);
            tnt1.setFuseTicks(40);
            tnt1.setPassenger(player);
            tnt1.setVelocity(new Vector(0.0, 2.0, 0.0));

            final TNTPrimed tnt2 = location.getWorld().spawn(player.getLocation(), TNTPrimed.class);
            tnt2.setFuseTicks(1);

            player.setGameMode(GameMode.SURVIVAL);
            lit.remove();
        }
    }

    public static final record Landmine(Location location, Player planter, double radius)
    {
        @Override
        public String toString()
        {
            return this.location.toString() + ", " + this.radius + ", " + this.planter.getName();
        }
    }

}
