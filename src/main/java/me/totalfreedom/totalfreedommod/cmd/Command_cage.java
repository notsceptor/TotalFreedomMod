package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.caging.CageData;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@Command(name = "cage", description = "Place a cage around someone.", usage = "/cage (-s) <player> [<outer_mat> <inner_mat>] | purge")
@Permission(permission = "tfm.admin.cage", level = Rank.SUPER_ADMIN)
public class Command_cage extends FCommand
{
    @Callback // /cage [-s] <player> | Will auto-toggle, but switch -s can be used to guarantee uncaging.
    public void toggle(final CommandSender sender, final Player player, @Switch("s") boolean stop)
    {
        CageData cageData = plugin.pl.getPlayer(player).getCageData();

        if (stop || cageData.isCaged())
        {
            cageData.setCaged(false);
        }

        cage(sender, player, Material.GLASS, Material.AIR);
    }

    @Callback // /cage purge
    @Subcommand("purge")
    public void purge(final CommandSender sender)
    {
        server.getOnlinePlayers()
              .stream()
              .map(plugin.pl::getPlayer)
              .map(FPlayer::getCageData)
              .forEach(cd -> cd.setCaged(false));

        adminAction(sender, "<red>Uncaging all players.");
    }

    @Callback // /cage <player> <outer_mat> <inner_mat>   =   No support for switch here because why would you supply a -s and then define materials? would default to switch above anyways.
    public void cage(final CommandSender sender, final Player player, final Material outer, final Material inner)
    {
        CageData data = plugin.pl.getPlayer(player).getCageData();
        Location loc = player.getLocation().clone().add(0, 1, 0);

        adminAction(sender, "<red>Caging <player>...", Placeholder.unparsed("player", player.getName()));

        data.cage(loc, outer, inner);
    }
}
