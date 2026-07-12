package me.totalfreedom.totalfreedommod.cmd;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;

@Command(name = "ban", description = "Bans an online or previously known player and their known IP addresses.", usage = "/<command> [-s] [-nrb] <player> [reason]", aliases = {"gtfo"})
@Permission(permission = "tfm.admin.ban", level = Rank.SUPER_ADMIN)
public class Command_ban extends FCommand
{

    @Callback
    public void ban(CommandSender sender, Player player, @Greedy String reason, @Switch("s") boolean silent, @Switch("nrb") boolean noRollback)
    {
        doBan(sender, player, reason, silent, noRollback);
    }

    @Callback
    public void banNoReason(CommandSender sender, Player player, @Switch("s") boolean silent, @Switch("nrb") boolean noRollback)
    {
        doBan(sender, player, null, silent, noRollback);
    }

    private void doBan(CommandSender sender, Player player, String reason, boolean silent, boolean noRollback)
    {
        PlayerData data = BanCommandUtil.getData(plugin, player.getName(), player);

        if (data == null)
        {
            msg(sender, "Can't find that player. Use /banname to ban an arbitrary name.");
            return;
        }

        String name = BanCommandUtil.getCanonicalName(player.getName(), player, data);

        if (plugin.bm.getByUsername(name) != null)
        {
            msg(sender, "%s is already banned.", name);
            return;
        }

        List<String> ips = BanCommandUtil.getIps(player, data);
        Ban ban = BanCommandUtil.createFullBan(name, ips, sender, FUtil.parseDateOffset("24h"), reason);

        if (!silent && player != null)
        {
            FUtil.bcastMsg(player.getName() + " has been a VERY naughty, naughty boy.", NamedTextColor.RED);
        }

        plugin.bm.addBan(ban);
        if (!silent)
        {
            adminAction(sender, "<red>Banning %s", name);

            if (reason != null)
            {
                FUtil.bcastMsg("  Reason: " + reason, NamedTextColor.YELLOW); // TODO: replace with MessageUtils stuff
            }

            plugin.db.sendActionMessage(sender.getName(), name, reason, ConfigEntry.DISCORD_PLAYER_BAN_MESSAGE);
        }

        if (!noRollback)
        {
            plugin.cpb.rollback(name);
        }

        if (!plugin.al.isAdmin(player))
        {
            data.setStrikes(0);
        }

        if (player != null)
        {
            try
            {
                plugin.web.undo(player, 15);
            }
            catch (NoClassDefFoundError ignored)
            {
            }

            if (!silent)
            {
                player.setOp(false);
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
                smitePlayer(player);
            }

            kickPlayer(player, MessageUtils.toPlainText(ban.bakeKickMessage()));
        }
    }
}
