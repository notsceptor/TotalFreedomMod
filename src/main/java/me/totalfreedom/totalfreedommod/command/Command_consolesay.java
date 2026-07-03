package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchContext;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_CONSOLE, permission = "tfm.admin.consolesay")
@CommandParameters(description = "Telnet command - Send a chat message with chat formatting over telnet.", usage = "/<command> <message>", aliases = "csay")
public class Command_consolesay extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<message..>")
    public boolean sendMessage(CommandContext ctx, String message)
    {
        // TODO: Make this consistent with the rest of the chat formatting system
        if (RemoteDispatchContext.isActive())
        {
            FUtil.bcastMsg(String.format("§f<§c%s§f> %s", sender.getName(), message));
        }
        else
        {
            FUtil.bcastMsg(String.format("§7[CONSOLE]§f<§c%s§f> %s", sender.getName(), message));
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
