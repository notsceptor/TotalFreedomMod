package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@Permission(source = SourceType.ONLY_IN_GAME, permission = "tfm.player.report")
@Command(name = "report", description = "Report a player for admins to see.", usage = "/report <player> <reason>")
public class Command_report extends FCommand
{

    private void doReport(Player player, OfflinePlayer target, String reason)
    {
        // This cast will never fail since the command may only be executed by players
        if (player.equals(target))
        {
            msg(player, "Please, don't try to report yourself.", NamedTextColor.RED);
            return;
        }

        // HTTP request may complete after the player logs out
        boolean sendFeedback = player.isOnline();
        // We'd still like to send the report though

        // getOfflinePlayer can return players that haven't played before
        // if (!target.hasPlayedBefore()) -- Doesn't seem like changes are reflected immediately
        if (target.getFirstPlayed() == 0)
        {
            if (sendFeedback)
            {
                msg(player, "<red>That player cannot be found!");
            }

            return;
        }

        Admin reportedAdmin = plugin().al.getAdminByUuid(target.getUniqueId());
        if (reportedAdmin != null && reportedAdmin.isActive())
        {
            if (sendFeedback)
            {
                msg(player, "<red>Please use the <yellow><click:open_url:https://forum.tfreedom.org/>forums</click> <red>for reporting admins."); // based MessageUtils
            }

            return;
        }

        plugin().cm.reportAction(player, target, reason);

        if (sendFeedback)
        {
            msg(player, "<green>Thank you, your report has been successfully logged.");
        }
    }

    @Callback
    public void report(Player sender, String playerName, @Greedy String reason) {
        Player player = getPlayer(playerName);

        if (player != null)
        {
            doReport(sender, player, reason);
        }

        else
        {
            async(t -> 
            {
                OfflinePlayer target = server().getOfflinePlayer(playerName);

                // Avoid interacting with Bukkit API & TFM outside the main thread where possible
                sync(() -> doReport(
                                    sender,
                                    target,
                                    reason
                            ), 0L);
            }, 0L);
        }
    }
}
