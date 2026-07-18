package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Greedy;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "gchat", description = "Send a chat message as someone else.", usage = "/gchat <player> <message>")
@Permission(permission = "tfm.admin.gchat", level = Rank.SUPER_ADMIN)
public class Command_gchat extends FCommand
{
    @Callback
    public void sendMessageAsSomeoneElse(CommandSender sender, Player player, @Greedy String message)
    {
        if (message.startsWith("/"))
        {
            if (!(CommandRegistry.getByName("gcmd") instanceof Command_gcmd gcmd))
            {
                msg(sender, "We were going to redirect you to use /gcmd instead, but apparently that isn't registered. Please contact a developer!");
                return;
            }

            gcmd.runAsOtherPlayer(sender, player, message.substring(1));
            return;
        }

        if (isAdmin(player))
        {
            msg(sender, "This command cannot be used on other admins.");
            return;
        }

        msg(sender, "<gray>Sending chat as <yellow><name><gray>: <white><message>",
                Placeholder.unparsed("name", player.getName()),
                MessageUtils.parsed("message", message)); // support tags in gchat

        player.chat(message);

        msg(sender, "<green>Chat message sent.");
    }
}
