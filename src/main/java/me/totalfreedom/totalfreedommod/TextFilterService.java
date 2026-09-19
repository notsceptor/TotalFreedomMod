package me.totalfreedom.totalfreedommod;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;

import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.cmd.MessageUtils;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class TextFilterService extends FreedomService
{
    private static final Map<Character, Character> LEET = Map.ofEntries(
        Map.entry('0', 'O'), Map.entry('1', 'I'), Map.entry('3', 'E'),
        Map.entry('4', 'A'), Map.entry('5', 'S'), Map.entry('7', 'T'),
        Map.entry('!', 'I'), Map.entry('|', 'I'), Map.entry('+', 'T'));

    private List<Pattern> filters = List.of();
    private List<Pattern> usernameFilters = List.of();

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
        usernameFilters = List.of();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event)
    {
        if (!shouldFilter(event.getPlayer()))
        {
            return;
        }

        final String message = MessageUtils.toPlainText(event.message());
        if (!matchesFilter(message))
        {
            return;
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> temporarilyBan(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        if (!shouldFilter(event.getPlayer()))
        {
            return;
        }

        if (!matchesFilter(event.getMessage()))
        {
            return;
        }

        event.setCancelled(true);
        temporarilyBan(event.getPlayer());
    }

    @SuppressWarnings("removal")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerEditBook(PlayerEditBookEvent event)
    {
        if (!shouldFilter(event.getPlayer()))
        {
            return;
        }

        final BookMeta book = event.getNewBookMeta();
        if (!matchesFilter(book.getTitle())
                && !matchesFilter(book.getAuthor())
                && !book.getPages().stream().anyMatch(this::matchesFilter))
        {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event)
    {
        if (!(event.getWhoClicked() instanceof Player player) || !shouldFilter(player))
        {
            return;
        }

        final boolean currentMatches = matchesFilteredItem(event.getCurrentItem());
        final boolean cursorMatches = matchesFilteredItem(event.getCursor());
        if (!currentMatches && !cursorMatches)
        {
            return;
        }

        event.setCancelled(true);
        if (currentMatches)
        {
            event.setCurrentItem(null);
        }
        if (cursorMatches)
        {
            event.getView().setCursor(null);
        }
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryCreative(InventoryCreativeEvent event)
    {
        if (!(event.getWhoClicked() instanceof Player player)
                || !shouldFilter(player)
                || !matchesFilteredItem(event.getCursor()))
        {
            return;
        }

        event.setCancelled(true);
        event.getView().setCursor(null);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event)
    {
        if (!(event.getWhoClicked() instanceof Player player) || !shouldFilter(player))
        {
            return;
        }

        if (!matchesFilteredItem(event.getOldCursor())
            && !event.getNewItems().values().stream().anyMatch(this::matchesFilteredItem))
        {
            return;
        }

        event.setCancelled(true);
        event.getView().setCursor(null);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event)
    {
        if (!shouldFilter(event.getPlayer()))
        {
            return;
        }

        final Player player = event.getPlayer();
        if (matchesFilteredItem(player.getInventory().getItem(event.getNewSlot())))
        {
            player.getInventory().setItem(event.getNewSlot(), null);
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event)
    {
        if (!shouldFilter(event.getPlayer()) || !matchesFilteredItem(event.getItemDrop().getItemStack()))
        {
            return;
        }

        event.setCancelled(true);
        event.getItemDrop().remove();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if (!shouldFilter(event.getPlayer()) || !(event.getRightClicked() instanceof Mob))
        {
            return;
        }

        final ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        if (item.getType() != Material.NAME_TAG)
        {
            return;
        }

        final ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !matchesFilter(meta.displayName()))
        {
            return;
        }

        event.setCancelled(true);
        event.getRightClicked().remove();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityAddToWorld(EntityAddToWorldEvent event)
    {
        if (!(event.getEntity() instanceof Mob mob) || !filterEnabled() || !matchesFilter(mob.customName()))
        {
            return;
        }

        mob.remove();
    }

    private void reloadFilters()
    {
        filters = compile(ConfigEntry.TEXT_FILTER_REGEX_FILTERS.getStringList());
        usernameFilters = compile(ConfigEntry.TEXT_FILTER_USERNAME_FILTERS.getStringList());

        FLog.info("Loaded " + filters.size() + " text filter regex pattern(s) and "
            + usernameFilters.size() + " username pattern(s).");
    }

    private static List<Pattern> compile(List<String> raw)
    {
        final List<Pattern> compiled = new ArrayList<>();

        for (String filter : raw)
        {
            if (filter == null || filter.isBlank())
            {
                continue;
            }

            try
            {
                compiled.add(Pattern.compile(filter, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            }
            catch (PatternSyntaxException ex)
            {
                FLog.warning("Skipping invalid text filter regex: " + ex.getDescription());
            }
        }

        return List.copyOf(compiled);
    }

    private boolean shouldFilter(Player player)
    {
        return filterEnabled() && !plugin.al.isAdmin(player);
    }

    private boolean filterEnabled()
    {
        return ConfigEntry.TEXT_FILTER_ENABLED.getBoolean(true) && !filters.isEmpty();
    }

    private boolean matchesFilter(String text)
    {
        return matchesAny(filters, text);
    }

    private boolean matchesFilter(Component text)
    {
        return text != null && matchesFilter(MessageUtils.toPlainText(text));
    }

    private boolean matchesFilteredItem(ItemStack item)
    {
        if (item == null || !(item.getItemMeta() instanceof ItemMeta meta))
        {
            return false;
        }

        if (meta.hasDisplayName() && matchesFilter(meta.displayName()))
        {
            return true;
        }
        if (meta.hasItemName() && matchesFilter(meta.itemName()))
        {
            return true;
        }
        if (!(meta instanceof BookMeta book))
        {
            return false;
        }

        return matchesFilter(book.getTitle())
                || matchesFilter(book.getAuthor())
                || book.getPages().stream().anyMatch(this::matchesFilter);
    }

    public boolean matchesUsername(String username)
    {
        return ConfigEntry.TEXT_FILTER_ENABLED.getBoolean(true) && matchesAny(usernameFilters, username);
    }

    private static boolean matchesAny(List<Pattern> patterns, String text)
    {
        if (text == null || text.isEmpty() || patterns.isEmpty())
        {
            return false;
        }

        final String folded = fold(text);
        final String deleeted = deleet(folded);

        return patterns.stream()
                       .anyMatch(filter -> filter.matcher(text).find()
                                        || filter.matcher(folded).find()
                                        || filter.matcher(deleeted).find());
    }

    private static String fold(String text)
    {
        return Normalizer.normalize(text, Normalizer.Form.NFKD)
                         .replaceAll("\\p{M}+", "");
    }

    private static String deleet(String text)
    {
        final StringBuilder out = new StringBuilder(text.length());
        text.chars().forEach(c -> out.append(LEET.getOrDefault((char) c, (char) c)));

        return out.toString();
    }

    private void temporarilyBan(Player player)
    {
        if (!player.isOnline())
        {
            return;
        }

        if (plugin.bm.getByUsername(player.getName()) != null)
        {
            player.kick(tempbanKickMessage());
            return;
        }

        final Ban ban = Ban.forPlayer(player, Bukkit.getConsoleSender(), FUtil.parseDateOffset("1d"), "Use of prohibited language");
        
        ban.addIp(player.getAddress().getAddress().getHostAddress());
        plugin.pl.getData(player).getIps().forEach(ban::addIp);

        plugin.bm.addBan(ban);

        MessageUtils.broadcast("<red><player> has been temporarily banned for prohibited language.",
            Placeholder.unparsed("player", player.getName()));
        FLog.warning("[TextFilter] Temporarily banned " + player.getName() + " for prohibited language.", true);

        player.kick(tempbanKickMessage());
    }

    private Component tempbanKickMessage()
    {
        final String message = """
            <red>Your username is temporarily banned from this server.
            Release procedures are available at
            </red><gold><url></gold>""";

        return MessageUtils.parse(message, MessageUtils.unparsed("url", ConfigEntry.SERVER_BAN_URL.getString()));
    }
}
