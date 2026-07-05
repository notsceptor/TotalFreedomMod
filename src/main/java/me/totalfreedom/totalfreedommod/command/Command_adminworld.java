package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.totalfreedommod.world.WorldTime;
import me.totalfreedom.totalfreedommod.world.WorldWeather;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.world.adminworld")
@CommandParameters(description = "Go to the AdminWorld.",
        usage = "/<command> [guest < list | purge | add <player> | remove <player> > | time <morning | noon | evening | night> | weather <off | rain | storm>]")
public class Command_adminworld extends FreedomCommand
{

    @CommandDispatchTarget
    public boolean teleportAdminWorld(CommandContext ctx)
    {
        Player playerSender = ctx.getPlayerSender();

        if (ctx.isSenderConsole())
        {
            return true;
        }

        World adminWorld = null;
        try
        {
            adminWorld = plugin.wm.adminworld.getWorld();
        }
        catch (Exception _)
        {
        }

        if (adminWorld == null || playerSender.getWorld().equals(adminWorld))
        {
            msg("Going to the main world.");
            playerSender.teleport(server.getWorlds().get(0).getSpawnLocation());
        }
        else if (plugin.wm.adminworld.canAccessWorld(playerSender))
        {
            msg("Going to the AdminWorld.");
            plugin.wm.adminworld.sendToWorld(playerSender);
        }
        else
        {
            msg("You don't have permission to access the AdminWorld.");
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "guest add <player:Player>")
    public boolean addGuest(CommandContext ctx, Player player)
    {
        CommandSender sender = ctx.getSender();
        Player playerSender = ctx.getPlayerSender();

        try
        {
            assertCommandPerms(sender, playerSender);

            if (plugin.wm.adminworld.addGuest(player, playerSender))
            {
                FUtil.adminAction(sender.getName(), "AdminWorld guest added: " + player.getName(), false);
            }
            else
            {
                msg("Could not add player to guest list.");
            }
        }
        catch (PermissionDeniedException ex)
        {
            if (ex.getMessage().isEmpty())
            {
                return noPerms();
            }
            sender.sendMessage(ex.getMessage());
            return true;
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "guest remove <player:Player>")
    public boolean removeGuest(CommandContext ctx, Player player)
    {
        CommandSender sender = ctx.getSender();

        if (plugin.wm.adminworld.removeGuest(player))
        {
            FUtil.adminAction(sender.getName(), "AdminWorld guest removed: " + player.getName(), false);
        }
        else
        {
            msg("Can't find guest entry for: " + player.getName());
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "guest list")
    public boolean listGuests(CommandContext ctx)
    {
        if (plugin.wm.adminworld.hasGuests())
        {
            msg("AdminWorld guest list: " + plugin.wm.adminworld.guestListToString());
        }
        else
        {
            msg("There are no AdminWorld guests.");
        }
        return true;
    }

    @CommandDispatchTarget(pattern = "guest purge")
    public boolean purgeGuests(CommandContext ctx)
    {
        CommandSender sender = ctx.getSender();
        Player playerSender = ctx.getPlayerSender();

        try
        {
            assertCommandPerms(sender, playerSender);
            plugin.wm.adminworld.purgeGuestList();
            FUtil.adminAction(sender.getName(), "AdminWorld guest list purged.", false);
        }
        catch (PermissionDeniedException ex)
        {
            if (ex.getMessage().isEmpty())
            {
                return noPerms();
            }
            sender.sendMessage(ex.getMessage());
            return true;
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "time <time>")
    public boolean setTime(CommandContext ctx, String timeString)
    {
        CommandSender sender = ctx.getSender();
        Player playerSender = ctx.getPlayerSender();

        try
        {
            assertCommandPerms(sender, playerSender);
            WorldTime timeOfDay = WorldTime.getByAlias(timeString);
            if (timeOfDay != null)
            {
                plugin.wm.adminworld.setTimeOfDay(timeOfDay);
                msg("AdminWorld time set to: " + timeOfDay.name());
            }
            else
            {
                msg("Invalid time of day. Can be: sunrise, noon, sunset, midnight");
            }
        }
        catch (PermissionDeniedException ex)
        {
            if (ex.getMessage().isEmpty())
            {
                return noPerms();
            }
            sender.sendMessage(ex.getMessage());
            return true;
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "weather <weather>")
    public boolean setWeather(CommandContext ctx, String weatherValue)
    {
        CommandSender sender = ctx.getSender();
        Player playerSender = ctx.getPlayerSender();

        try
        {
            assertCommandPerms(sender, playerSender);
            WorldWeather weatherMode = WorldWeather.getByAlias(weatherValue);
            if (weatherMode != null)
            {
                plugin.wm.adminworld.setWeatherMode(weatherMode);
                msg("AdminWorld weather set to: " + weatherMode.name());

            }
            else
            {
                msg("Invalid weather mode. Can be: off, rain, storm");
            }
        }
        catch (PermissionDeniedException ex)
        {
            if (ex.getMessage().isEmpty())
            {
                return noPerms();
            }
            sender.sendMessage(ex.getMessage());
            return true;
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }

    // TODO: Redo this properly -- khyvie: maybe introduce a per-dispatch target permission node system
    private void assertCommandPerms(CommandSender sender, Player playerSender) throws PermissionDeniedException
    {
        if (!(sender instanceof Player) || playerSender == null || !isAdmin(sender))
        {
            throw new PermissionDeniedException();
        }
    }

    private enum CommandMode
    {

        TELEPORT, GUEST, TIME, WEATHER
    }

    private class PermissionDeniedException extends Exception
    {

        private static final long serialVersionUID = 1L;

        private PermissionDeniedException()
        {
            super("");
        }

        private PermissionDeniedException(String string)
        {
            super(string);
        }
    }

}
