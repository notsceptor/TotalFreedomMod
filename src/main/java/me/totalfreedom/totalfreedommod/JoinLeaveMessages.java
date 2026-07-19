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
        event.joinMessage(null);

        final Player subject = event.getPlayer();
        if (isAdminOrDeveloper(subject))
        {
            // RankManager.onPlayerJoin already broadcasts a dedicated admin/developer
            // login announcement to everyone, unfiltered by anyone's toggle - sending
            // our own generic message on top of that would just be a duplicate.
            return;
        }

        broadcast(subject, "<gray><player> joined the game.</gray>");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        event.quitMessage(null);
        broadcast(event.getPlayer(), "<gray><player> left the game.</gray>");
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
