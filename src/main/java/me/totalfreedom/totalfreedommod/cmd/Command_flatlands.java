package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.entity.Player;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.SourceType;
import me.totalfreedom.api.cmd.annotation.*;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.world.GeneratedWorld;

@Command(name = "flatlands", description = "Goto the flatlands.", usage = "/flatlands")
@Permission(permission = "tfm.world.flatlands", source = SourceType.ONLY_IN_GAME)
public class Command_flatlands extends FCommand
{
    @Callback
    public void flatlands(Player player)
    {
        if (!ConfigEntry.FLATLANDS_GENERATE.getBoolean())
        {
            msg(player, "<gray>Flatlands is currently disabled.");
            return;
        }

        final GeneratedWorld flatlands = plugin().worlds().flatlands();

        if (flatlands == null)
        {
            msg(player, "<red>Flatlands is not available right now.");
            return;
        }

        flatlands.sendToWorld(player);
    }
}
