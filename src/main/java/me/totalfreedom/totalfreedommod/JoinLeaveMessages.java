package me.totalfreedom.totalfreedommod;

import me.totalfreedom.totalfreedommod.cmd.MessageUtils;
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
        final Player player = event.getPlayer();
        event.joinMessage(null);
        broadcast(player, "<dark_gray>[<green>+<dark_gray>] <yellow><italic><player> has joined the game.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        final Player player = event.getPlayer();
        event.quitMessage(null);
        broadcast(player, "<dark_gray>[<red>-<dark_gray>] <yellow><italic><player> has left the game.");
    }

    private boolean isAdminOrDeveloper(final Player player)
    {
        return plugin.al.isAdmin(player) || FUtil.DEVELOPERS.contains(player.getName());
    }

    /**
     * Sends a join/leave notice to every online player. The subject always sees their
     * own message; other viewers who have disabled join/leave messages still receive it
     * when the subject is an admin, so admin presence is never hideable.
     */
    private void broadcast(final Player subject, final String miniMessage)
    {
        final boolean subjectIsAdmin = isAdminOrDeveloper(subject);
        final Component message = MessageUtils.parse(miniMessage, Placeholder.unparsed("player", subject.getName()));

        for (final Player viewer : server.getOnlinePlayers())
        {
            final boolean isSubject = viewer.getUniqueId().equals(subject.getUniqueId());
            if (isSubject || subjectIsAdmin || plugin.pl.getPlayer(viewer).joinLeaveMessagesEnabled())
            {
                FUtil.playerMsg(viewer, message);
            }
        }
    }
}
