package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;

@Command(name = "banname", description = "Bans the specified name.", usage = "/banname <name> [reason]")
@Permission(level = Rank.SUPER_ADMIN, permission = "tfm.admin.ban")
public class Command_banname extends FCommand
{
    @Callback
    public void banName(CommandSender sender, String name)
    {
        banNameWithReason(sender, name, null);
    }

    @Callback
    public void banNameWithReason(CommandSender sender, String name, @Greedy String reason)
    {
        if (plugin.bm.getByUsername(name) != null)
        {
            msg(sender, "%s is already banned.", name);
            return;
        }
        final Ban ban = Ban.forPlayerName(name, sender, null, reason);

        plugin.bm.addBan(ban);

        final Player player = server.getPlayer(name);
        if (player != null)
        {
            kickPlayer(player, MessageUtils.toPlainText(ban.bakeKickMessage()));
        }
    }
}
