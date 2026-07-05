package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

@CommandPermissions(level = Rank.SENIOR_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.senior.deafen")
@CommandParameters(description = "Make some noise.", usage = "/<command>")
public class Command_deafen extends FreedomCommand
{

    public static final double STEPS = 10.0;
    private static final Random random = new Random();
    private static final List<Sound> SOUNDS = Registry.SOUNDS.stream().toList();

    private static Location randomOffset(Location a, double magnitude)
    {
        return a.clone().add(randomDoubleRange(-1.0, 1.0) * magnitude, randomDoubleRange(-1.0, 1.0) * magnitude, randomDoubleRange(-1.0, 1.0) * magnitude);
    }

    private static Double randomDoubleRange(double min, double max)
    {
        return min + (random.nextDouble() * ((max - min) + 1.0));
    }

    private static Sound getRandomSound()
    {
        return SOUNDS.get(random.nextInt(SOUNDS.size()));
    }

    @CommandDispatchTarget
    public boolean deafenNoArgument(CommandContext ctx)
    {
        for (final Player player : server.getOnlinePlayers())
        {
            for (double percent = 0.0; percent <= 1.0; percent += (1.0 / STEPS))
            {
                final float pitch = (float) (percent * 2.0);

                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> player.playSound(randomOffset(player.getLocation(), 5.0), getRandomSound(), 100.0f, pitch),
                        Math.round(20.0 * percent * 2.0));
            }
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {

        return false;
    }
}
