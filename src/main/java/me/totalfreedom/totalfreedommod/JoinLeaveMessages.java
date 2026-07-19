package me.totalfreedom.totalfreedommod;

import me.totalfreedom.totalfreedommod.cmd.MessageUtils;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveMessages extends FreedomService
{

    public JoinLeaveMessages(TotalFreedomMod plugin)
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event)
    {
        event.joinMessage(null);
        broadcast(event.getPlayer(), "<gray><player> joined the game.</gray>");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        event.quitMessage(null);
        broadcast(event.getPlayer(), "<gray><player> left the game.</gray>");
    }

    /**
     * Sends a join/leave notice to every online player, except {@code subject} itself.
     * Viewers who have disabled join/leave messages still receive it when {@code subject}
     * is an admin, so admin presence is never hideable.
     */
    private void broadcast(final Player subject, final String miniMessage)
    {
        final boolean subjectIsAdmin = plugin.al.isAdmin(subject);
        final Component message = MessageUtils.parse(miniMessage, Placeholder.unparsed("player", subject.getName()));

        for (final Player viewer : server.getOnlinePlayers())
        {
            if (viewer.getUniqueId().equals(subject.getUniqueId()))
            {
                continue;
            }

            final FPlayer viewerData = plugin.pl.getPlayer(viewer);
            if (subjectIsAdmin || viewerData.joinLeaveMessagesEnabled())
            {
                FUtil.playerMsg(viewer, message);
            }
        }
    }
}
