package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH)
@CommandParameters(description = "Send a chat message as someone else.", usage = "/<command> <fromname> <message>")
public class Command_gchat extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length < 2)
        {
            return false;
        }

        final Player player = getPlayer(args[0]);

        if (player == null)
        {
            msg(PLAYER_NOT_FOUND);
            return true;
        }

        if (plugin.al.isAdmin(player) && !plugin.rm.getRank(sender).isAtLeast(Rank.SENIOR_ADMIN))
        {
            msg(Component.text(
                    "You cannot gchat other admins.",
                    NamedTextColor.RED));
            return true;
        }

        final String message = StringUtils.join(args, " ", 1, args.length).strip();

        if (message.isEmpty())
        {
            msg(Component.text("You must provide a message.", NamedTextColor.RED));
            return true;
        }

        if (message.startsWith("/"))
        {
            msg(Component.text("Commands cannot be sent through /gchat. Use /gcmd instead.", NamedTextColor.RED));
            return true;
        }

        try
        {
            msg(Component.text("Sending chat as ", NamedTextColor.GRAY)
                    .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(FUtil.colorizeWithLinks(message, NamedTextColor.WHITE)));

            player.chat(message);

            msg(Component.text("Chat message sent.", NamedTextColor.GREEN));
        }
        catch (Throwable ex)
        {
            msg(Component.text(
                    "Error sending chat message: " + ex.getMessage(),
                    NamedTextColor.RED));
        }

        return true;
    }
}

