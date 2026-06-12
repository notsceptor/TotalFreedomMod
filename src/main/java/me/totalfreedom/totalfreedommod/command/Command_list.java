package me.totalfreedom.totalfreedommod.command;

import java.util.ArrayList;
import java.util.List;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Displayable;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchContext;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.PlayerListUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.IMPOSTOR, source = SourceType.BOTH, permission = "tfm.player.list")
@CommandParameters(description = "Lists the real names of all online players.", usage = "/<command> [-a | -i | -f]", aliases = "who")
public class Command_list extends FreedomCommand
{

    private static enum ListFilter
    {

        PLAYERS,
        ADMINS,
        FAMOUS_PLAYERS,
        IMPOSTORS;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length > 1)
        {
            return false;
        }

        if (senderIsConsole && !SshDispatchContext.isActive())
        {
            msg(PlayerListUtil.buildPlainList(), NamedTextColor.WHITE);
            return true;
        }

        final ListFilter listFilter;
        if (args.length == 1)
        {
            switch (args[0])
            {
                case "-a":
                    listFilter = ListFilter.ADMINS;
                    break;
                case "-i":
                    listFilter = ListFilter.IMPOSTORS;
                    break;
                case "-f":
                    listFilter = ListFilter.FAMOUS_PLAYERS;
                    break;
                default:
                    return false;
            }
        }
        else
        {
            listFilter = ListFilter.PLAYERS;
        }

        Component onlineStats = Component.text("There are ")
                .color(NamedTextColor.BLUE)
                .append(Component.text(String.valueOf(server.getOnlinePlayers().size())).color(NamedTextColor.RED))
                .append(Component.text(" out of a maximum ").color(NamedTextColor.BLUE))
                .append(Component.text(String.valueOf(server.getMaxPlayers())).color(NamedTextColor.RED))
                .append(Component.text(" players online.").color(NamedTextColor.BLUE));

        final List<Component> nameComponents = new ArrayList<>();
        for (Player player : server.getOnlinePlayers())
        {
            if (listFilter == ListFilter.ADMINS && !plugin.al.isAdmin(player))
            {
                continue;
            }

            if (listFilter == ListFilter.IMPOSTORS && !plugin.al.isAdminImpostor(player))
            {
                continue;
            }

            if (listFilter == ListFilter.FAMOUS_PLAYERS && !ConfigEntry.FAMOUS_PLAYERS.getList().contains(player.getName().toLowerCase()))
            {
                continue;
            }

            Displayable display = plugin.rm.getDisplay(player);

            nameComponents.add(display.getColoredTag().append(Component.text(player.getName())));
        }

        String playerType = listFilter == null ? "players" : listFilter.toString().toLowerCase().replace('_', ' ');

        Component onlineUsers = Component.text("Connected " + playerType + ": ");
        
        // Join name components with ", "
        for (int i = 0; i < nameComponents.size(); i++)
        {
            if (i > 0)
            {
                onlineUsers = onlineUsers.append(Component.text(", ").color(NamedTextColor.WHITE));
            }
            onlineUsers = onlineUsers.append(nameComponents.get(i));
        }

        if (senderIsConsole)
        {
            // Console gets plain text
            sender.sendMessage(AdventureUtil.stripColor(AdventureUtil.componentToLegacy(onlineStats)));
            sender.sendMessage(AdventureUtil.stripColor(AdventureUtil.componentToLegacy(onlineUsers)));
        }
        else
        {
            sender.sendMessage(onlineStats);
            sender.sendMessage(onlineUsers);
        }

        return true;
    }
}
