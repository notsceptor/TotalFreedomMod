package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.cleanchat")
@CommandParameters(description = "Clears the chat.", usage = "/<command>", aliases = "cc,clearchat")
public class Command_cleanchat extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        for (Player player : server.getOnlinePlayers())
        {
            if (plugin.al.isAdmin(player))
            {
                continue;
            }

            for (int i = 0; i < 100; i++)
            {
                player.sendMessage(Component.empty());
            }
        }

        FUtil.adminAction(sender.getName(), "Cleared chat", true);
        return true;
    }
}
