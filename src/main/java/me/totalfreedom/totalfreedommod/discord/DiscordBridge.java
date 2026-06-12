package me.totalfreedom.totalfreedommod.discord;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in Discord bridge (owns the JDA instance, chat/console relay and slash-command listeners).
 */
public class DiscordBridge extends FreedomService
{

    private JDA jda;
    private Guild guild;
    private TextChannel publicChannel;
    private TextChannel consoleChannel;

    private DiscordChatRelay chatRelay;
    private DiscordConsoleRelay consoleRelay;
    private DiscordCommands commands;

    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();
    private int linkCodeTtlSeconds;

    public DiscordBridge(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        if (!Boolean.TRUE.equals(ConfigEntry.DISCORD_ENABLED.getBoolean()))
        {
            return;
        }

        String token = ConfigEntry.DISCORD_TOKEN.getString();
        if (token == null || token.isBlank())
        {
            FLog.warning("[Discord] discord.enabled is true but discord.token is empty; bridge will not start.");
            return;
        }
        String guildId = ConfigEntry.DISCORD_GUILD_ID.getString();
        if (guildId == null || guildId.isBlank())
        {
            FLog.warning("[Discord] discord.guild_id is empty; bridge will not start.");
            return;
        }

        Integer ttl = ConfigEntry.DISCORD_LINK_CODE_TTL.getInteger();
        linkCodeTtlSeconds = ttl == null || ttl <= 0 ? 300 : ttl;

        try
        {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.DIRECT_MESSAGES)
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER,
                            CacheFlag.SCHEDULED_EVENTS, CacheFlag.ACTIVITY,
                            CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
                    .setMemberCachePolicy(MemberCachePolicy.NONE)
                    .addEventListeners(new ReadyListener(guildId))
                    .build();

            jda.awaitReady();
        }
        catch (Exception ex)
        {
            FLog.severe("[Discord] Failed to start JDA client: " + ex.getMessage());
            FLog.severe(ex);
            jda = null;
            return;
        }

        guild = jda.getGuildById(guildId);
        if (guild == null)
        {
            FLog.warning("[Discord] Bot is not a member of guild " + guildId + "; bridge will not start.");
            shutdownJdaQuietly();
            return;
        }

        publicChannel = resolveChannel(ConfigEntry.DISCORD_PUBLIC_CHANNEL_ID.getString(), "public_channel_id");
        consoleChannel = resolveChannel(ConfigEntry.DISCORD_CONSOLE_CHANNEL_ID.getString(), "console_channel_id");

        commands = new DiscordCommands(plugin, this);
        chatRelay = new DiscordChatRelay(plugin, this);
        consoleRelay = new DiscordConsoleRelay(plugin, this);

        jda.addEventListener(commands, chatRelay, consoleRelay);

        guild.updateCommands().addCommands(
                Commands.slash("list", "Show online players."),
                Commands.slash("link", "Redeem an in-game /link code.")
                        .addOption(OptionType.STRING, "code", "The 8-char code shown in-game.", true),
                Commands.slash("unlink", "Remove your existing Discord ↔ admin link.")
        ).queue(
                ok -> FLog.info("[Discord] Registered slash commands on guild " + guild.getName() + "."),
                err -> FLog.warning("[Discord] Failed to register slash commands: " + err.getMessage())
        );

        consoleRelay.attachAppender();

        // Periodic cleanup of expired pending link codes.
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupPendingLinks,
                20L * 60L, 20L * 60L);

        FLog.info("[Discord] Bridge ready. Guild: " + guild.getName()
                + " | public: " + (publicChannel == null ? "(none)" : publicChannel.getName())
                + " | console: " + (consoleChannel == null ? "(none)" : consoleChannel.getName()));
    }

    @Override
    protected void onStop()
    {
        if (consoleRelay != null)
        {
            consoleRelay.detachAppender();
        }

        shutdownJdaQuietly();

        pendingLinks.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event)
    {
        if (chatRelay == null || publicChannel == null)
        {
            return;
        }
        Player player = event.getPlayer();
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        chatRelay.sendPlayerChatToDiscord(player.getName(), text);
    }

    private TextChannel resolveChannel(String id, String configKey)
    {
        if (id == null || id.isBlank())
        {
            return null;
        }
        TextChannel channel = guild.getTextChannelById(id);
        if (channel == null)
        {
            FLog.warning("[Discord] " + configKey + " '" + id + "' is not a text channel in " + guild.getName() + ".");
        }
        return channel;
    }

    private void shutdownJdaQuietly()
    {
        if (jda == null)
        {
            return;
        }
        try
        {
            jda.shutdown();
            if (!jda.awaitShutdown(java.time.Duration.ofSeconds(10)))
            {
                jda.shutdownNow();
            }
        }
        catch (Exception ex)
        {
            FLog.warning("[Discord] Error during JDA shutdown: " + ex.getMessage());
        }
        finally
        {
            jda = null;
        }
    }

    public JDA getJda()
    {
        return jda;
    }

    public TextChannel getPublicChannel()
    {
        return publicChannel;
    }

    public TextChannel getConsoleChannel()
    {
        return consoleChannel;
    }

    public boolean isReady()
    {
        return jda != null && guild != null;
    }

    /**
     * Register a pending link code. Returns the generated code.
     */
    public String createPendingLink(UUID adminUuid)
    {
        long expiryMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(linkCodeTtlSeconds);
        String code;
        do
        {
            code = generateCode();
        }
        while (pendingLinks.putIfAbsent(code, new PendingLink(adminUuid, expiryMs)) != null);
        return code;
    }

    /**
     * Consume {@code code}: returns the admin UUID it was registered for and
     * removes the entry. Returns null if the code is unknown or expired.
     */
    public UUID consumePendingLink(String code)
    {
        if (code == null)
        {
            return null;
        }
        PendingLink link = pendingLinks.remove(code.toUpperCase());
        if (link == null)
        {
            return null;
        }
        if (link.expiresAtMs() < System.currentTimeMillis())
        {
            return null;
        }
        return link.adminUuid();
    }

    public int getLinkCodeTtlSeconds()
    {
        return linkCodeTtlSeconds;
    }

    private void cleanupPendingLinks()
    {
        long now = System.currentTimeMillis();
        pendingLinks.entrySet().removeIf(e -> e.getValue().expiresAtMs() < now);
    }

    private static String generateCode()
    {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        java.util.Random r = new java.util.Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++)
        {
            sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private record PendingLink(UUID adminUuid, long expiresAtMs)
    {
    }

    private static final class ReadyListener extends ListenerAdapter
    {
        private final String guildId;

        ReadyListener(String guildId)
        {
            this.guildId = guildId;
        }

        @Override
        public void onReady(@NotNull ReadyEvent event)
        {
            FLog.info("[Discord] JDA gateway ready. Bot: "
                    + event.getJDA().getSelfUser().getName()
                    + " | configured guild id: " + guildId);
        }
    }
}
