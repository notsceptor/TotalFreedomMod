package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Greedy;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import net.kyori.adventure.text.Component;

import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.command.CommandSender;

@Command(name = "realname", description = "Finds the real name of a nicknamed player", usage = "/<command> <nickname..>")
@Permission(level = Rank.OP, permission = "tfm.player.realname")
public class Command_realname extends FCommand
{
    @Callback
    public void realname(CommandSender sender, @Greedy String nickname)
    {
        AtomicBoolean foundOne = new AtomicBoolean(false);
        server().getOnlinePlayers().forEach(player -> 
        {
            final PlayerData data = plugin().pl.getData(player);
            final Component playerNickname = data.getNickname();
            if (playerNickname != null)
            {
                final String plainNick = AdventureUtil.componentToPlainText(playerNickname);
                if (plainNick.contains(nickname))
                {
                    msg(sender, "<nickname><gray> is <name>.", MessageUtils.component("nickname", data.getDisplayedNickname()),
                            MessageUtils.unparsed("name", player.getName()));
                    foundOne.set(true);
                }
            }
        });

        if (!foundOne.get())
        {
            msg(sender, "Could not find a player with such a nickname.");
        }
    }
}
