package me.totalfreedom.totalfreedommod;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.util.FSync;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class AntiSpam extends FreedomService
{

    public AntiSpam(TotalFreedomMod plugin)
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

    private static boolean enabled()
    {
        return ConfigEntry.ANTISPAM_ENABLED.getBoolean(true);
    }

    private static int limit()
    {
        return ConfigEntry.ANTISPAM_LIMIT.getInteger(8);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncPlayerChat(AsyncChatEvent event)
    {
        if (!enabled())
        {
            return;
        }
        final Player player = event.getPlayer();
        final String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        final FPlayer playerdata = plugin.pl.getPlayerSync(player);

        if (playerdata.incrementAndGetMsgCount() > limit())
        {
            FSync.bcastMsg(player.getName() + " was automatically kicked for spamming chat.", NamedTextColor.RED);
            FSync.autoEject(player, "Kicked for spamming chat.");

            playerdata.resetMsgCount();

            event.setCancelled(true);
            return;
        }

        if (ConfigEntry.VAULT_CHAT_PREVENT_SPAM.getBoolean() && playerdata.getLastMessage().equalsIgnoreCase(message))
        {
            FSync.playerMsg(player, "Please do not repeat messages.");
            event.setCancelled(true);
            return;
        }

        playerdata.setLastMessage(message);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        if (!enabled())
        {
            return;
        }
        final String command = event.getMessage();
        final Player player = event.getPlayer();
        final FPlayer fPlayer = plugin.pl.getPlayer(player);
        fPlayer.setLastCommand(command);

        if (fPlayer.allCommandsBlocked())
        {
            FUtil.playerMsg(player, "Your commands have been blocked by an admin.", NamedTextColor.RED);
            event.setCancelled(true);
            return;
        }

        if (fPlayer.incrementAndGetMsgCount() > limit())
        {
            FUtil.bcastMsg(player.getName() + " was automatically kicked for spamming commands.", NamedTextColor.RED);
            plugin.ae.autoEject(player, "Kicked for spamming commands.");

            fPlayer.resetMsgCount();
            event.setCancelled(true);
        }
    }

}
