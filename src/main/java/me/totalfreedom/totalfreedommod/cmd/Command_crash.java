package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Command(name = "crash", description = "Crashes the specified player", usage = "/crash <player>", aliases = {"fuckup"})
@Permission(level = Rank.SUPER_ADMIN, permission = "tfm.admin.fuckup")
public class Command_crash extends FCommand
{
    @Callback
    public void crash(final CommandSender sender, final Player player)
    {
        if (plugin().al.isAdmin(player))
        {
            msg(sender, "This command cannot be used on other admins.");
            return;
        }
        
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

        sender.sendMessage(
                Component.text("Crashed ", NamedTextColor.GRAY)
                        .append(Component.text(player.getName(), NamedTextColor.GRAY))
                        .append(Component.text(".", NamedTextColor.GRAY))
        );

        // not fucking with the translatable shit. video can do that.
    }
}
