package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.denick")
@CommandParameters(description = "Remove the nickname of all players on the server.", usage = "/<command>")
public class Command_denick extends FreedomCommand
{
    @CommandDispatchTarget
    public boolean removeNicknames(CommandContext ctx)
    {
        FUtil.adminAction(ctx.getSender().getName(), "Removing all nicknames", false);

        for (Player player : server.getOnlinePlayers())
            plugin.pl.getData(player).setNickname(null);

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
