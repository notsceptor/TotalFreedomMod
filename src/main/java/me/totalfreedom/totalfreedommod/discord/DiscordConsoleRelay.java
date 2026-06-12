package me.totalfreedom.totalfreedommod.discord;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchContext;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchSession;
import me.totalfreedom.totalfreedommod.util.CallbackLogAppender;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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

    private final TotalFreedomMod plugin;
    private final DiscordBridge bridge;

    private final Deque<String> pendingLines = new ArrayDeque<>();
    private final Object pendingLock = new Object();

    private CallbackLogAppender logAppender;
    private BukkitTask flushTask;

    public DiscordConsoleRelay(TotalFreedomMod plugin, DiscordBridge bridge)
    {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    void attachAppender()
    {
        if (bridge.getConsoleChannel() == null)
        {
            return;
        }
        Integer flushConfig = ConfigEntry.DISCORD_CONSOLE_FLUSH.getInteger();
        int flushMs = flushConfig == null || flushConfig < 250 ? 1500 : flushConfig;
        long ticks = Math.max(1L, flushMs / 50L);

        logAppender = new CallbackLogAppender("DiscordConsoleAppender", (line, level) ->
        {
            synchronized (pendingLock)
            {
                pendingLines.add(line);
            }
        });
        logAppender.start();
        ((Logger) LogManager.getRootLogger()).addAppender(logAppender);

        flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flush, ticks, ticks);
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
        }
    }

    private void flush()
    {
        TextChannel channel = bridge.getConsoleChannel();
        if (channel == null)
        {
            return;
        }

        StringBuilder chunk = new StringBuilder();
        synchronized (pendingLock)
        {
            while (!pendingLines.isEmpty())
            {
                String line = pendingLines.peekFirst();
                // Reserve 8 chars for the ```\n ... \n``` wrapper.
                int candidate = chunk.length() + line.length() + 1;
                if (candidate > DISCORD_MAX_MESSAGE_LENGTH - 8)
                {
                    break;
                }
                pendingLines.pollFirst();
                if (!chunk.isEmpty())
                {
                    chunk.append('\n');
                }
                // Truncate single oversized lines.
                if (line.length() > DISCORD_MAX_MESSAGE_LENGTH - 16)
                {
                    line = line.substring(0, DISCORD_MAX_MESSAGE_LENGTH - 16) + "…";
                }
                chunk.append(line);
            }
        }

        if (chunk.isEmpty())
        {
            return;
        }
        String body = "```\n" + chunk + "\n```";
        channel.sendMessage(body).queue(
                null,
                err -> FLog.warning("[Discord] Console flush failed: " + err.getMessage())
        );
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event)
    {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem())
        {
            return;
        }
        TextChannel channel = bridge.getConsoleChannel();
        if (channel == null || !event.isFromGuild())
        {
            return;
        }
        if (!event.getChannel().getId().equals(channel.getId()))
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
            event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("⚠️")).queue();
            return;
        }

        if (adminUuid == null)
        {
            event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("❌")).queue();
            return;
        }

        Admin admin = plugin.al.getAdminByUuid(adminUuid);
        if (admin == null || !admin.isActive())
        {
            event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("❌")).queue();
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
}
