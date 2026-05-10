package me.totalfreedom.totalfreedommod;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FSync;
import me.totalfreedom.totalfreedommod.vault.ChatService;
import me.totalfreedom.totalfreedommod.vault.PermissionService;
import static me.totalfreedom.totalfreedommod.util.FUtil.playerMsg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

public class ChatManager extends FreedomService
{
    // The maximum message length that the Java Minecraft client can currently handle.
    private static final int MAX_MESSAGE_LENGTH_HARD_LIMIT = 32767;

    private ChatService vaultChatProvider = null;
    private PermissionService vaultPermissionProvider = null;
    private boolean essentialsChatInstalled = false;

    private String cachedRawFormat = null;
    private String cachedTranslatedFormat = null;

    public ChatManager(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        // Check for the EssentialsChat plugin
        Plugin essentialsChat = server.getPluginManager().getPlugin("EssentialsChat");
        if (essentialsChat != null && essentialsChat.isEnabled()) {
            essentialsChatInstalled = true;
        }

        // Try to register the Vault chat provider using a delayed task
        server.getScheduler().runTask(plugin, () -> {
            registerVaultChatProvider();
        });
    }

    private void registerVaultChatProvider() {
        if (!ConfigEntry.VAULT_CHAT_PROVIDER_ENABLED.getBoolean()) {
            return;
        }

		Plugin vaultPlugin = server.getPluginManager().getPlugin("Vault");
		if (vaultPlugin == null || !vaultPlugin.isEnabled()) {
			return;
		}

		try {
			org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.chat.Chat> existingProvider = server
					.getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);

			boolean shouldOverride = ConfigEntry.VAULT_CHAT_PROVIDER_OVERRIDE_EXISTING.getBoolean();
			if (existingProvider != null && !shouldOverride && !essentialsChatInstalled) {
				if (vaultChatProvider == null) {
					FLog.info("Using a registered chat provider (" + existingProvider.getProvider().getName()
							+ "). To avoid this, set 'override_existing' to 'true' in config.yml.");
				}
				return;
			}

			if (!essentialsChatInstalled && existingProvider != null) {
				FLog.info("Overriding existing chat provider (" + existingProvider.getProvider().getName() + ".");
			}

			// Register Permission provider (required to use the Vault handler)
			vaultPermissionProvider = new PermissionService(plugin);
			server.getServicesManager().register(
					Permission.class,
					vaultPermissionProvider,
					plugin,
					org.bukkit.plugin.ServicePriority.High);

			// Register Chat provider
			vaultChatProvider = new ChatService(plugin, vaultPermissionProvider);
			server.getServicesManager().register(
					Chat.class,
					vaultChatProvider,
					plugin,
					org.bukkit.plugin.ServicePriority.High);

			// Trigger EssentialsX to re-check permissions provider
			triggerEssentialsPermissionRecheck();
		} catch (Exception ex) {
			FLog.warning("Failed to register chat provider: " + ex.getMessage());
			FLog.warning(ex);
		}
	}

	/**
	 * Handles PluginEnableEvent to re-register Vault Chat Provider when Vault
	 * loads.
	 * This ensures we register even if Vault loads after TotalFreedomMod.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPluginEnable(PluginEnableEvent event) {
		if (event.getPlugin().getName().equals("Vault")) {
			server.getScheduler().runTaskLater(plugin, () -> {
				registerVaultChatProvider();
			}, 5L);
		}
	}

	/**
	 * Triggers EssentialsX to re-check permissions providers.
	 * EssentialsX checks for Vault providers on startup, but TFM registers after.
	 * This forces a re-check so EssentialsX uses TFM's Vault provider instead of
	 * superperms.
	 */
	private void triggerEssentialsPermissionRecheck() {
		Plugin essentials = server.getPluginManager().getPlugin("Essentials");
		if (essentials == null || !essentials.isEnabled()) {
			return;
		}

		try {
			// Use reflection to call PermissionsHandler.checkPermissions()
			Object permissionsHandler = essentials.getClass().getMethod("getPermissionsHandler").invoke(essentials);
			if (permissionsHandler != null) {
				permissionsHandler.getClass().getMethod("checkPermissions").invoke(permissionsHandler);
			}
		} catch (Exception ex) {
			// If reflection fails, log a warning but don't break TFM
			FLog.info(
					"Could not trigger EssentialsX to re-check permissions. Server restart may be required for prefixes to work.");
		}
	}

	@Override
	protected void onStop() {
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onPlayerChatFormat(AsyncChatEvent event) {
		try {
			handleChatEvent(event);
		} catch (Exception ex) {
			FLog.severe(ex);
		}
	}

	private void handleChatEvent(AsyncChatEvent event) {
		final Player player = event.getPlayer();
		String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

		// Handle color and formatting codes based on config
		boolean allowColors = ConfigEntry.VAULT_CHAT_ALLOW_COLOR_FORMATTING.getBoolean();
		boolean allowSpecial = ConfigEntry.VAULT_CHAT_ALLOW_SPECIAL_FORMATTING.getBoolean();
		
		if (!allowColors && !allowSpecial) {
			message = AdventureUtil.stripColor(message);
		} else {
			message = stripColorCodesSelectively(message, allowColors, allowSpecial);
			if (allowColors || allowSpecial) {
				message = ChatColor.translateAlternateColorCodes('&', message);
			}
		}

		// Truncate messages that are too long
		Integer maxLengthConfig = ConfigEntry.VAULT_CHAT_MAX_MESSAGE_LENGTH.getInteger();
		int maxLength = 256; // Default fallback
		if (maxLengthConfig != null && maxLengthConfig >= 1) {
			maxLength = maxLengthConfig;
		}
		maxLength = Math.min(maxLength, MAX_MESSAGE_LENGTH_HARD_LIMIT);
		maxLength = Math.max(1, maxLength); // Ensure at least 1
		
		if (message.length() > maxLength) {
			message = message.substring(0, maxLength);
			if (ConfigEntry.VAULT_CHAT_NOTIFY_TRUNCATED.getBoolean()) {
				FSync.playerMsg(player, "Message was shortened because it was too long to send.");
			}
		}

		// Check for caps (if enabled)
		if (ConfigEntry.VAULT_CHAT_NO_CAPS.getBoolean() && message.length() >= 6) {
			int caps = 0;
			for (char c : message.toCharArray()) {
				if (Character.isUpperCase(c)) {
					caps++;
				}
			}
			Integer capsRatioConfig = ConfigEntry.VAULT_CHAT_CAPS_RATIO.getInteger();
			double capsRatio = 0.65; // integer fallback
			if (capsRatioConfig != null && capsRatioConfig >= 0 && capsRatioConfig <= 100) {
				capsRatio = capsRatioConfig / 100.0;
			}
			if (((float) caps / (float) message.length()) > capsRatio) {
				message = message.toLowerCase();
			}
		}

		// Check for adminchat
		final FPlayer fPlayer = plugin.pl.getPlayerSync(player);
		if (fPlayer.inAdminChat()) {
			FSync.adminChatMessage(player, message);
			event.setCancelled(true);
			return;
		}

		// Finally, set message
		final Component messageComponent = (allowColors || allowSpecial)
			? LegacyComponentSerializer.legacySection().deserialize(message)
			: Component.text(message);
		event.message(messageComponent);

		// If EssentialsChat is installed, let it handle formatting
		if (essentialsChatInstalled) {
			return;
		}

		// Only set format if EssentialsChat is not installed
		// Get prefix (includes tag if present, based on enforce_prefix setting)
		String prefix = (vaultChatProvider != null)
			? vaultChatProvider.getPlayerPrefix(player)
			: getPlayerRankPrefix(player);
		String suffix = getPlayerSuffix(player);
		String worldName = player.getWorld().getName();

		final String formatTemplate = getTranslatedChatFormat();

		final String resolvedTemplate = formatTemplate
			.replace("{PREFIX}", prefix)
			.replace("{SUFFIX}", suffix)
			.replace("{WORLD}", worldName)
			.replace("{GROUP}", "");
		final int dnIdx = resolvedTemplate.indexOf("{DISPLAYNAME}");
		final int msgIdx = resolvedTemplate.indexOf("{MESSAGE}");

		event.renderer((source, sourceDisplayName, msg, viewer) ->
			buildRenderedMessage(sourceDisplayName, msg, resolvedTemplate, dnIdx, msgIdx));
	}

	private String getTranslatedChatFormat()
	{
		String raw = ConfigEntry.VAULT_CHAT_FORMAT.getString();
		if (raw == null || raw.isEmpty()) {
			raw = "{PREFIX}<{DISPLAYNAME}> {MESSAGE}";
		}
		if (raw == cachedRawFormat || raw.equals(cachedRawFormat)) {
			return cachedTranslatedFormat;
		}
		cachedRawFormat = raw;
		cachedTranslatedFormat = ChatColor.translateAlternateColorCodes('&', raw);
		return cachedTranslatedFormat;
	}

	private Component buildRenderedMessage(Component sourceDisplayName, Component message,
		String resolved, int dnIdx, int msgIdx) {

		if (dnIdx < 0 && msgIdx < 0) {
			return legacySection(resolved).append(message);
		}

		if (dnIdx >= 0 && msgIdx >= 0) {
			if (dnIdx < msgIdx) {
				return legacySection(resolved.substring(0, dnIdx))
					.append(sourceDisplayName)
					.append(legacySection(resolved.substring(dnIdx + "{DISPLAYNAME}".length(), msgIdx)))
					.append(message)
					.append(legacySection(resolved.substring(msgIdx + "{MESSAGE}".length())));
			} else {
				return legacySection(resolved.substring(0, msgIdx))
					.append(message)
					.append(legacySection(resolved.substring(msgIdx + "{MESSAGE}".length(), dnIdx)))
					.append(sourceDisplayName)
					.append(legacySection(resolved.substring(dnIdx + "{DISPLAYNAME}".length())));
			}
		}

		if (dnIdx >= 0) {
			return legacySection(resolved.substring(0, dnIdx))
				.append(sourceDisplayName)
				.append(legacySection(resolved.substring(dnIdx + "{DISPLAYNAME}".length())))
				.append(message);
		}

		// msgIdx >= 0
		return legacySection(resolved.substring(0, msgIdx))
			.append(message)
			.append(legacySection(resolved.substring(msgIdx + "{MESSAGE}".length())));
	}

	private static Component legacySection(String s) {
		if (s == null || s.isEmpty()) {
			return Component.empty();
		}
		return LegacyComponentSerializer.legacySection().deserialize(s);
	}

	public void adminChat(CommandSender sender, String message) {
		Component nameComponent = Component.text(sender.getName() + " ")
				.append(plugin.rm.getDisplay(sender).getColoredTag())
				.append(Component.text("").color(NamedTextColor.WHITE));

		Component adminMsg = Component.text("[")
				.color(NamedTextColor.AQUA)
				.append(Component.text("ADMIN").color(NamedTextColor.AQUA))
				.append(Component.text("] ").color(NamedTextColor.WHITE))
				.append(nameComponent.color(NamedTextColor.DARK_RED))
				.append(Component.text(": ").color(NamedTextColor.DARK_RED))
				.append(Component.text(message).color(NamedTextColor.GOLD));

		// Serialize console message to ANSI for terminal colors
		Component consoleMsg = Component.text("[ADMIN] ")
				.color(NamedTextColor.AQUA)
				.append(nameComponent)
				.append(Component.text(": ").color(NamedTextColor.WHITE))
				.append(Component.text(message).color(NamedTextColor.GOLD));
		String ansiMessage = ANSIComponentSerializer.ansi().serialize(consoleMsg);
		Bukkit.getConsoleSender().sendMessage(ansiMessage);

		for (Player player : plugin.al.getOnlineAdmins()) {
			player.sendMessage(adminMsg);
		}
	}

	public void reportAction(Player reporter, Player reported, String report) {
		Component reportMsg = Component.text("[REPORTS] ")
				.color(NamedTextColor.RED)
				.append(Component.text(reporter.getName() + " has reported " + reported.getName() + " for " + report)
						.color(NamedTextColor.GOLD));

		for (Player player : plugin.al.getOnlineAdmins()) {
			playerMsg(player, reportMsg);
		}
	}

	/**
	 * Gets the player's rank prefix only (without custom tag).
	 */
	private String getPlayerRankPrefix(Player player) {
		if (vaultChatProvider != null) {
			String prefix = vaultChatProvider.getPlayerPrefix(player);
			return prefix != null ? prefix : "";
		}

		// Build rank prefix directly
		me.totalfreedom.totalfreedommod.rank.Displayable display = plugin.rm.getDisplay(player);
		if (display == null) {
			return "";
		}

		// Get configurable prefix for this rank/title
		String configPrefix = getConfigPrefix(display);
		if (configPrefix != null && !configPrefix.isEmpty()) {
			return ChatColor.translateAlternateColorCodes('&', configPrefix);
		}

		// Fall back to default rank tag
		Component coloredTag = display.getColoredTag();
		if (coloredTag != null && !coloredTag.equals(Component.empty())) {
			return AdventureUtil.componentToLegacySection(coloredTag);
		}

		return "";
	}

	/**
	 * Gets the player's custom tag (from /tag command).
	 */
	private String getPlayerCustomTag(Player player) {
		FPlayer fPlayer = plugin.pl.getPlayer(player);
		if (fPlayer == null) {
			return "";
		}
		String tag = fPlayer.getTag();
		return tag != null ? tag : "";
	}

	/**
	 * Gets the configured prefix for a display rank/title.
	 * Returns null if not configured (will use default).
	 */
	private String getConfigPrefix(me.totalfreedom.totalfreedommod.rank.Displayable display) {
		if (display instanceof me.totalfreedom.totalfreedommod.rank.CustomRank) {
			me.totalfreedom.totalfreedommod.rank.CustomRank custom = (me.totalfreedom.totalfreedommod.rank.CustomRank) display;
			if (custom.getPrefix() != null && !custom.getPrefix().isEmpty()) {
				return custom.getPrefix();
			}
		}
		if (display instanceof me.totalfreedom.totalfreedommod.rank.Rank) {
			me.totalfreedom.totalfreedommod.rank.Rank rank = (me.totalfreedom.totalfreedommod.rank.Rank) display;
			switch (rank) {
				case IMPOSTOR:
					return ConfigEntry.VAULT_PREFIX_IMPOSTOR.getString();
				case NON_OP:
					return ConfigEntry.VAULT_PREFIX_NON_OP.getString();
				case OP:
					return ConfigEntry.VAULT_PREFIX_OP.getString();
				case SUPER_ADMIN:
					return ConfigEntry.VAULT_PREFIX_SUPER_ADMIN.getString();
				case TELNET_ADMIN:
					return ConfigEntry.VAULT_PREFIX_TELNET_ADMIN.getString();
				case SENIOR_ADMIN:
					return ConfigEntry.VAULT_PREFIX_SENIOR_ADMIN.getString();
				case TELNET_CONSOLE:
					return ConfigEntry.VAULT_PREFIX_TELNET_CONSOLE.getString();
				case SENIOR_CONSOLE:
					return ConfigEntry.VAULT_PREFIX_SENIOR_CONSOLE.getString();
				default:
					return null;
			}
		}
		return null;
	}

	/**
	 * Used by TabList so the same logic and config settings apply in chat and in the tab list.
	 */
	public String buildPlayerPrefix(Player player)
	{
		if (vaultChatProvider != null)
		{
			// ChatService already contains the full logic; just normalise § → &.
			return vaultChatProvider.getPlayerPrefix(player).replace('§', '&');
		}

		me.totalfreedom.totalfreedommod.rank.Displayable display = plugin.rm.getDisplay(player);
		String rankPrefix = "";

		if (display != null)
		{
			String configPrefix = getConfigPrefix(display);
			if (configPrefix != null && !configPrefix.isEmpty())
			{
				rankPrefix = ChatColor.translateAlternateColorCodes('&', configPrefix);
			}
			else
			{
				Component coloredTag = display.getColoredTag();
				if (coloredTag != null && !coloredTag.equals(Component.empty()))
				{
					rankPrefix = AdventureUtil.componentToLegacySection(coloredTag);
				}
			}
		}

		String customTag = getPlayerCustomTag(player);
		boolean enforcePrefix = Boolean.TRUE.equals(ConfigEntry.VAULT_CHAT_ENFORCE_PREFIX.getBoolean());

		String formattedTag = null;
		if (customTag != null && !customTag.isEmpty())
		{
			String tagTemplate = ConfigEntry.VAULT_CHAT_TAG.getString();
			if (tagTemplate == null || tagTemplate.isEmpty())
			{
				tagTemplate = "&7{TAG} ";
			}
			formattedTag = ChatColor.translateAlternateColorCodes('&', tagTemplate).replace("{TAG}", customTag);
		}

		String result;
		if (!enforcePrefix)
		{
			result = formattedTag != null ? formattedTag : rankPrefix;
		}
		else
		{
			result = formattedTag != null
					? (!rankPrefix.isEmpty() ? rankPrefix + formattedTag : formattedTag)
					: rankPrefix;
		}

		return result.replace('§', '&');
	}

	/**
	 * Gets the player's suffix (currently returns an empty string).
	 */
	private String getPlayerSuffix(Player player) {
		if (vaultChatProvider != null) {
			return vaultChatProvider.getPlayerSuffix(player);
		}
		return "";
	}

	/**
	 * Strips color/formatting codes selectively based on config.
	 * @param text The text to process
	 * @param allowColors Whether to allow color codes
	 * @param allowSpecial Whether to allow formatting codes
	 * @return Text with appropriate codes stripped
	 */
	private String stripColorCodesSelectively(String text, boolean allowColors, boolean allowSpecial) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		
		StringBuilder result = new StringBuilder();
		char[] chars = text.toCharArray();
		
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			
			if ((c == '&' || c == '§') && i + 1 < chars.length) {
				char code = chars[i + 1];
				boolean shouldKeep = false;
				
				if (allowColors && ((code >= '0' && code <= '9') || 
								   (code >= 'a' && code <= 'f') || 
								   (code >= 'A' && code <= 'F'))) {
					shouldKeep = true;
				}
				else if (allowSpecial && (code == 'l' || code == 'L' ||  // bold
										 code == 'o' || code == 'O' ||  // italic
										 code == 'n' || code == 'N' ||  // underline
										 code == 'm' || code == 'M' ||  // strikethrough
										 code == 'k' || code == 'K' ||  // obfuscated
										 code == 'r' || code == 'R')) { // reset
					shouldKeep = true;
				}
				
				if (shouldKeep) {
					result.append(c).append(code);
					i++;
				} else {
					i++;
				}
			} else {
				result.append(c);
			}
		}
		
		return result.toString();
	}

}
