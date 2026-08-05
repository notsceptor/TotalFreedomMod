package me.totalfreedom.totalfreedommod.discord;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.discord.acquisition.DiscordAcquisition;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FTask;
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
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in Discord bridge: owns the JDA client, the chat/console relays and the slash-command
 * listeners.
 * <p>
 * The connection itself is owned by {@link DiscordAcquisition}, which guarantees this server holds
 * at most one gateway session on the token and that a failed start never leaves an orphan
 * connected. Startup runs off the main thread, so a slow or unreachable Discord no longer blocks
 * server boot the way {@code awaitReady()} on the main thread used to.
 * <p>
 * The live connection is published as one immutable {@link DiscordSession} behind a volatile
 * field. Readers on gateway and scheduler threads take a snapshot and either use a whole
 * connection or find none, and reconnecting swaps the lot in a single assignment.
 *
 * @see DiscordConnectionSupervisor
 */
public class DiscordBridge extends FreedomService
{

    public static volatile boolean reloading = false;

    private static final SecureRandom CODE_RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();

    private final DiscordAcquisition acquisition = new DiscordAcquisition();

    /**
     * Held for the duration of one connect attempt. The initial attempt and a supervisor retry can
     * overlap, and the acquisition layer would refuse the second anyway; this keeps it from
     * getting that far and logging a refusal for something benign.
     */
    private final AtomicBoolean connecting = new AtomicBoolean();

    /**
     * The live connection, or {@code null} when disconnected. Volatile because it is written by
     * the connect task and read from JDA's gateway threads and the console flush task.
     */
    private volatile DiscordSession session;

    private volatile DiscordConnectionSupervisor supervisor;
    private volatile boolean started;

    // Written by the connect task, read from JDA's gateway threads and the server main thread.
    private volatile DiscordChatRelay chatRelay;
    private volatile DiscordAdminchatRelay adminchatRelay;
    private volatile DiscordConsoleRelay consoleRelay;
    private volatile DiscordCommands commands;

    private volatile BukkitTask cleanupTask;
    private volatile int linkCodeTtlSeconds;
    private volatile boolean startedWhileReloading;

    public DiscordBridge(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        startedWhileReloading = reloading;

        if (!Boolean.TRUE.equals(ConfigEntry.DISCORD_ENABLED.getBoolean()))
            return;

        if (isBlank(ConfigEntry.DISCORD_TOKEN.getString()))
        {
            FLog.warning("[Discord] discord.enabled is true but discord.token is empty; bridge will not start.");
            return;
        }
        if (isBlank(ConfigEntry.DISCORD_GUILD_ID.getString()))
        {
            FLog.warning("[Discord] discord.guild_id is empty; bridge will not start.");
            return;
        }

        final Integer ttl = ConfigEntry.DISCORD_LINK_CODE_TTL.getInteger();
        linkCodeTtlSeconds = ttl == null || ttl <= 0 ? 300 : ttl;
        started = true;

        supervisor = new DiscordConnectionSupervisor(plugin, this::connect, this::teardownConnection);

        cleanupTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                FTask.guard("DiscordBridge/cleanupPendingLinks", this::cleanupPendingLinks),
                20L * 60L, 20L * 60L);

