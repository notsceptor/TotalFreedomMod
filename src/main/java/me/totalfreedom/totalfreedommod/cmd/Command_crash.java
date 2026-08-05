package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;

@Command(name = "crash", description = "Crashes the specified player", usage = "/crash <player>", aliases = {"fuckup"})
@Permission(permission = "tfm.admin.fuckup")
public class Command_crash extends FCommand
{
    @Callback
    public void crash(final CommandSender sender, final Player player)
    {
        if (isProtectedAdmin(sender, player))
            return;
        
        player.spawnParticle(
                Particle.ASH,
                player.getLocation(),
                1_000_000_000,
                0.0,
                0.0,
                0.0,
                1.0,
                null,
                true
        );

        Component c = Component.text("GET ABSOLUTELY FUCKED!");
        for (int i = 0; i < 50; ++i)
            c = Component.translatable("%1$s%1$s%1$s", "%1$s%1$s%1$s", c);

        player.sendMessage(c);

        msg(sender, "<gray>Crashed <player>.",
                Placeholder.unparsed("player", player.getName())
        );
    }
}