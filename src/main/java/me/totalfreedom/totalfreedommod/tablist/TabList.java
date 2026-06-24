package me.totalfreedom.totalfreedommod.tablist;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

public class TabList extends FreedomService
{
    private BukkitTask updateTask;
    private BukkitTask recolorTask;
    private boolean enabled;
    private boolean colorful;
    private Component colorfulHeader = Component.empty();
    private Component colorfulFooter = Component.empty();

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

        colorful = ConfigEntry.TABLIST_COLORFUL.getBoolean();

        if (colorful)
        {
            recolorHeaderFooter();

            long colorInterval = Math.max(
                    1,
                    ConfigEntry.TABLIST_CHANGE_COLORS.getInteger()) * 20L;

            recolorTask = plugin.getServer().getScheduler()
                    .runTaskTimer(
                            plugin,
                            this::pushRecolor,
                            colorInterval,
                            colorInterval);
        }

        long interval = ConfigEntry.TABLIST_UPDATE_INTERVAL.getInteger();

        updateTask = plugin.getServer().getScheduler()
                .runTaskTimer(
                        plugin,
                        this::updateAll,
                        interval,
                        interval);

        server.getScheduler().runTask(plugin, this::updateAll);
    }

    @Override
    protected void onStop()
    {
        if (updateTask != null)
        {
            FUtil.cancel(updateTask);
            updateTask = null;
        }

        if (recolorTask != null)
        {
            FUtil.cancel(recolorTask);
            recolorTask = null;
        }

        for (Player player : server.getOnlinePlayers())
        {
            player.sendPlayerListHeaderAndFooter(
                    Component.empty(),
                    Component.empty());
            player.playerListName(null);
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
        final CycleContext ctx = newCycleContext();
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> applyToPlayer(player, ctx),
                1L);
    }

    private void updateAll()
    {
        final CycleContext ctx = newCycleContext();

        for (Player player : server.getOnlinePlayers())
        {
            applyToPlayer(player, ctx);
        }
    }

    private CycleContext newCycleContext()
    {
        final CycleContext ctx = new CycleContext();

        if (colorful)
        {
            ctx.header = colorfulHeader;
            ctx.footer = colorfulFooter;
        }
        else
        {
            ctx.header = AdventureUtil.legacyToComponent(
                    ConfigEntry.TABLIST_HEADER.getString());

            ctx.footer = AdventureUtil.legacyToComponent(
                    ConfigEntry.TABLIST_FOOTER.getString());
        }

        return ctx;
    }

    private void applyToPlayer(Player player, CycleContext ctx)
    {
        if (!player.isOnline())
        {
            return;
        }

        player.sendPlayerListHeaderAndFooter(ctx.header, ctx.footer);
        player.playerListName(null);
    }

    private void pushRecolor()
    {
        recolorHeaderFooter();

        for (Player player : server.getOnlinePlayers())
        {
            player.sendPlayerListHeaderAndFooter(
                    colorfulHeader,
                    colorfulFooter);
        }
    }

    private void recolorHeaderFooter()
    {
        colorfulHeader = colorizeWords(
                ConfigEntry.TABLIST_HEADER.getString());

        colorfulFooter = colorizeWords(
                ConfigEntry.TABLIST_FOOTER.getString());
    }

    private Component colorizeWords(String input)
    {
        if (input == null || input.isEmpty())
        {
            return Component.empty();
        }

        final StringBuilder sb = new StringBuilder();
        final String[] lines = input.split("\n", -1);

        for (int i = 0; i < lines.length; i++)
        {
            if (i > 0)
            {
                sb.append('\n');
            }

            final String[] words = lines[i].split(" ", -1);

            for (int j = 0; j < words.length; j++)
            {
                if (j > 0)
                {
                    sb.append(' ');
                }

                final String word = words[j];

                if (word.isEmpty() || startsWithColor(word))
                {
                    sb.append(word);
                }
                else
                {
                    sb.append('&')
                            .append(randomColorChar())
                            .append(word);
                }
            }
        }

        return AdventureUtil.legacyToComponent(sb.toString());
    }

    private boolean startsWithColor(String word)
    {
        if (word.length() < 2 || word.charAt(0) != '&')
        {
            return false;
        }

        final char code = Character.toLowerCase(word.charAt(1));

        return (code >= '0' && code <= '9')
                || (code >= 'a' && code <= 'f')
                || code == 'r';
    }

    private char randomColorChar()
    {
        final NamedTextColor color = FUtil.randomChatColor();
        final ChatColor legacy =
                AdventureUtil.namedTextColorToChatColor(color);

        return (legacy != null ? legacy : ChatColor.WHITE).getChar();
    }

    private static final class CycleContext
    {
        private Component header;
        private Component footer;
    }
}
