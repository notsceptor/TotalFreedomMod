package me.totalfreedom.totalfreedommod;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandSpy extends FreedomService
{

    public CommandSpy(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
    }

    @Override
    protected void onStop()
    {
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        final Player commandSender = event.getPlayer();
        final boolean senderIsAdmin = plugin.al.isAdmin(commandSender);

        for (Player player : plugin.al.getOnlineAdmins())
        {
            if (!plugin.pl.getPlayer(player).cmdspyEnabled())
            {
                continue;
            }

            if (senderIsAdmin && !plugin.rm.getRank(player).isAtLeast(Rank.SENIOR_ADMIN))
            {
                continue;
            }

            if (player.equals(commandSender))
            {
                continue;
            }

            FUtil.playerMsg(player, commandSender.getName() + ": " + event.getMessage());
        }
    }
}