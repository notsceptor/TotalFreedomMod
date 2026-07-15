package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@Command(name = "spawn", description = "Teleport to the server spawn.", usage = "/spawn [player]")
@Permission(permission = "tfm.player.spawn", level = Rank.NON_OP, source = SourceType.ONLY_IN_GAME)
public class Command_spawn extends FCommand
{
    @Callback
    public void spawnSelf(Player player)
    {
        if (plugin.sm.sendToSpawn(player))
        {
            msg(player, "Teleported to spawn.");
        }
    }

    @Callback
    public void spawnOther(Player sender, Player target)
    {
        if (!isAdmin(sender))
        {
            msg(sender, "No permission to send other players to spawn.");
            return;
        }

        if (plugin.sm.sendToSpawn(target))
        {
            msg(sender, "Sent <player> to spawn.", Placeholder.unparsed("player", target.getName()));
        }
    }
}
