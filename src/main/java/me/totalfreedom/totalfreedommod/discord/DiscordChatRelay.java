package me.totalfreedom.totalfreedommod.discord;

import java.util.concurrent.TimeUnit;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Bidirectional chat relay for the public chat channel.
 */
public class DiscordChatRelay extends ListenerAdapter
{

    private final TotalFreedomMod plugin;
    private final DiscordBridge bridge;

    public DiscordChatRelay(TotalFreedomMod plugin, DiscordBridge bridge)
    {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    private static final int DISCORD_MAX_MESSAGE_LENGTH = 1900;

    public void sendPlayerChatToDiscord(Component rendered)
    {
        String body = DiscordMarkdown.render(rendered);
        if (body.isBlank())
        {
            return;
        }

        String template = ConfigEntry.DISCORD_CHANNEL_FORMAT.getString();
        if (template != null && !template.isBlank())
        {
            body = template.replace("{message}", body);
        }

        if (body.length() > DISCORD_MAX_MESSAGE_LENGTH)
        {
            body = body.substring(0, DISCORD_MAX_MESSAGE_LENGTH) + "…";
        }

        sendToPublicChannel(body, "forward chat to Discord");
    }

    public void sendSystemMessageToDiscord(String message)
    {
        if (message == null || message.isBlank())
        {
            return;
        }

        sendToPublicChannel(sanitizeForDiscord(message), "send system message to Discord");
    }

    public void sendSystemMessageToDiscordNow(String message, long timeout, TimeUnit unit)
    {
        if (message == null || message.isBlank())
        {
            return;
        }

        TextChannel channel = bridge.getPublicChannel();
        if (channel == null)
        {
            return;
        }

        try
        {
            channel.sendMessage(sanitizeForDiscord(message)).submit().get(timeout, unit);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            FLog.warning("[Discord] Interrupted while sending system message to Discord.");
        }
        catch (Exception ex)
        {
            FLog.warning("[Discord] Failed to send system message to Discord: " + ex.getMessage());
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event)
    {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem())
        {
            return;
        }
        TextChannel channel = bridge.getPublicChannel();
        if (channel == null || !event.isFromGuild())
        {
            return;
        }
        if (!event.getChannel().getId().equals(channel.getId()))
        {
            return;
        }

        String content = event.getMessage().getContentDisplay();
        if (content.isBlank())
        {
            return;
        }

        String template = ConfigEntry.DISCORD_CHAT_FORMAT.getString();
        if (template == null || template.isBlank())
        {
            template = "&9[Discord] &r{user}&7: &f{message}";
        }
        User author = event.getAuthor();
        String displayName = event.getMember() != null ? event.getMember().getEffectiveName() : author.getName();

        String rendered = template
                .replace("{user}", displayName)
                .replace("{message}", content);
        String translated = AdventureUtil.translateAlternateColorCodes(rendered);

        Component component = LegacyComponentSerializer.legacySection().deserialize(translated);
        // Hop back to the main thread to broadcast.
        Bukkit.getScheduler().runTask(plugin, () -> FUtil.bcastMsg(component));
    }

    private static String sanitizeForDiscord(String input)
    {
        return input.replace("`", "'");
    }

    private void sendToPublicChannel(String body, String failureDescription)
    {
        TextChannel channel = bridge.getPublicChannel();
        if (channel == null)
        {
            return;
        }

        channel.sendMessage(body).queue(
                null,
                err -> FLog.warning("[Discord] Failed to " + failureDescription + ": " + err.getMessage())
        );
    }
}
