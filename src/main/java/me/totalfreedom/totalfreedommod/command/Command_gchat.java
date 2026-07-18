package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH)
@CommandParameters(description = "Send a chat message as someone else.", usage = "/<command> <player> <message>")
public class Command_gchat extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<player:Player> <message..>")
    public boolean sendMessageAsSomeoneElse(CommandContext ctx, Player player, String message)
    {
        if (message.startsWith("/"))
        {
            final FreedomCommand gcmd = CommandHandler.getByName("gcmd");
            if (gcmd == null)
            {
                msg(ctx.getSender(), "We were going to redirect you to use /gcmd instead, but apparently that isn't registered. Please contact a developer!");
                return true;
            }

            // still needs to be rewritten
            //return ((Command_gcmd) gcmd).runAsOtherPlayer(ctx, player, message.substring(1));
        }

        if (plugin.al.isAdmin(player))
        {
            msg(ctx.getSender(), "This command cannot be used on other admins.");
            return true;
        }

        msg(ctx.getSender(), Component.text("Sending chat as ", NamedTextColor.GRAY)
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(FUtil.colorizeWithLinks(message, NamedTextColor.WHITE)));

        player.chat(message);

        msg(ctx.getSender(), Component.text("Chat message sent.", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}

