package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.ONLY_IN_GAME)
@CommandParameters(description = "Easter egg command - pretends to hack the server", usage = "/<command>")
public class Command_hack extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (senderIsConsole)
        {
            msg("This command can only be used by players.", ChatColor.RED);
            return true;
        }

        Player player = (Player) sender;

        // Fake hacking sequence
        msg(player, "Initializing hack sequence...", ChatColor.RED);
        msg(player, "Bypassing security protocols...", ChatColor.YELLOW);
        msg(player, "Accessing server files...", ChatColor.GOLD);
        msg(player, "Uploading virus...", ChatColor.GREEN);
        msg(player, "ERROR: Hack failed!", ChatColor.DARK_RED);
        msg(player, "You have been detected and will be kicked.", ChatColor.RED);

        // Kick the player with a funny message - kickPlayer accepts String with legacy codes
        String kickMsg = ChatColor.RED + "Nice try, hacker!\n" + ChatColor.YELLOW + "The server is protected by TotalFreedomMod.";
        player.kickPlayer(kickMsg);

        return true;
    }
}

