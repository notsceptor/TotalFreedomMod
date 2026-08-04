package me.totalfreedom.totalfreedommod.discord;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchContext;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchSession;
import me.totalfreedom.totalfreedommod.util.CallbackLogAppender;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FTask;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * Streams server log output into the Discord console channel and dispatches
 * plain Minecraft commands typed into that channel as the linked admin.
 */
public class DiscordConsoleRelay extends ListenerAdapter
{

    /** Discord hard message-length limit. We pack lines into chunks below this. */
    private static final int DISCORD_MAX_MESSAGE_LENGTH = 1900;

    /** Length of the {@code ```ansi\n} ... {@code \n```} wrapper each chunk is sent inside. */
    private static final int CODE_FENCE_LENGTH = 12;

    /** Longest chunk that still fits inside the fence. */
    private static final int MAX_CHUNK_LENGTH = DISCORD_MAX_MESSAGE_LENGTH - CODE_FENCE_LENGTH;

    private static final int DEFAULT_QUEUE_LIMIT = 2000;
    private static final int DEFAULT_FLUSH_MS = 1500;
    private static final int MIN_FLUSH_MS = 250;

    /**
     * Loggers whose output is never relayed. JDA reports its own rate limiting and connection
     * trouble through the root logger, and relaying that back into the channel being rate limited
     * makes every problem generate more traffic describing itself.
     */
    private static final String[] EXCLUDED_LOGGERS = {"net.dv8tion.jda", "okhttp3", "club.minnced"};

    /**
     * Set while this relay logs its own failures. The appender is on the root logger, so without
     * it a failed send would enqueue the warning about the failed send, and that warning would be
     * in the next attempt's payload.
     */
    private static final ThreadLocal<Boolean> SUPPRESS_CAPTURE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final TotalFreedomMod plugin;
    private final DiscordBridge bridge;

    private final Deque<String> pendingLines = new ArrayDeque<>();
    private final Object pendingLock = new Object();

    private final int queueLimit;

    /** Lines discarded because the queue was full, reported in the next successful flush. */
    private int droppedLines;

    private CallbackLogAppender logAppender;
    private BukkitTask flushTask;

    public DiscordConsoleRelay(TotalFreedomMod plugin, DiscordBridge bridge)
    {
        this.plugin = plugin;
        this.bridge = bridge;
        this.queueLimit = Math.max(64, ConfigEntry.DISCORD_CONSOLE_QUEUE_LIMIT.getInteger(DEFAULT_QUEUE_LIMIT));
    }

