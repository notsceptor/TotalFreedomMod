package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import me.totalfreedom.api.world.IWorldAccessGate;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.world.GeneratedWorld;
import me.totalfreedom.api.world.WorldTime;
import me.totalfreedom.api.world.WorldWeather;

@Command(name = "adminworld", description = "Go to, or manage the AdminWorld.", usage = "/adminworld [guest <add | remove | purge | list>]", aliases = {"aw"})
@Permission(permission = "tfm.world.adminworld") // default permission is op, and default source is BOTH
public class Command_adminworld extends FCommand
{

    @Callback
    @Permission(source = SourceType.ONLY_IN_GAME, permission = "tfm.world.adminworld")
    public void moveToWorld(Player player)
    {
        final GeneratedWorld adminworld = plugin().worlds().adminworld();
        final IWorldAccessGate gate = plugin().worlds().adminGate();

        World adminWorld = null;
        try
        {
            adminWorld = adminworld == null ? null : adminworld.getWorld();
        }
        catch (Exception ignored) {}

        if (adminWorld == null || gate == null || player.getWorld().getUID().equals(adminWorld.getUID()))
        {
            msg(player, "<gray>Going to the main world.");
            player.teleport(server().getWorlds().get(0).getSpawnLocation().clone());
        }
        else if (gate.canAccess(player))
        {
            msg(player, "<gray>Going to the AdminWorld.");
            adminworld.sendToWorld(player);
        }
        else
        {
            msg(player, "<red>You don't have permission to access the AdminWorld.");
        }
    }

    @Callback
    @Subcommand("guest add")
    @Permission(source = SourceType.ONLY_IN_GAME, permission = "tfm.world.adminworld.manage")
    public void addGuest(Player sender, Player target)
    {
        final IWorldAccessGate gate = requireGate(sender);
        if (gate == null)
            return;

        if (gate.addGuest(target, sender))
        {
            adminAction(sender, "<aqua>AdminWorld guest added: <target>",
                    Placeholder.unparsed("target", target.getName()));
            return;
        }
        msg(sender, "<red>Could not add player to guest list.");
    }

    @Callback
    @Subcommand("guest list")
    @Permission(permission = "tfm.world.adminworld")
    public void listGuests(CommandSender sender)
    {
        final IWorldAccessGate gate = requireGate(sender);
        if (gate == null)
            return;

        if (gate.hasGuests())
        {
            // TODO: Probably not use this weird guestListToString
            msg(sender, "<aqua>AdminWorld guest list: <list>",
                    Placeholder.parsed("list", gate.guestListToString()));
            return;
        }

        msg(sender, "<red>There are no AdminWorld guests.");
    }

    @Callback
    @Subcommand("guest remove")
    @Permission(permission = "tfm.world.adminworld.manage")
    public void removeGuest(CommandSender sender, Player target)
    {
        final IWorldAccessGate gate = requireGate(sender);
        if (gate == null)
            return;

        if (gate.removeGuest(target))
        {
            adminAction(sender, "<aqua>AdminWorld guest removed: <target>",
                    Placeholder.unparsed("target", target.getName()));
        }
        else
        {
            msg(sender, "<gray>Can't find guest entry for <target>.",
                    Placeholder.unparsed("target", target.getName()));
        }
    }

    @Callback
    @Subcommand("guest purge")
    @Permission(permission = "tfm.world.adminworld.manage")
    public void purgeGuests(CommandSender sender)
    {
        final IWorldAccessGate gate = requireGate(sender);
        if (gate == null)
            return;

        gate.purgeGuestList();
        adminAction(sender, "<aqua>AdminWorld guest list purged.");
    }

    @Callback
    @Subcommand("time")
    @Permission(permission = "tfm.world.adminworld.manage")
    public void setTime(CommandSender sender, WorldTime time)
    {
        final IWorldAccessGate gate = requireGate(sender);
        if (gate == null)
            return;

        gate.setTimeOfDay(time);
        msg(sender, "<gray>AdminWorld time set to <time>.",
                Placeholder.unparsed("time", time.name()));
    }

    @Callback
    @Subcommand("weather")
    @Permission(permission = "tfm.world.adminworld.manage")
    public void setWeather(CommandSender sender, WorldWeather weather)
    {
        final IWorldAccessGate gate = requireGate(sender);
        if (gate == null)
            return;

        gate.setWeatherMode(weather);
        msg(sender, "<aqua>AdminWorld weather set to <weather>.",
                Placeholder.unparsed("weather", weather.name()));
    }

    /** The AdminWorld's gate, or null (with an error already sent to sender) if it isn't up yet. */
    private IWorldAccessGate requireGate(CommandSender sender)
    {
        final IWorldAccessGate gate = plugin().worlds().adminGate();

        if (gate == null)
            msg(sender, "<red>The AdminWorld is not available right now.");

        return gate;
    }
}
