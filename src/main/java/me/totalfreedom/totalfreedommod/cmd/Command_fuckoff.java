package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;

import org.bukkit.entity.Player;

@Permission(level = Rank.SENIOR_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.admin.senior.fuckoff")
@Command(name = "fuckoff", description = "You'll never even see it coming.", usage = "/fuckoff <on [radius (default=25)] | off>")
public class Command_fuckoff extends FCommand
{

    @Callback
    public boolean fuckoff(Player sender, final @Resolve("Double") double radius)
    {

        FPlayer player = fplayer(sender);
        double clamp = Math.clamp(radius, 0.0, 50.0);

        if (radius <= 0)
        {
            plugin().fo.disable(sender);
        }
        else
        {
            plugin().fo.enable(sender, clamp);
        }

        msg(
            sender, 
            "Fuckoff <choice:enabled. Radius: \"<radius>\":disabled>.", 
            Formatter.booleanChoice("choice", player.isFuckOff()),
            Formatter.number("radius", clamp)
        );

        return true;
    }
}
