package me.totalfreedom.totalfreedommod.command;

import java.util.Iterator;
import java.util.Map;

import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.player.colorme")
@CommandParameters(description = "Essentials Interface Command - Color your current nickname.", usage = "/<command> <color>")
public class Command_colorme extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length != 1)
        {
            return false;
        }

        if ("list".equalsIgnoreCase(args[0]))
        {
            msg("Colors: " + StringUtils.join(FUtil.CHAT_COLOR_NAMES.keySet(), ", "));
            return true;
        }

        final String needle = args[0].trim().toLowerCase();
        NamedTextColor color = null;
        final Iterator<Map.Entry<String, NamedTextColor>> it = FUtil.CHAT_COLOR_NAMES.entrySet().iterator();
        while (it.hasNext())
        {
            final Map.Entry<String, NamedTextColor> entry = it.next();
            if (entry.getKey().contains(needle))
            {
                color = entry.getValue();
                break;
            }
        }

        if (color == null)
        {
            msg("Invalid color: " + needle + " - Use \"/colorme list\" to list colors.");
            return true;
        }

        // Build nickname with color - convert to legacy string for Essentials
        String displayNamePlain = AdventureUtil.stripColor(playerSender.getDisplayName()).trim();
        Component newNick = Component.text(displayNamePlain)
            .color(color)
            .append(Component.text("").color(NamedTextColor.WHITE));
        
        final PlayerData data = plugin.pl.getData(playerSender);
        data.setNickname(newNick);

        msg(Component.text("Your nickname is now: ")
            .append(newNick));

        return true;
    }
}
