package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.fun.Trailer;

import org.bukkit.entity.Player;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.SourceType;
import me.totalfreedom.api.cmd.annotation.*;

@Command(name = "trail", description = "Pretty rainbow trails.", usage = "/trail [on | off]")
@Permission(permission = "tfm.fun.trail", source = SourceType.ONLY_IN_GAME)
public class Command_trail extends FCommand
{
    @Callback
    public void toggle(Player player)
    {
        setTrail(player, plugin().services().require(Trailer.class).has(player));
    }

    @Callback
    public void setTrail(Player player, @Resolve("Boolean") boolean value) 
    // This forces the custom boolean argument resolver 
    // instead of native brigadier boolean argument resolver which only resolves true / false
    {
        if (value)
        {
            plugin().services().require(Trailer.class).remove(player);
            msg(player, "<gray>Trail disabled.");
        }
        else
        {
            plugin().services().require(Trailer.class).add(player);
            msg(player, "<gray>Trail enabled.");
        }
    }
}
