package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH)
@CommandParameters(description = "Send a command as someone else.", usage = "/<command> <player> <command>")
public class Command_gcmd extends FreedomCommand
{

    @CommandDispatchTarget(pattern = "<player:Player> <command..>")
    public boolean runAsOtherPlayer(CommandContext ctx, Player player, String command)
    {
        if (plugin.cb.isCommandBlocked(command, sender))
        {
            msg(ctx.getSender(), "Did you really think that was going to work?", NamedTextColor.RED);
            return true;
        }

        if (plugin.al.isAdmin(player))
        {
            msg(ctx.getSender(), "This command can't be used on other admins.", NamedTextColor.RED);
            return true;
        }

        try
        {
            msg(ctx.getSender(), Component.text("Sending command as ", NamedTextColor.GRAY)
                    .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(command, NamedTextColor.WHITE)));

            if (server.dispatchCommand(player, command))
            {
                msg(ctx.getSender(), Component.text("Command sent.", NamedTextColor.GREEN));
            }
            else
            {
                msg(ctx.getSender(), Component.text("Unknown error sending command.", NamedTextColor.RED));
            }
        }
        catch (Throwable ex)
        {
            msg(ctx.getSender(), Component.text("Error sending command: " + ex.getMessage(), NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