        runAsync("DiscordBridge/connect", this::connect);
    }

    @Override
    protected void onStop()
    {
        started = false;
        cancel(cleanupTask);
        cleanupTask = null;

        final DiscordConnectionSupervisor currentSupervisor = supervisor;
        if (currentSupervisor != null)
            currentSupervisor.beginIntentionalShutdown();

        if (consoleRelay != null)
            consoleRelay.detachAppender();

        if (chatRelay != null && !reloading && currentPublicChannel().isPresent())
        {
            chatRelay.sendSystemMessageToDiscordNow(getConfiguredMessage(ConfigEntry.DISCORD_SERVER_SHUTDOWN_MESSAGE),
                    5L, TimeUnit.SECONDS);
        }

        teardownConnection();

        supervisor = null;
        pendingLinks.clear();
    }

    // ============================================
    // Connection lifecycle
    // ============================================

    /**
     * One connect attempt: open the gateway through the acquisition layer, resolve the guild and
     * channels, and publish the session. Runs off the main thread.
     * <p>
     * Cleanup of a half-opened client is the acquisition layer's job, and the supervisor releases
     * whatever a failed attempt left behind before it retries, so the failure path here only has
     * to report the attempt.
     */
    private void connect()
    {
        final DiscordConnectionSupervisor currentSupervisor = supervisor;

        if (!started || currentSupervisor == null || currentSupervisor.hasGivenUp())
            return;

        if (session != null || !connecting.compareAndSet(false, true))
            return;

        try
        {
            final Optional<JDA> opened = acquisition.acquire(() -> buildClient(currentSupervisor));
            if (opened.isEmpty())
                return;

            final JDA client = opened.get();
            final String guildId = ConfigEntry.DISCORD_GUILD_ID.getString();
            final Guild guild = client.getGuildById(guildId);
            if (guild == null)
            {
                acquisition.release();
                currentSupervisor.reportFailure(String.format("bot is not a member of guild %s", guildId));
                return;
            }

            publishSession(new DiscordSession(client, guild,
                    resolveChannel(guild, ConfigEntry.DISCORD_PUBLIC_CHANNEL_ID.getString(), "public_channel_id"),
                    resolveChannel(guild, ConfigEntry.DISCORD_ADMINCHAT_CHANNEL_ID.getString(), "adminchat_channel_id"),
                    resolveChannel(guild, ConfigEntry.DISCORD_CONSOLE_CHANNEL_ID.getString(), "console_channel_id")),
                    currentSupervisor);
        }
        catch (Throwable thrown)
        {
            currentSupervisor.reportFailure(String.format("connect failed: %s",
                    DiscordConnectionSupervisor.describeFailure(thrown)));
        }
        finally
        {
            connecting.set(false);
        }
    }

    /**
     * The supervisor is attached here, before {@code build()} opens the gateway, so there is no
     * point in the client's life at which a drop can go unseen: a connection that dies during the
     * readiness wait, or in the moments between it and the session being published, still produces
     * a shutdown event with the supervisor listening. Closes the bridge performs itself do not
     * reach it, because the acquisition layer detaches listeners before closing anything.
     */
    private JDA buildClient(final DiscordConnectionSupervisor currentSupervisor)
    {
        return JDABuilder.createDefault(ConfigEntry.DISCORD_TOKEN.getString())
                .enableIntents(GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.DIRECT_MESSAGES)
                .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER,
                        CacheFlag.SCHEDULED_EVENTS, CacheFlag.ACTIVITY,
                        CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
                .setMemberCachePolicy(MemberCachePolicy.NONE)
                .addEventListeners(new ReadyListener(), currentSupervisor)
                .build();
    }

    /**
     * Attach the relays to a freshly opened connection and make it visible to the rest of the
     * plugin. Ordering matters: the session is published before the console appender starts, so
     * the first flush already has somewhere to send.
     */
    private void publishSession(final DiscordSession opened, final DiscordConnectionSupervisor currentSupervisor)
    {
        if (!started)
        {
            acquisition.release();
            return;
        }

        commands = new DiscordCommands(plugin, this);
        chatRelay = new DiscordChatRelay(plugin, this);
        adminchatRelay = new DiscordAdminchatRelay(plugin, this);
        consoleRelay = new DiscordConsoleRelay(plugin, this);

        session = opened;
        // Only the relays here; the supervisor has been attached since buildClient().
        opened.jda().addEventListener(commands, chatRelay, adminchatRelay, consoleRelay);

        opened.guild().updateCommands().addCommands(
                Commands.slash("list", "Show a list of players on the server."),
                Commands.slash("link", "Link your Minecraft account by entering a code from the in-game /link command.")
                        .addOption(OptionType.STRING, "code", "The 8-character code shown in-game.", true),
                Commands.slash("unlink", "Unlink your Minecraft account from your current Discord account.")
        ).queue(
                ok -> FLog.info(String.format("[Discord] Registered slash commands on guild %s.", opened.guild().getName())),
                err -> FLog.warning(String.format("[Discord] Failed to register slash commands: %s", err.getMessage()))
        );

        consoleRelay.attachAppender();
        currentSupervisor.reportConnected();

        FLog.info(String.format("[Discord] Bridge ready. Guild: %s | %s",
                opened.guild().getName(), opened.describeChannels()));

        final ConfigEntry greeting = startedWhileReloading
                ? ConfigEntry.DISCORD_PLUGIN_RELOAD_MESSAGE
                : ConfigEntry.DISCORD_SERVER_STARTUP_MESSAGE;
        startedWhileReloading = false;

        if (opened.publicChannel().isPresent())
            chatRelay.sendSystemMessageToDiscord(getConfiguredMessage(greeting));
    }

    /**
     * Take the live connection down and detach everything hanging off it. Safe to call when
     * nothing is connected, and safe to call repeatedly.
     * <p>
     * Blocks for as long as the acquisition layer takes to close the client, so callers reaching
     * here from a gateway event have to hop off that thread first.
     */
    private void teardownConnection()
    {
        session = null;

        if (consoleRelay != null)
        {
            consoleRelay.detachAppender();
            consoleRelay = null;
        }

        chatRelay = null;
        adminchatRelay = null;
        commands = null;

        acquisition.release();
    }

    // ============================================
    // Relay entry points
    // ============================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event)
    {
        final DiscordChatRelay relay = chatRelay;
        if (relay == null || currentPublicChannel().isEmpty())
            return;

        final Player player = event.getPlayer();
        Component rendered;
        try
        {
            rendered = event.renderer().render(player, player.displayName(), event.message(), Audience.empty());
        }
        catch (Exception ex)
        {
            FLog.warning(String.format("[Discord] Chat renderer threw, falling back to plain: %s", ex.getMessage()));
            rendered = Component.text(player.getName() + ": ").append(event.message());
        }
        relay.sendMessageToDiscord(rendered);
    }

    public void sendBroadcastMessage(String senderName, String message, ConfigEntry configEntry)
    {
        final String template = getConfiguredMessage(configEntry);
        if (template == null)
            return;

        sendToPublicRelay(template.replace("{sender}", senderName).replace("{message}", message));
    }

    public void sendActionMessage(String senderName, String playerName, String reason, ConfigEntry configEntry)
    {
        final String template = getConfiguredMessage(configEntry);
        if (template == null)
            return;

        sendToPublicRelay(template.replace("{sender}", senderName == null ? "CONSOLE" : senderName)
                .replace("{player}", playerName == null ? "null" : playerName)
                .replace("{reason}", reason == null || reason.isBlank() ? "No reason provided." : reason));
    }

    public void relayAdminchatMessage(CommandSender sender, Component tag, Component message)
    {
        final DiscordAdminchatRelay relay = adminchatRelay;
        if (relay == null || currentAdminchatChannel().isEmpty())
            return;

        final Component rendered = sender instanceof Player
                ? tag.append(Component.text(" " + sender.getName() + ": ")).append(message)
                : Component.text(sender.getName() + " ")
                .append(tag)
                .append(Component.text(": "))
                .append(message);
        relay.sendMessageToDiscord(rendered);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        sendPlayerStatusMessage(event.getPlayer().getName(), ConfigEntry.DISCORD_PLAYER_JOIN_MESSAGE);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        sendPlayerStatusMessage(event.getPlayer().getName(), ConfigEntry.DISCORD_PLAYER_LEAVE_MESSAGE);
    }

    public void relayLoginMessage(Component message)
    {
        if (message == null)
            return;

        final String rendered = DiscordMarkdown.render(message);
        if (rendered.isBlank())
            return;

        sendToPublicRelay(rendered);
    }

    // ============================================
    // Connection accessors
    // ============================================

    /**
     * The live connection, absent while disconnected.
     */
    public Optional<DiscordSession> getSession()
    {
        return Optional.ofNullable(session);
    }

    public Optional<TextChannel> currentPublicChannel()
    {
        return getSession().flatMap(DiscordSession::publicChannel);
    }

    public Optional<TextChannel> currentAdminchatChannel()
    {
        return getSession().flatMap(DiscordSession::adminchatChannel);
    }

    public Optional<TextChannel> currentConsoleChannel()
    {
        return getSession().flatMap(DiscordSession::consoleChannel);
    }

    /**
     * Report a send failure that means the client underneath us is gone, so the supervisor can
     * spend a reconnect attempt on it rather than waiting for a gateway event that will not come.
     */
    public void reportTransportFailure(final String context, final Throwable thrown)
    {
        final DiscordConnectionSupervisor currentSupervisor = supervisor;
        if (currentSupervisor == null)
            return;

        currentSupervisor.reportFailure(String.format("%s: %s", context,
                DiscordConnectionSupervisor.describeFailure(thrown)));
    }

    public boolean isReady()
    {
        return session != null;
    }

    // ============================================
    // Account linking
    // ============================================

    /**
     * Register a pending link code. Returns the generated code.
     */
    public String createPendingLink(UUID adminUuid)
    {
        final long expiryMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(linkCodeTtlSeconds);
        String code;
        do
        {
            code = generateCode();
        }
        while (pendingLinks.putIfAbsent(code, new PendingLink(adminUuid, expiryMs)) != null);
        return code;
    }

    /**
     * Consume {@code code}: returns the admin UUID it was registered for and removes the entry.
     * Returns empty if the code is unknown or expired.
     */
    public Optional<UUID> consumePendingLink(String code)
    {
        if (code == null)
            return Optional.empty();

        final PendingLink link = pendingLinks.remove(code.toUpperCase());
        if (link == null || link.expiresAtMs() < System.currentTimeMillis())
            return Optional.empty();

        return Optional.of(link.adminUuid());
    }

    public int getLinkCodeTtlSeconds()
    {
        return linkCodeTtlSeconds;
    }

    // ============================================
    // Helpers
    // ============================================

    private void sendPlayerStatusMessage(String playerName, ConfigEntry configEntry)
    {
        final String template = getConfiguredMessage(configEntry);
        if (template == null)
            return;

        sendToPublicRelay(template.replace("{player}", playerName));
    }

    private void sendToPublicRelay(final String message)
    {
        final DiscordChatRelay relay = chatRelay;
        if (relay == null || message == null || currentPublicChannel().isEmpty())
            return;

        relay.sendSystemMessageToDiscord(message);
    }

    private String getConfiguredMessage(ConfigEntry configEntry)
    {
        final String message = configEntry.getString();
        return isBlank(message) ? null : message;
    }

    private Optional<TextChannel> resolveChannel(final Guild guild, final String id, final String configKey)
    {
        if (isBlank(id))
            return Optional.empty();

        final TextChannel channel = guild.getTextChannelById(id);
        if (channel == null)
        {
            FLog.warning(String.format("[Discord] %s '%s' is not a text channel in %s.",
                    configKey, id, guild.getName()));
        }
        return Optional.ofNullable(channel);
    }

    private void runAsync(final String label, final Runnable body)
    {
        if (!plugin.isEnabled())
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, FTask.guard(label, body));
    }

    private void cleanupPendingLinks()
    {
        final long now = System.currentTimeMillis();
        pendingLinks.entrySet().removeIf(entry -> entry.getValue().expiresAtMs() < now);
    }

    private static void cancel(final BukkitTask task)
    {
        if (task != null)
            task.cancel();
    }

    private static boolean isBlank(final String value)
    {
        return value == null || value.isBlank();
    }

    private static String generateCode()
    {
        final StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++)
        {
            builder.append(CODE_ALPHABET.charAt(CODE_RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private record PendingLink(UUID adminUuid, long expiresAtMs)
    {
    }

    private static final class ReadyListener extends ListenerAdapter
    {
        @Override
        public void onReady(@NotNull ReadyEvent event)
        {
            FLog.info(String.format("[Discord] JDA gateway ready. Bot: %s",
                    event.getJDA().getSelfUser().getName()));
        }
    }
}
