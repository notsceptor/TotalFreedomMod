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
@CommandParameters(description = "Unbans an online or offline player and linked IP addresses.", usage = "/<command> <player> [-r]")
public class Command_unban extends FreedomCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length < 1)
        {
            return false;
        }
        Player player = getPlayer(args[0]);
        PlayerData data = BanCommandUtil.getData(plugin, args[0], player);
        String name = BanCommandUtil.getCanonicalName(args[0], player, data);
        Set<Ban> bans = BanCommandUtil.findLinkedBans(plugin, args[0], player, data);
        if (bans.isEmpty())
        {
            msg("No ban on record for " + args[0] + ".");
            return true;
        }
        for (Ban ban : bans)
        {
            plugin.bm.removeBan(ban);
        }
        FUtil.adminAction(sender.getName(), "Unbanning " + name, true);
        if (args.length > 1 && args[1].equalsIgnoreCase("-r"))
        {
            plugin.cpb.restore(name);
        }
        return true;
    }
}
