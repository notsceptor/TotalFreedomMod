package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.deop")
@CommandParameters(description = "Deop a player.", usage = "/<command> <player>")
public class  Command_deop extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<player:OfflinePlayer>")
    public boolean deopPlayer(CommandContext ctx, OfflinePlayer player)
    {
        FUtil.adminAction(sender.getName(), "De-opping " + player.getName(), false);
        player.setOp(false);

        if (player.isOnline())
        {
            msg(player.getPlayer(), FreedomCommand.YOU_ARE_NOT_OP);
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
