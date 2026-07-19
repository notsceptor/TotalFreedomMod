package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

@Permission(level = Rank.SENIOR_ADMIN, permission = "tfm.admin.senior.deafen")
@Command(name = "deafen", description = "Make some noise.", usage = "/<command>")
public class Command_deafen extends FCommand
{

    public static final double STEPS = 10.0;
    private static final Random random = new Random();
    private static final List<Sound> SOUNDS = Registry.SOUNDS.stream().toList();

    private static Location randomOffset(Location a, double magnitude)
    {
        return a.clone().add(random.nextDouble(-magnitude, magnitude),
                random.nextDouble(-magnitude, magnitude),
                random.nextDouble(-magnitude, magnitude));
    }

    private static Sound getRandomSound()
    {
        return SOUNDS.get(random.nextInt(SOUNDS.size()));
    }

    private void playNoiseSequence(Player player)
    {
        for (double percent = 0.0; percent <= 1.0; percent += (1.0 / STEPS))
        {
            final float pitch = (float) (percent * 2.0);
            sync(() -> player.playSound(randomOffset(player.getLocation(), 5.0), getRandomSound(), 100.0f, pitch),
                    Math.round(20.0 * percent * 2.0));
        }
    }

    @Callback
    public void deafenNoArgument(CommandSender sender)
    {
        server().getOnlinePlayers().forEach(this::playNoiseSequence);
    }

    // cuz why not make it able to target one player?
    @Callback
    public void deafenPlayer(CommandSender sender, Player player)
    {
        for (int x = 0; x <= server().getOnlinePlayers().size(); x++) // using player size since that's how the other method functions
        {
            playNoiseSequence(player);
        }
    }
}
