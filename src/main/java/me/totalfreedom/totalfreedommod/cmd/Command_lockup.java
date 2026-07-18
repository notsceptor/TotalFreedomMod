package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Subcommand;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

@Command(name = "lockup", description = "Block target's minecraft input. This is evil, and I never should have wrote it.", usage = "/<command> <all | purge | <<partialname> on | off>>")
@Permission(level = Rank.SENIOR_ADMIN, source = SourceType.ONLY_CONSOLE, permission = "tfm.admin.senior.lockup")
public class Command_lockup extends FCommand
{
    @Callback
    @Subcommand("all")
    public void lockAll(CommandSender sender)
    {
        adminAction(sender, "<aqua>Locking up all players");

        server().getOnlinePlayers().forEach(this::startLockup);

        msg(sender, "Locked up all players.");
    }

    @Callback
    @Subcommand("purge")
    public void unlockAll(CommandSender sender)
    {
        adminAction(sender, "<aqua>Unlocking all players");

        server().getOnlinePlayers().forEach(this::cancelLockup);

        msg(sender, "Unlocked all players.");
    }

    @Callback
    public void toggle(CommandSender sender, String name, String state)
    {
        final Player player = server().getPlayer(name);

        if (player == null)
        {
            msg(sender, "<gray>Player not found!");
            return;
        }

        if ("on".equalsIgnoreCase(state))
        {
            adminAction(sender, "<aqua>Locking up <player>", Placeholder.unparsed("player", player.getName()));
            startLockup(player);
            msg(sender, "Locked up <player>.", Placeholder.unparsed("player", player.getName()));
        }
        else if ("off".equalsIgnoreCase(state))
        {
            adminAction(sender, "<aqua>Unlocking <player>", Placeholder.unparsed("player", player.getName()));
            cancelLockup(player);
            msg(sender, "Unlocked <player>.", Placeholder.unparsed("player", player.getName()));
        }
    }

    private void cancelLockup(FPlayer playerdata)
    {
        BukkitTask lockupScheduleId = playerdata.getLockupScheduleID();
        if (lockupScheduleId != null)
        {
            lockupScheduleId.cancel();
            playerdata.setLockupScheduleId(null);
        }
    }

    private void cancelLockup(final Player player)
    {
        cancelLockup(fplayer(player));
    }

    private void startLockup(final Player player)
    {
        final FPlayer playerdata = fplayer(player);

        cancelLockup(playerdata);

        playerdata.setLockupScheduleId(new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (player.isOnline())
                {
                    player.openInventory(player.getInventory());
                }
                else
                {
                    cancelLockup(playerdata);
                }
            }
        }.runTaskTimer(plugin(), 0L, 5L));
    }
}
