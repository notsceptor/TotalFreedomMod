package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchContext;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.say")
@CommandParameters(description = "Broadcasts the given message as the console, includes sender name.", usage = "/<command> <message>")
public class Command_say extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length == 0)
        {
            return false;
        }

        String message = StringUtils.join(args, " ");

        if (senderIsConsole && !SshDispatchContext.isActive())
        {
            if (message.equalsIgnoreCase("WARNING: Server is restarting, you will be kicked"))
            {
                FUtil.bcastMsg("Server is going offline.", NamedTextColor.GRAY);

                for (Player player : server.getOnlinePlayers())
                {
                    player.kick(Component.text("Server is going offline, come back in about 20 seconds."));
                }

                server.shutdown();

                return true;
            }
        }

        FUtil.bcastMsg(String.format("[Server:%s] %s", sender.getName(), message), NamedTextColor.LIGHT_PURPLE);

        return true;
    }
}
