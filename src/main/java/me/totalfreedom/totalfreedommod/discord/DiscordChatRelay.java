package me.totalfreedom.totalfreedommod.discord;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
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

    public void sendPlayerChatToDiscord(String playerName, String message)
    {
        TextChannel channel = bridge.getPublicChannel();
        if (channel == null)
        {
            return;
        }
        String template = ConfigEntry.DISCORD_CHANNEL_FORMAT.getString();
        if (template == null || template.isBlank())
        {
            template = "**{player}**: {message}";
        }
        String body = template
                .replace("{player}", sanitizeForDiscord(playerName))
                .replace("{message}", sanitizeForDiscord(message));
        channel.sendMessage(body).queue(
                null,
                err -> FLog.warning("[Discord] Failed to forward chat to Discord: " + err.getMessage())
        );
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
        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.broadcast(component, "tfm.discord.receive"));
    }

    private static String sanitizeForDiscord(String input)
    {
        return input.replace("`", "'");
    }
}
