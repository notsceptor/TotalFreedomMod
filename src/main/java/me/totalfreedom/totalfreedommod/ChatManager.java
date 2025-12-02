package me.totalfreedom.totalfreedommod;

import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FSync;
import static me.totalfreedom.totalfreedommod.util.FUtil.playerMsg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatManager extends FreedomService
{

    public ChatManager(TotalFreedomMod plugin)
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChatFormat(AsyncPlayerChatEvent event)
    {
        try
        {
            handleChatEvent(event);
        }
        catch (Exception ex)
        {
            FLog.severe(ex);
        }
    }

    private void handleChatEvent(AsyncPlayerChatEvent event)
    {
        final Player player = event.getPlayer();
        String message = event.getMessage().trim();

        // Strip color from messages
        message = AdventureUtil.stripColor(message);

        // Truncate messages that are too long - 256 characters is vanilla client max
        if (message.length() > 256)
        {
            message = message.substring(0, 256);
            FSync.playerMsg(player, "Message was shortened because it was too long to send.");
        }

        // Check for caps
        if (message.length() >= 6)
        {
            int caps = 0;
            for (char c : message.toCharArray())
            {
                if (Character.isUpperCase(c))
                {
                    caps++;
                }
            }
            if (((float) caps / (float) message.length()) > 0.65) //Compute a ratio so that longer sentences can have more caps.
            {
                message = message.toLowerCase();
            }
        }

        // Check for adminchat
        final FPlayer fPlayer = plugin.pl.getPlayerSync(player);
        if (fPlayer.inAdminChat())
        {
            FSync.adminChatMessage(player, message);
            event.setCancelled(true);
            return;
        }

        // Finally, set message
        event.setMessage(message);

        // Make format
        String format = "<%1$s> %2$s";

        String tag = fPlayer.getTag();
        if (tag != null && !tag.isEmpty())
        {
            format = tag.replace("%", "%%") + " " + format;
        }

        // Set format
        event.setFormat(format);
    }

    public void adminChat(CommandSender sender, String message)
    {
        Component nameComponent = Component.text(sender.getName() + " ")
                .append(plugin.rm.getDisplay(sender).getColoredTag())
                .append(Component.text("").color(NamedTextColor.WHITE));

        Component adminMsg = Component.text("[")
                .color(NamedTextColor.AQUA)
                .append(Component.text("ADMIN").color(NamedTextColor.AQUA))
                .append(Component.text("] ").color(NamedTextColor.WHITE))
                .append(nameComponent.color(NamedTextColor.DARK_RED))
                .append(Component.text(": ").color(NamedTextColor.DARK_RED))
                .append(Component.text(message).color(NamedTextColor.GOLD));

        // Serialize console message to ANSI for terminal colors
        Component consoleMsg = Component.text("[ADMIN] ")
                .color(NamedTextColor.AQUA)
                .append(nameComponent)
                .append(Component.text(": ").color(NamedTextColor.WHITE))
                .append(Component.text(message).color(NamedTextColor.GOLD));
        String ansiMessage = ANSIComponentSerializer.ansi().serialize(consoleMsg);
        Bukkit.getConsoleSender().sendMessage(ansiMessage);

        for (Player player : server.getOnlinePlayers())
        {
            if (plugin.al.isAdmin(player))
            {
                player.sendMessage(adminMsg);
            }
        }
    }

    public void reportAction(Player reporter, Player reported, String report)
    {
        Component reportMsg = Component.text("[REPORTS] ")
                .color(NamedTextColor.RED)
                .append(Component.text(reporter.getName() + " has reported " + reported.getName() + " for " + report)
                        .color(NamedTextColor.GOLD));

        for (Player player : server.getOnlinePlayers())
        {
            if (plugin.al.isAdmin(player))
            {
                playerMsg(player, reportMsg);
            }
        }
    }

}
