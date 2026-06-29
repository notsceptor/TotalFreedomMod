package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Objects;

@CommandPermissions(level = Rank.OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.player.report")
@CommandParameters(description = "Report a player for admins to see.", usage = "/<command> <player> <reason>")
public class Command_report extends FreedomCommand
{
    private void doReport(CommandSender sender, OfflinePlayer target, String reason)
    {
        // This cast will never fail since the command may only be executed by players
        Player playerSender = (Player) sender;

        if (playerSender.equals(target))
        {
            msg(sender, "Please, don't try to report yourself.", NamedTextColor.RED);
            return;
        }

        // HTTP request may complete after the player logs out
        boolean sendFeedback = playerSender.isOnline();
        // We'd still like to send the report though

        // getOfflinePlayer can return players that haven't played before
        // if (!target.hasPlayedBefore()) -- Doesn't seem like changes are reflected immediately
        if (target.getFirstPlayed() == 0)
        {
            if (sendFeedback)
            {
                msg(sender, PLAYER_NOT_FOUND);
            }

            return;
        }

        Admin reportedAdmin = plugin.al.getAdminByUuid(target.getUniqueId());
        if (reportedAdmin != null && reportedAdmin.isActive())
        {
            if (sendFeedback)
            {
                msg(sender, Component.text("You can not report an admin.", NamedTextColor.RED));
            }

            return;
        }

        plugin.cm.reportAction(playerSender, target, reason);

        if (sendFeedback)
        {
            msg(sender, Component.text("Thank you, your report has been successfully logged.", NamedTextColor.GREEN));
        }
    }

    @CommandDispatchTarget(pattern = "<playerName> <reason..>")
    public boolean report(CommandContext ctx, String playerName, String reason) {
        Player player = getPlayer(playerName);

        if (player != null)
        {
            doReport(sender, player, reason);
        }
        else
        {
            BukkitScheduler scheduler = server.getScheduler();

            // getOfflinePlayer(String) can make an HTTP request if we're in online mode,
            // so let's avoid doing that on the main thread
            scheduler.runTaskAsynchronously(
                    plugin,
                    () -> {
                        OfflinePlayer target = server.getOfflinePlayer(playerName);

                        // Avoid interacting with Bukkit API & TFM outside the main thread where possible
                        scheduler.runTask(
                                plugin,
                                () -> doReport(
                                        sender,
                                        target,
                                        reason
                                )
                        );
                    }
            );
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
