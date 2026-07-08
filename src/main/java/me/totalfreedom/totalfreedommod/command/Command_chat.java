package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.NON_OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.player.chat")
@CommandParameters(description = "Send a chat message without needing to verify your ID with Microsoft.",
        usage = "/<command> <message>",
        aliases = "c,fuckofcom")
public class Command_chat extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<message..>")
    public boolean sendChatMessage(CommandContext ctx, String message)
    {
        ctx.getPlayerSender().chat(message);
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
