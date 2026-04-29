package me.totalfreedom.totalfreedommod.tablist;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

public class TabList extends FreedomService
{

    private BukkitTask updateTask;
    private boolean enabled;

    public TabList(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        enabled = ConfigEntry.TABLIST_ENABLED.getBoolean();
        if (!enabled)
        {
            return;
        }

        long interval = ConfigEntry.TABLIST_UPDATE_INTERVAL.getInteger();
        updateTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::updateAll, interval, interval);
    }

    @Override
    protected void onStop()
    {
        if (updateTask != null)
        {
            FUtil.cancel(updateTask);
            updateTask = null;
        }

        for (Player player : server.getOnlinePlayers())
        {
            player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        if (!enabled)
        {
            return;
        }
        final Player player = event.getPlayer();
        // Delay 1 tick so all other MONITOR-priority join handlers finish first.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> applyToPlayer(player), 1L);
    }

    private void updateAll()
    {
        for (Player player : server.getOnlinePlayers())
        {
            applyToPlayer(player);
        }
    }

    private void applyToPlayer(Player player)
    {
        if (!player.isOnline())
        {
            return;
        }
        player.sendPlayerListHeaderAndFooter(buildHeader(), buildFooter());
        player.playerListName(buildPlayerListName(player));
    }

    private Component buildHeader()
    {
        return AdventureUtil.legacyToComponent(ConfigEntry.TABLIST_HEADER.getString());
    }

    private Component buildFooter()
    {
        return AdventureUtil.legacyToComponent(ConfigEntry.TABLIST_FOOTER.getString());
    }

    private Component buildPlayerListName(Player player)
    {
        // ${prefix} — rank tag / custom tag, same logic as chat prefix
        String prefix = plugin.cm.buildPlayerPrefix(player);

        // ${afk_tag} — AFK indicator (empty if not AFK or Essentials unavailable)
        String afkTag = "";
        if (plugin.esb.isAfk(player.getName()))
        {
            afkTag = ConfigEntry.TABLIST_AFK_TAG.getString();
        }

        // ${display_name} — nickname (with optional prepended indicator) or plain real name
        String displayName = resolveDisplayName(player);

        // ${name} — plain real username, no color applied
        String name = player.getName();

        String resolved = ConfigEntry.TABLIST_PLAYER_COMPONENT.getString()
                .replace("${prefix}", prefix)
                .replace("${afk_tag}", afkTag)
                .replace("${display_name}", displayName)
                .replace("${name}", name);

        return AdventureUtil.legacyToComponent(resolved);
    }

    // Returns the player's display name for the tab list:
    // Essentials may return § codes in the nickname; these are normalised to & for legacyToComponent.
    private String resolveDisplayName(Player player)
    {
        if (plugin.esb.isEssentialsEnabled())
        {
            String nickname = plugin.esb.getNickname(player.getName());
            if (nickname != null && !nickname.isEmpty()
                    && !nickname.equalsIgnoreCase(player.getName()))
            {
                String nicknameIndicator = ConfigEntry.TABLIST_DISPLAY_NICKNAME_PREFIX.getString();
                if (nicknameIndicator == null)
                {
                    nicknameIndicator = "";
                }
                return nicknameIndicator + nickname.replace('§', '&');
            }
        }
        return player.getName();
    }
}
