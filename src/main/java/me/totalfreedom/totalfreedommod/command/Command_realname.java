package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.player.realname")
@CommandParameters(description = "Finds the real name of a nicknamed player", usage = "/<command> <nickname..>")
public class Command_realname extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<nickname..>")
    public boolean realname(final CommandContext ctx, final String nickname)
    {
        boolean foundOne = false;
        for (final Player player : Bukkit.getOnlinePlayers())
        {
            final PlayerData data = plugin.pl.getData(player);
            final Component playerNickname = data.getNickname();
            if (playerNickname == null)
                continue;
            final String plainNick = AdventureUtil.componentToPlainText(playerNickname);
            if (plainNick.contains(nickname))
            {
                msg(data.getDisplayedNickname().append(
                    Component.text(" is ")
                        .append(Component.text(player.getName()))
                        .append(Component.text("."))
                        .color(NamedTextColor.GRAY)));
                foundOne = true;
            }
        }

        if (!foundOne)
            msg("Could not find a player with such a nickname.");
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
