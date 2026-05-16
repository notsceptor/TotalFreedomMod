package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.fun.hack")
@CommandParameters(description = "Easter egg command - pretends to hack the server", usage = "/<command>")
public class Command_hack extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (senderIsConsole)
        {
            msg("This command can only be used by players.", NamedTextColor.RED);
            return true;
        }

        Player player = (Player) sender;

        // Fake hacking sequence
        // Kick the player with a funny message - kickPlayer accepts String with legacy codes
        msg(player, "Initializing hack sequence...", NamedTextColor.RED);
        msg(player, "Bypassing security protocols...", NamedTextColor.YELLOW);
        msg(player, "Accessing server files...", NamedTextColor.GOLD);
        msg(player, "Uploading virus...", NamedTextColor.GREEN);
        msg(player, "ERROR: Hack failed!", NamedTextColor.DARK_RED);
        msg(player, "You have been detected and will be kicked.", NamedTextColor.RED);

        player.kick(Component.text("Nice try, hacker!\n", NamedTextColor.RED)
                .append(Component.text("The server is protected by TotalFreedomMod.", NamedTextColor.YELLOW)));

        return true;
    }
}

