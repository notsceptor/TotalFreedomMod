package me.totalfreedom.totalfreedommod.discord;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Bidirectional chat relay for the chat channel chosen by the extending subclass.
 */
public abstract class AbstractDiscordChatRelay extends ListenerAdapter
{
    protected static final int DISCORD_MAX_MESSAGE_LENGTH = 1900;
    protected static final int MINECRAFT_MAX_MESSAGE_LENGTH = 200;

    private final TextChannel channel;
    private final String channelFormat;
    private final String chatFormat;
    private final Consumer<Component> chatAction;
    private final TotalFreedomMod plugin;
    private final DiscordBridge bridge;

    public AbstractDiscordChatRelay(TextChannel channel, String channelFormat, String chatFormat, Consumer<Component> chatAction, TotalFreedomMod plugin, DiscordBridge bridge)
    {
        this.channel = channel;
        this.channelFormat = channelFormat;
        this.chatFormat = chatFormat;
        this.chatAction = chatAction;
        this.plugin = plugin;
        this.bridge = bridge;
    }


    public void sendMessageToDiscord(Component rendered)
    {
        String body = DiscordMarkdown.render(rendered);
        if (body.isBlank())
        {
            return;
        }

        if (channelFormat != null && !channelFormat.isBlank())
        {
            body = channelFormat.replace("{message}", body);
        }

        if (body.length() > DISCORD_MAX_MESSAGE_LENGTH)
        {
            body = body.substring(0, DISCORD_MAX_MESSAGE_LENGTH) + "…";
        }

        sendToRelayChannel(body, "forward chat to Discord");
    }

    public void sendSystemMessageToDiscord(String message)
    {
        if (message == null || message.isBlank())
        {
            return;
        }

        sendToRelayChannel(sanitizeForDiscord(message), "send system message to Discord");
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

        String template = chatFormat;
        if (template == null || template.isBlank())
            template = "&9[Discord] &r{user}&7: &f{message}";
        User author = event.getAuthor();
        String displayName = event.getMember() != null ? event.getMember().getEffectiveName() : author.getName();

        final String truncatedContent = content.length() > MINECRAFT_MAX_MESSAGE_LENGTH ? content.substring(0, MINECRAFT_MAX_MESSAGE_LENGTH) : content;

        final Component component = buildDiscordMessage(template, displayName, truncatedContent);
        // Hop back to the main thread to broadcast.
        Bukkit.getScheduler().runTask(plugin, () -> chatAction.accept(component));
    }

    private static Component buildDiscordMessage(String template, String user, String message)
    {
        String resolved = template.replace("{user}", user);
        int msgIdx = resolved.indexOf("{message}");
        if (msgIdx < 0)
        {
            String translated = AdventureUtil.translateAlternateColorCodes(resolved.replace("{message}", message));
            return AdventureUtil.addLinks(legacySection(translated));
        }

        String before = resolved.substring(0, msgIdx);
        String after = resolved.substring(msgIdx + "{message}".length());
        Component messageComponent = AdventureUtil.addLinks(
                legacySection(AdventureUtil.translateAlternateColorCodes(trailingLegacyCodes(before) + message)));
        return legacySection(AdventureUtil.translateAlternateColorCodes(before))
                .append(messageComponent)
                .append(legacySection(AdventureUtil.translateAlternateColorCodes(after)));
    }

    private static Component legacySection(String s)
    {
        if (s == null || s.isEmpty())
        {
            return Component.empty();
        }
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }

    private static String trailingLegacyCodes(String s)
    {
        int i = s.length();
        while (i >= 2 && s.charAt(i - 2) == '&')
        {
            char code = Character.toLowerCase(s.charAt(i - 1));
            if ("0123456789abcdefklmnor".indexOf(code) >= 0)
            {
                i -= 2;
            }
            else
            {
                break;
            }
        }
        return s.substring(i);
    }

    private static String sanitizeForDiscord(String input)
    {
        return input.replace("`", "'");
    }

    private void failRelayChannelSend(final String failureDescription, final Throwable err)
    {
        FLog.warning("[Discord] Failed to " + failureDescription + (err != null ? ": " + err.getMessage() : ""));
    }

    private void sendToRelayChannel(final String body, final String failureDescription)
    {
        if (channel == null || body == null)
        {
            failRelayChannelSend(failureDescription, null);
            return;
        }

        try
        {
            channel.sendMessage(body).queue(
                    null,
                    err -> failRelayChannelSend(failureDescription, err)
            );
        }
        catch (java.util.concurrent.RejectedExecutionException ex)
        {
            failRelayChannelSend(failureDescription, ex);
        }
    }
}
