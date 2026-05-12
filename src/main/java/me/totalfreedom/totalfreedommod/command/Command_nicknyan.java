package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.player.nicknyan")
@CommandParameters(description = "Essentials Interface Command - Nyanify your nickname.", usage = "/<command> <<nick> | off>")
public class Command_nicknyan extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length != 1)
        {
            return false;
        }

        if ("off".equals(args[0]))
        {
            plugin.esb.setNickname(sender.getName(), null);
            msg("Nickname cleared.");
            return true;
        }

        Component colorized = FUtil.colorize(args[0].trim());
        final String nickPlain = AdventureUtil.stripColor(AdventureUtil.componentToLegacy(colorized));

        if (!nickPlain.matches("^[a-zA-Z_0-9\u00A7]+$"))
        {
            msg("That nickname contains invalid characters.");
            return true;
        }
        else if (nickPlain.length() < 4 || nickPlain.length() > 30)
        {
            msg("Your nickname must be between 4 and 30 characters long.");
            return true;
        }

        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (player == playerSender)
            {
                continue;
            }
            if (player.getName().equalsIgnoreCase(nickPlain) || AdventureUtil.stripColor(player.getDisplayName()).trim().equalsIgnoreCase(nickPlain))
            {
                msg("That nickname is already in use.");
                return true;
            }
        }

        Component newNickComponent = Component.empty();
        final char[] chars = nickPlain.toCharArray();
        for (char c : chars)
        {
            newNickComponent = newNickComponent.append(Component.text(String.valueOf(c)).color(FUtil.randomChatColor()));
        }
        newNickComponent = newNickComponent.append(Component.text("").color(NamedTextColor.WHITE));
        
        String newNick = AdventureUtil.componentToLegacy(newNickComponent);

        plugin.esb.setNickname(sender.getName(), newNick);

        msg("Your nickname is now: " + newNick);

        return true;
    }
}
