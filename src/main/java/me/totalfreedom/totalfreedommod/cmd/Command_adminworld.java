package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.totalfreedommod.world.WorldTime;
import me.totalfreedom.totalfreedommod.world.WorldWeather;

@Command(name = "adminworld", description = "", usage = "/adminworld [guest <add | remove | purge | list>]")
@Permission(permission = "tfm.world.adminworld") // default permission is op, and default source is BOTH
public class Command_adminworld extends FCommand 
{

    @Callback
    @Permission(source = SourceType.ONLY_IN_GAME, permission = "tfm.world.adminworld")
    public void moveToWorld(Player player) 
    {
        World adminWorld = null;
        try
        {
            adminWorld = plugin.wm.adminworld.getWorld();
        }
        catch (Exception ignored) {}

        if (adminWorld == null || player.getWorld().equals(adminWorld))
        {
            msg(player, "Going to the main world.");
            player.teleport(server.getWorlds().get(0).getSpawnLocation().clone());
        }
        else if (plugin.wm.adminworld.canAccessWorld(player))
        {
            msg(player, "Going to the AdminWorld.");
            plugin.wm.adminworld.sendToWorld(player);
        }
        else
        {
            msg(player, "You don't have permission to access the AdminWorld.");
        }
    }

    @Callback
    @Subcommand("guest add")
    @Permission(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.world.adminworld")
    public void addGuest(Player sender, Player target) 
    {
        if (plugin.wm.adminworld.addGuest(target, sender))
        {
            FUtil.adminAction(sender.getName(), "AdminWorld guest added: " + target.getName(), false);
            return;
        }
        msg(sender, "Could not add player to guest list.");
    }

    @Callback
    @Subcommand("guest list")
    @Permission(permission = "tfm.world.adminworld")
    public void listGuests(CommandSender sender)
    {
        if (plugin.wm.adminworld.hasGuests())
        {
            msg(sender, "AdminWorld guest list: " + plugin.wm.adminworld.guestListToString());
            return;
        }

        msg(sender, "There are no AdminWorld guests.");
    }

    @Callback
    @Subcommand("guest remove")
    @Permission(level = Rank.SUPER_ADMIN, permission = "tfm.world.adminworld")
    public void removeGuest(CommandSender sender, Player target) 
    {
        if (plugin.wm.adminworld.removeGuest(target))
        {
            FUtil.adminAction(sender.getName(), "AdminWorld guest removed: " + target.getName(), false);
        }
        else
        {
            msg(sender, "Can't find guest entry for: " + target.getName());
        }
    }

    @Callback
    @Subcommand("guest purge")
    @Permission(level = Rank.SUPER_ADMIN, permission = "tfm.world.adminworld")
    public void purgeGuests(CommandSender sender)
    {
        plugin.wm.adminworld.purgeGuestList();
        FUtil.adminAction(sender.getName(), "AdminWorld guest list purged.", false);
    }

    @Callback
    @Subcommand("time")
    @Permission(level = Rank.SUPER_ADMIN, permission = "tfm.world.adminworld")
    public void setTime(CommandSender sender, WorldTime time)
    {
        plugin.wm.adminworld.setTimeOfDay(time);
        msg(sender, "AdminWorld time set to: " + time.name());
    }

    @Callback
    @Subcommand("weather")
    @Permission(level = Rank.SUPER_ADMIN, permission = "tfm.world.adminworld")
    public void setWeather(CommandSender sender, WorldWeather weather)
    {
        plugin.wm.adminworld.setWeatherMode(weather);
        msg(sender, "AdminWorld eather set to: " + weather.name());
    }
}
