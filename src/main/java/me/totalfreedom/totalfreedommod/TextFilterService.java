package me.totalfreedom.totalfreedommod;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import me.totalfreedom.totalfreedommod.banning.PermBan;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class TextFilterService extends FreedomService
{

    private static final String BAN_REASON = "Use of prohibited language.";

    private List<Pattern> filters = List.of();

    public TextFilterService(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        reloadFilters();
    }

    @Override
    protected void onStop()
    {
        filters = List.of();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event)
    {
        if (!shouldFilter())
        {
            return;
        }

        final String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (!matchesFilter(message))
        {
            return;
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> permanentlyBan(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        if (!shouldFilter())
        {
            return;
        }

        if (!matchesFilter(event.getMessage()))
        {
            return;
        }

        event.setCancelled(true);
        permanentlyBan(event.getPlayer());
    }

    private void reloadFilters()
    {
        final List<Pattern> compiledFilters = new ArrayList<>();
        for (String filter : ConfigEntry.TEXT_FILTER_REGEX_FILTERS.getStringList())
        {
            if (filter == null || filter.isBlank())
            {
                continue;
            }

            try
            {
                compiledFilters.add(Pattern.compile(filter, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            }
            catch (PatternSyntaxException ex)
            {
                FLog.warning("Skipping invalid text filter regex: " + ex.getDescription());
            }
        }

        filters = List.copyOf(compiledFilters);
        FLog.info("Loaded " + filters.size() + " text filter regex pattern(s).");
    }

    private boolean shouldFilter()
    {
        return ConfigEntry.TEXT_FILTER_ENABLED.getBoolean(true) && !filters.isEmpty();
    }

    private boolean matchesFilter(String text)
    {
        if (text == null || text.isEmpty())
        {
            return false;
        }

        return filters.stream()
                .anyMatch(filter -> filter.matcher(text).find());
    }

    private void permanentlyBan(Player player)
    {
        if (!player.isOnline())
        {
            return;
        }

        if (plugin.pm.getPermban(player.getName()) != null)
        {
            player.kick(permbanKickMessage());
            return;
        }

        final PermBan permban = new PermBan(player.getUniqueId(), player.getName(), BAN_REASON);
        permban.addIps(getKnownIps(player));

        plugin.pm.addPermban(permban);

        FUtil.bcastMsg("<red><player> has been permanently banned for prohibited language.",
            Placeholder.unparsed("player", player.getName()));
        FLog.warning("[TextFilter] Permanently banned " + player.getName() + " for prohibited language.", true);

        player.kick(permbanKickMessage());
    }

    private List<String> getKnownIps(Player player)
    {
        final Set<String> ips = new LinkedHashSet<>();
        final PlayerData data = plugin.pl.getData(player);
        ips.addAll(data.getIps());

        if (player.getAddress() != null)
        {
            ips.add(player.getAddress().getAddress().getHostAddress());
        }

        if (Boolean.TRUE.equals(ConfigEntry.RANGE_BAN_IPS.getBoolean()))
        {
            new ArrayList<>(ips).stream()
                    .map(FUtil::getFuzzyIp)
                    .forEach(ips::add);
        }

        return new ArrayList<>(ips);
    }

    private Component permbanKickMessage()
    {
        return Component.text("Your username is permanently banned from this server.\n"
                        + "Release procedures are available at\n", NamedTextColor.RED)
                .append(Component.text(ConfigEntry.SERVER_PERMBAN_URL.getString(), NamedTextColor.GOLD));
    }
}
