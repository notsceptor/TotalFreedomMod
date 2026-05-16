package me.totalfreedom.totalfreedommod;

import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.util.FSync;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class AntiSpam extends FreedomService
{

    public static final int MSG_PER_CYCLE = 8;
    public static final int TICKS_PER_CYCLE = 2 * 10;

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

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncPlayerChat(AsyncChatEvent event)
    {
        final Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        final FPlayer playerdata = plugin.pl.getPlayerSync(player);

        // Check for spam
        if (playerdata.incrementAndGetMsgCount() > MSG_PER_CYCLE)
        {
            FSync.bcastMsg(player.getName() + " was automatically kicked for spamming chat.", NamedTextColor.RED);
            FSync.autoEject(player, "Kicked for spamming chat.");

            playerdata.resetMsgCount();

            event.setCancelled(true);
            return;
        }

        // Check for message repeat
        if (me.totalfreedom.totalfreedommod.config.ConfigEntry.VAULT_CHAT_PREVENT_SPAM.getBoolean() && playerdata.getLastMessage().equalsIgnoreCase(message))

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
        String command = event.getMessage();
        final Player player = event.getPlayer();
        final FPlayer fPlayer = plugin.pl.getPlayer(player);
        fPlayer.setLastCommand(command);

        if (fPlayer.allCommandsBlocked())
        {
            FUtil.playerMsg(player, "Your commands have been blocked by an admin.", NamedTextColor.RED);
            event.setCancelled(true);
            return;
        }

        if (fPlayer.incrementAndGetMsgCount() > MSG_PER_CYCLE)
        {
            FUtil.bcastMsg(player.getName() + " was automatically kicked for spamming commands.", NamedTextColor.RED);
            plugin.ae.autoEject(player, "Kicked for spamming commands.");

            fPlayer.resetMsgCount();
            event.setCancelled(true);
        }
    }

}
