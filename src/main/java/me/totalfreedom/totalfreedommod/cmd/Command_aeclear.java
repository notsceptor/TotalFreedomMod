package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.rank.Rank;

import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;

@Command(name = "aeclear", description = "Removes all area-of-effect clouds on the server.", usage = "/aeclear", aliases = {"aec"})
@Permission(level = Rank.SUPER_ADMIN, permission = "tfm.admin.aeclear")
public class Command_aeclear extends FCommand
{
    @Callback
    public void clear(CommandSender sender)
    {
        adminAction(sender, "<red>Removing all area-of-effect clouds");

        int removed = 0;
        for (World world : server.getWorlds())
        {
            for (Entity entity : world.getEntities())
            {
                if (entity instanceof AreaEffectCloud)
                {
                    entity.remove();
                    removed++;
                }
            }
        }

        msg(sender, "<gray><count> area-of-effect clouds removed.", Formatter.number("count", removed));
    }
}
