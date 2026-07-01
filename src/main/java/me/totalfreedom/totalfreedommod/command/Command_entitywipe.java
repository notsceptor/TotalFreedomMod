package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.server.entitywipe")
@CommandParameters(description = "Remove various server entities that may cause lag, such as dropped items, minecarts, and boats.", usage = "/<command> [world]", aliases = "ew,rd")
public class Command_entitywipe extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length >= 1)
        {
            World world = Bukkit.getWorld(args[0]);
            if (world == null)
            {
                msg("World \"" + args[0] + "\" not found.", NamedTextColor.RED);
                return true;
            }
            FUtil.adminAction(sender.getName(), "Removing entities in world " + world.getName(), true);
            int removed = plugin.ew.wipeEntities(world, true);
            msg(removed + " " + (removed == 1 ? "entity" : "entities") + " removed from \"" + world.getName() + "\".");
            return true;
        }

        FUtil.adminAction(sender.getName(), "Removing all server entities", true);
        int removed = plugin.ew.wipeEntities(true);
        msg(removed + " " + (removed == 1 ? "entity" : "entities") + " removed.");
        return true;
    }
}