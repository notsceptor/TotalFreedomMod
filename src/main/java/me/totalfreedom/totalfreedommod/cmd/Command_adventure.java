package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;

@Command(name = "adventure", description = "Quickly change your own gamemode to adventure, or define someone's username to change theirs.", usage = "/adventure [-a | name]", aliases = {"gma"})
@Permission(permission = "tfm.admin.gamemode")
public class Command_adventure extends FCommand 
{
       @Callback
       @Permission(permission = "tfm.admin.gamemode", source = SourceType.ONLY_IN_GAME)
       public void changeGamemodeSelf(Player player)
       {
            player.setGameMode(GameMode.ADVENTURE);
            msg(player, "Your gamemode has been set to Adventure.");
       }

       @Callback
       @Subcommand("-a") // a switch wouldn't be really appropriate here due to the nature of the @Permission annotation
       @Permission(permission = "tfm.admin.gamemode", level = Rank.SUPER_ADMIN)
       public void changeGamemodeAll(CommandSender sender)
       {
            Bukkit.getOnlinePlayers().forEach(player -> player.setGameMode(GameMode.ADVENTURE));
            FUtil.adminAction(sender.getName(), "Changing everyone's gamemode to Adventure.", true);
       }

       @Callback
       @Permission(permission = "tfm.admin.gamemode", level = Rank.SUPER_ADMIN)
       public void changeGamemodeOther(CommandSender sender, Player target)
       {
            target.setGameMode(GameMode.ADVENTURE);
            msg(sender, "Setting " + target.getName() + "'s gamemode to Adventure.");
            msg(target, sender.getName() + " set your gamemode to Adventure.");
       }
}
