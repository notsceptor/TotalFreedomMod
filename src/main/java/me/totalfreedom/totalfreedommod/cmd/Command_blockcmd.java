package me.totalfreedom.totalfreedommod.cmd;

import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.Rank;

@Command(name = "blockcmd", description = "Block all commands for a specific player.", usage = "/<command> <-a | purge | <player>>", aliases = {"blockcommands","blockcommand","bc","bcmd"})
@Permission(level = Rank.SUPER_ADMIN, permission = "tfm.admin.blockcmd")
public class Command_blockcmd extends FCommand
{
    @Callback
    public void blockPlayer(final CommandSender sender, final Player player)
    {
        if (isAdmin(player))
        {
            msg(sender, "%s is a Superadmin, and cannot have their commands blocked.", player.getName());
            return;
        }

        FPlayer playerdata = plugin.pl.getPlayer(player);

        playerdata.setCommandsBlocked(!playerdata.allCommandsBlocked());

        adminAction(sender,  "<red>%slocking all commands for %s", (playerdata.allCommandsBlocked() ? "B" : "Unb"), player.getName());
        msg(sender, "%slocked all commands for %s", (playerdata.allCommandsBlocked() ? "B" : "Unb"), player.getName());
    }

    @Callback
    @Subcommand("-a") // This one is also not fit for a switch, but this case is due to the way handlers are implemented
    public void blockAll(final CommandSender sender)
    {
            adminAction(sender, "<red>Blocking commands for all non-admins");
            AtomicInteger counter = new AtomicInteger(0);

            server.getOnlinePlayers()
                  .stream()
                  .filter(p -> !isAdmin(p))
                  .forEach(p -> {
                    plugin.pl.getPlayer(p).setCommandsBlocked(true);
                    msg(p, "<red>Your commands have been blocked by an admin.");
                    counter.incrementAndGet();
                  });

            msg(sender, "Blocked commands for %i players.", counter.get());
    }

    @Callback
    @Subcommand("purge")
    public void purge(final CommandSender sender)
    {
        adminAction(sender, "<red>Unblocking commands for all players");
        AtomicInteger counter = new AtomicInteger(0);

        server.getOnlinePlayers()
              .stream()
              .map(plugin.pl::getPlayer)
              .filter(FPlayer::allCommandsBlocked)
              .forEach(data -> {
                    data.setCommandsBlocked(false);
                    counter.incrementAndGet();
                });

        msg(sender, "Unblocked commands for %i players.", counter.get());
    }
}