package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@Command(name = "entitywipe", description = "Remove various server entities that may cause lag, such as dropped items, minecarts, and boats.", usage = "/entitywipe [world]", aliases = {"ew", "rd"})
@Permission(permission = "tfm.server.entitywipe", level = Rank.SUPER_ADMIN)
public class Command_entitywipe extends FCommand
{
    @Callback
    public void entitywipeAll(CommandSender sender)
    {
        adminAction(sender, "<red>Removing all server entities");
        int removed = plugin().ew.wipeEntities(true);
        msg(
            sender,
            "<count> <noun> removed.",
            Formatter.number("count", removed),
            Placeholder.unparsed("noun", removed == 1 ? "entity" : "entities")
        );
    }

    @Callback
    public void entitywipeWorld(CommandSender sender, String world)
    {
        World target = Bukkit.getWorld(world);
        if (target == null)
        {
            msg(sender, "<red>World \"<world>\" not found.", Placeholder.unparsed("world", world));
            return;
        }

        adminAction(sender, "<red>Removing entities in world <world>", Placeholder.unparsed("world", target.getName()));
        int removed = plugin().ew.wipeEntities(target, true);
        msg(
            sender,
            "<count> <noun> removed from \"<world>\".",
            Formatter.number("count", removed),
            Placeholder.unparsed("noun", removed == 1 ? "entity" : "entities"),
            Placeholder.unparsed("world", target.getName())
        );
    }
}
