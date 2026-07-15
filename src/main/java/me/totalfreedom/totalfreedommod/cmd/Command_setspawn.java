package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@Command(name = "setspawn", description = "Set the server spawn to your current location.", usage = "/setspawn")
@Permission(permission = "tfm.world.setspawn", level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME)
public class Command_setspawn extends FCommand
{
    @Callback
    public void setspawn(Player player)
    {
        Location location = player.getLocation();
        plugin().sm.setSpawnLocation(location);
        msg(player, "Server spawn set to: <location>", Placeholder.unparsed("location", FUtil.formatLocation(location)));
    }
}
