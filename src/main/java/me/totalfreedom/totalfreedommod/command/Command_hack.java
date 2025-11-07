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
        player.sendMessage(ChatColor.RED + "Initializing hack sequence...");
        player.sendMessage(ChatColor.YELLOW + "Bypassing security protocols...");
        player.sendMessage(ChatColor.GOLD + "Accessing server files...");
        player.sendMessage(ChatColor.GREEN + "Uploading virus...");
        player.sendMessage(ChatColor.DARK_RED + "ERROR: Hack failed!");
        player.sendMessage(ChatColor.RED + "You have been detected and will be kicked.");

        // Kick the player with a funny message
        player.kickPlayer(ChatColor.RED + "Nice try, hacker!\n" + ChatColor.YELLOW + "The server is protected by TotalFreedomMod.");

        return true;
    }
}

