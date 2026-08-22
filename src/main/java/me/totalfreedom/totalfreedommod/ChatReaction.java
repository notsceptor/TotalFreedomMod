package me.totalfreedom.totalfreedommod;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FTask;
import me.totalfreedom.totalfreedommod.util.FUtil;

public class ChatReaction extends FreedomService
{
    private static final String CHAR_POOL = "abcdefghijklmnopqrstuvwxyz0123456789!@#$%^*";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Sound APPEAR_SOUND = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    private static final Sound WIN_SOUND = Sound.UI_TOAST_CHALLENGE_COMPLETE;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private boolean enabled;
    private long intervalTicks;
    private long answerDurationMillis;
    private int phraseLength;

    private BukkitTask reactionTask;
    private BukkitTask barTask;
    private BossBar bossBar;
    private volatile long roundStartNanos;

    private final AtomicReference<String> activePhrase = new AtomicReference<>(null);

    public ChatReaction(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        enabled = ConfigEntry.CHATREACTION_ENABLED.getBoolean();
        final int intervalSeconds = ConfigEntry.CHATREACTION_INTERVAL.getInteger();
        final int answerSeconds = ConfigEntry.CHATREACTION_DURATION.getInteger();
        intervalTicks = intervalSeconds * 20L;
        answerDurationMillis = answerSeconds * 1000L;
        phraseLength = ConfigEntry.CHATREACTION_LENGTH.getInteger();

        if (!enabled)
            return;

        reactionTask = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                FTask.run("ChatReaction/round", ChatReaction.this::startRound);
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    @Override
    protected void onStop()
    {
        if (reactionTask != null)
        {
            FUtil.cancel(reactionTask);
            reactionTask = null;
        }
        clearRoundVisuals();
        activePhrase.set(null);
    }

    private void startRound()
    {
        if (Bukkit.getOnlinePlayers().isEmpty())
            return;

        clearRoundVisuals();

        final String phrase = generatePhrase(phraseLength);
        activePhrase.set(phrase);
        roundStartNanos = System.nanoTime();

        final Map<String, Component> placeholders = new LinkedHashMap<>();
        placeholders.put("%phrase%", Component.text(phrase, NamedTextColor.AQUA));

        final Component announcement = renderTemplate(ConfigEntry.CHATREACTION_MESSAGE.getString(), placeholders);
        Bukkit.getServer().sendMessage(announcement);

        final Component barTitle = renderTemplate(ConfigEntry.CHATREACTION_BAR_MESSAGE.getString(), placeholders);
        bossBar = BossBar.bossBar(barTitle, 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);

        for (final Player online : Bukkit.getOnlinePlayers())
        {
            online.showBossBar(bossBar);
            online.playSound(online.getLocation(), APPEAR_SOUND, 1f, 1f);
        }

        barTask = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                FTask.run("ChatReaction/bar", ChatReaction.this::updateBar);
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    private void updateBar()
    {
        if (bossBar == null)
            return;

        final long elapsedMillis = (System.nanoTime() - roundStartNanos) / 1_000_000L;
        final float progress = Math.max(0f, 1f - ((float) elapsedMillis / answerDurationMillis));
        bossBar.progress(progress);

        if (progress <= 0f)
        {
            clearRoundVisuals();
            activePhrase.set(null);
        }
    }

    private void clearRoundVisuals()
    {
        if (barTask != null)
        {
            FUtil.cancel(barTask);
            barTask = null;
        }
        if (bossBar != null)
        {
            for (final Player online : Bukkit.getOnlinePlayers())
                online.hideBossBar(bossBar);
            bossBar = null;
        }
    }

    private static String generatePhrase(int length)
    {
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
        return sb.toString();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerChat(AsyncChatEvent event)
    {
        final String phrase = activePhrase.get();
        if (phrase == null)
            return;

        final String message = PlainTextComponentSerializer.plainText()
                .serialize(event.message())
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!message.equals(phrase))
            return;

        if (activePhrase.compareAndSet(phrase, null))
        {
            final Player winner = event.getPlayer();
            final double elapsedSeconds = (System.nanoTime() - roundStartNanos) / 1_000_000_000.0;

            Bukkit.getScheduler().runTask(plugin, () ->
                    FTask.run("ChatReaction/win", () -> finishRound(winner, elapsedSeconds)));
        }
    }

    private void finishRound(Player winner, double elapsedSeconds)
    {
        clearRoundVisuals();

        for (final Player online : Bukkit.getOnlinePlayers())
            online.playSound(online.getLocation(), WIN_SOUND, 1f, 1f);

        final String time = String.format(Locale.ROOT, "%.2f", elapsedSeconds);
        final Map<String, Component> placeholders = new LinkedHashMap<>();
        placeholders.put("%player%", Component.text(winner.getName(), NamedTextColor.AQUA));
        placeholders.put("%time%", Component.text(time, NamedTextColor.AQUA));

        final Component winMessage = renderTemplate(ConfigEntry.CHATREACTION_WIN_MESSAGE.getString(), placeholders);
        Bukkit.getServer().sendMessage(winMessage);
    }

    private static Component renderTemplate(String template, Map<String, Component> placeholders)
    {
        if (template == null || template.isEmpty())
            return Component.empty();

        final String[] lines = template.split("\n", -1);
        Component result = null;
        for (final String line : lines)
        {
            final Component lineComponent = renderLine(line, placeholders);
            result = (result == null) ? lineComponent : result.appendNewline().append(lineComponent);
        }
        return result == null ? Component.empty() : result;
    }

    private static Component renderLine(String line, Map<String, Component> placeholders)
    {
        Component result = Component.empty();
        int cursor = 0;

        while (cursor < line.length())
        {
            int earliestIdx = -1;
            String earliestToken = null;

            for (final String token : placeholders.keySet())
            {
                final int idx = line.indexOf(token, cursor);
                if (idx >= 0 && (earliestIdx == -1 || idx < earliestIdx))
                {
                    earliestIdx = idx;
                    earliestToken = token;
                }
            }

            if (earliestIdx == -1)
            {
                result = result.append(LEGACY.deserialize(line.substring(cursor)));
                break;
            }

            if (earliestIdx > cursor)
                result = result.append(LEGACY.deserialize(line.substring(cursor, earliestIdx)));

            result = result.append(placeholders.get(earliestToken));
            cursor = earliestIdx + earliestToken.length();
        }

        return result;
    }

}