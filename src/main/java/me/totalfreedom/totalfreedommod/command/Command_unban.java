package me.totalfreedom.totalfreedommod.command;

import java.util.Set;
import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ban")
@CommandParameters(description = "Unbans an online or offline player and linked IP addresses.", usage = "/<command> [-s] [-r] <player>")
public class Command_unban extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<playerName>", switches = "s,r")
    public boolean unban(CommandContext ctx, String playerName, boolean silent, boolean restore)
    {
        Player player = getPlayer(playerName);
        PlayerData data = BanCommandUtil.getData(plugin, playerName, player);
        String name = BanCommandUtil.getCanonicalName(playerName, player, data);
        Set<Ban> bans = BanCommandUtil.findLinkedBans(plugin, playerName, player, data);
        if (bans.isEmpty())
        {
            msg("No ban on record for " + playerName + ".");
            return true;
        }
        for (Ban ban : bans)
        {
            plugin.bm.removeBan(ban);
        }
        if (!silent)
            FUtil.adminAction(sender.getName(), "Unbanning " + name, true);
        if (restore)
            plugin.cpb.restore(name);
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
