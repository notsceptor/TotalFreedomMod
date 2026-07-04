package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SENIOR_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.senior.rawsay")
@CommandParameters(description = "Broadcasts the given message. Supports colors.", usage = "/<command> <message>")
public class Command_rawsay extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<message..>")
    public boolean rawsay(CommandContext ctx, String message)
    {
        FUtil.bcastMsg(FUtil.colorize(message));
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
