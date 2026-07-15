package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;

@Command(name = "flatlands", description = "Goto the flatlands.", usage = "/flatlands")
@Permission(permission = "tfm.world.flatlands", level = Rank.NON_OP, source = SourceType.ONLY_IN_GAME)
public class Command_flatlands extends FCommand
{
    @Callback
    public void flatlands(Player player)
    {
        if (!ConfigEntry.FLATLANDS_GENERATE.getBoolean())
        {
            msg(player, "Flatlands is currently disabled.");
            return;
        }

        plugin().wm.flatlands.sendToWorld(player);
    }
}