    void attachAppender()
    {
        if (bridge.currentConsoleChannel().isEmpty())
        {
            return;
        }

        Integer flushConfig = ConfigEntry.DISCORD_CONSOLE_FLUSH.getInteger();
        int flushMs = flushConfig == null || flushConfig < MIN_FLUSH_MS ? DEFAULT_FLUSH_MS : flushConfig;
        long ticks = Math.max(1L, flushMs / 50L);

        logAppender = new CallbackLogAppender("DiscordConsoleAppender", (line, level) -> enqueue(line))
                .excludeLoggers(EXCLUDED_LOGGERS);
        logAppender.start();
        ((Logger) LogManager.getRootLogger()).addAppender(logAppender);

        flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                FTask.guard("DiscordConsoleRelay/flush", this::flush), ticks, ticks);
    }

    void detachAppender()
    {
        if (logAppender != null)
        {
            ((Logger) LogManager.getRootLogger()).removeAppender(logAppender);
            logAppender.stop();
            logAppender = null;
        }
        if (flushTask != null)
        {
            flushTask.cancel();
            flushTask = null;
        }
        synchronized (pendingLock)
        {
            pendingLines.clear();
            droppedLines = 0;
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event)
    {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem())
        {
            return;
        }
        if (!event.isFromGuild())
        {
            return;
        }

        Optional<TextChannel> channel = bridge.currentConsoleChannel();
        if (channel.isEmpty() || !event.getChannel().getId().equals(channel.get().getId()))
        {
            return;
        }

        String content = event.getMessage().getContentRaw().trim();
        if (content.isEmpty())
        {
            return;
        }

        String discordUserId = event.getAuthor().getId();
        UUID adminUuid;
        try
        {
            adminUuid = plugin.dm.getDiscordLinkRepository().findAdminUuidByDiscordId(discordUserId);
        }
        catch (SQLException ex)
        {
            FLog.warning("[Discord] discord_links lookup failed: " + ex.getMessage());
            react(event, "⚠️");
            return;
        }

        if (adminUuid == null)
        {
            react(event, "❌");
            return;
        }

        Admin admin = plugin.al.getAdminByUuid(adminUuid);
        if (admin == null || !admin.isActive())
        {
            react(event, "❌");
            return;
        }

        String commandLine = content.startsWith("/") ? content.substring(1) : content;
        if (commandLine.isEmpty())
        {
            return;
        }

        String displayName = "Discord@" + admin.getName();
        RemoteDispatchSession session = new RemoteDispatchSession(
                RemoteDispatchSession.Channel.DISCORD,
                admin.getName(),
                displayName,
                true);

        Bukkit.getScheduler().runTask(plugin, () ->
        {
            FLog.info("[Discord: " + admin.getName() + "] /" + commandLine);
            RemoteDispatchContext.dispatch(session, commandLine);
        });
    }

    /**
     * Queue one captured log line, dropping the oldest when the queue is full.
     * <p>
     * The bound matters because everything downstream can stall: a channel that has gone away, a
     * bot without send permission, or a rate limit all leave lines arriving with nowhere to go.
     * Unbounded, that is a heap leak that only ends when the server does.
     */
    private void enqueue(String line)
    {
        if (Boolean.TRUE.equals(SUPPRESS_CAPTURE.get()))
        {
            return;
        }

        synchronized (pendingLock)
        {
            while (pendingLines.size() >= queueLimit)
            {
                pendingLines.pollFirst();
                droppedLines++;
            }
            pendingLines.addLast(line);
        }
    }

    private void flush()
    {
        Optional<TextChannel> channel = bridge.currentConsoleChannel();
        if (channel.isEmpty())
        {
            return;
        }

        String chunk = drainChunk();
        if (chunk.isEmpty())
        {
            return;
        }

        String body = "```ansi\n" + chunk + "\n```";
        try
        {
            channel.get().sendMessage(body).queue(
                    null,
                    err -> warnWithoutCapture("[Discord] Console flush failed: " + err.getMessage())
            );
        }
        catch (RejectedExecutionException ex)
        {
            // The client is gone, so the failure consumer above will never run. Tell the
            // supervisor so this counts toward the reconnect budget.
            warnWithoutCapture("[Discord] Console flush rejected: " + ex.getMessage());
            bridge.reportTransportFailure("console flush", ex);
        }
    }

    /**
     * Take as many queued lines as fit in one message.
     * <p>
     * Each line is shortened to fit <em>before</em> it is measured against the remaining budget.
     * Testing an over-long line first and breaking out of the loop left it sitting at the head of
     * the deque, where it failed the same test on every subsequent flush: a single log line longer
     * than one Discord message used to stop console output permanently, with the queue behind it
     * growing forever, while inbound commands kept working and made the relay look healthy.
     */
    private String drainChunk()
    {
        StringBuilder chunk = new StringBuilder();
        int dropped;

        synchronized (pendingLock)
        {
            dropped = droppedLines;
            droppedLines = 0;

            if (dropped > 0)
            {
                chunk.append(String.format("... %d line(s) dropped, console output is falling behind ...", dropped));
            }

            while (!pendingLines.isEmpty())
            {
                String line = truncateToFit(pendingLines.peekFirst());

                // The first line always goes in, having already been cut to fit on its own.
                if (!chunk.isEmpty() && chunk.length() + 1 + line.length() > MAX_CHUNK_LENGTH)
                {
                    break;
                }

                pendingLines.pollFirst();

                if (!chunk.isEmpty())
                {
                    chunk.append('\n');
                }
                chunk.append(line);
            }
        }

        return chunk.toString();
    }

    private static String truncateToFit(String line)
    {
        return line.length() <= MAX_CHUNK_LENGTH
                ? line
                : line.substring(0, MAX_CHUNK_LENGTH - 1) + "…";
    }

    /**
     * Log a relay failure without that log line being captured and queued for delivery to the
     * channel that just failed.
     */
    private static void warnWithoutCapture(String message)
    {
        SUPPRESS_CAPTURE.set(Boolean.TRUE);
        try
        {
            FLog.warning(message);
        }
        finally
        {
            SUPPRESS_CAPTURE.set(Boolean.FALSE);
        }
    }

    private static void react(MessageReceivedEvent event, String emoji)
    {
        event.getMessage().addReaction(Emoji.fromUnicode(emoji)).queue(null, ignored -> {});
    }
}
