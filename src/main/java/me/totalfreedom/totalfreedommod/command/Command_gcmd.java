package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH)
@CommandParameters(description = "Send a command as someone else.", usage = "/<command> <fromname> <outcommand>")
public class Command_gcmd extends FreedomCommand
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

        final String outCommand = StringUtils.join(args, " ", 1, args.length);

        if (plugin.cb.isCommandBlocked(outCommand, sender))
        {
            return true;
        }

        if (plugin.al.isAdmin(player) && !plugin.rm.getRank(sender).isAtLeast(Rank.SENIOR_ADMIN))
        {
            msg(Component.text(
                    "You cannot gcmd other admins.",
                    NamedTextColor.RED));
            return true;
        }

        try
        {
            msg(Component.text("Sending command as ", NamedTextColor.GRAY)
                    .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(outCommand, NamedTextColor.WHITE)));

            if (server.dispatchCommand(player, outCommand))
            {
                msg(Component.text("Command sent.", NamedTextColor.GREEN));
            }
            else
            {
                msg(Component.text("Unknown error sending command.", NamedTextColor.RED));
            }
        }
        catch (Throwable ex)
        {
            msg(Component.text(
                    "Error sending command: " + ex.getMessage(),
                    NamedTextColor.RED));
        }

        return true;
    }
}
