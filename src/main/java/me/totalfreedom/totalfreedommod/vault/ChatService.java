package me.totalfreedom.totalfreedommod.vault;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Vault Chat Provider implementation for TotalFreedomMod.
 * Provides prefixes to other plugins via the Vault Chat API.
 * 
 * Extends the legacy Vault API (net.milkbowl.vault.chat.Chat) for
 * maximum compatibility with plugins that use Vault.
 */
public class ChatService extends Chat {
	private final TotalFreedomMod plugin;

	public ChatService(TotalFreedomMod plugin, Permission perms) {
		super(perms);
		this.plugin = plugin;
	}

	/**
	 * Builds the prefix string for a player (includes rank prefix and/or custom tag based on enforce_prefix setting).
	 * This is used by EssentialsChat via Vault Chat API.
	 */
	private String buildPrefixString(Player player) {
		if (player == null || !player.isOnline()) {
			return "";
		}

		FPlayer fPlayer = plugin.pl.getPlayer(player);
		if (fPlayer == null) {
			return "";
		}

		// Get player's custom tag
		String customTag = fPlayer.getTag();
		
		Boolean enforcePrefixConfig = ConfigEntry.VAULT_CHAT_ENFORCE_PREFIX.getBoolean();
		boolean enforcePrefix = enforcePrefixConfig != null ? enforcePrefixConfig : false;
		
		// Get rank prefix
		me.totalfreedom.totalfreedommod.rank.Displayable display = plugin.rm.getDisplay(player);
		String rankPrefix = "";
		
		if (display != null) {
			// Get configurable prefix for this rank/title
			String configPrefix = getConfigPrefix(display);
			if (configPrefix != null && !configPrefix.isEmpty()) {
				// Convert & color codes to § codes
				rankPrefix = ChatColor.translateAlternateColorCodes('&', configPrefix);
			} else {
				// Fall back to default rank tag
				Component coloredTag = display.getColoredTag();
				if (coloredTag != null && !coloredTag.equals(Component.empty())) {
					rankPrefix = AdventureUtil.componentToLegacySection(coloredTag);
				}
			}
		}
		
		// Format custom tag if it exists
		String formattedTag = null;
		if (customTag != null && !customTag.isEmpty()) {
			String tagTemplate = ConfigEntry.VAULT_CHAT_TAG.getString();
			if (tagTemplate == null || tagTemplate.isEmpty()) {
				tagTemplate = "&7{TAG} "; // Default template
			}
			// Replace {TAG} placeholder with actual tag value
			formattedTag = tagTemplate.replace("{TAG}", customTag);
			// Convert & color codes to § codes
			formattedTag = ChatColor.translateAlternateColorCodes('&', formattedTag);
		}
		
		if (!enforcePrefix) {
			if (formattedTag != null) {
				return formattedTag;
			}
			return customTag != null && customTag.isEmpty() ? "" : rankPrefix;
		} else {
			if (formattedTag != null) {
				return rankPrefix != null && !rankPrefix.isEmpty() ? rankPrefix + formattedTag : formattedTag;
			}
			return rankPrefix != null && !rankPrefix.isEmpty() ? rankPrefix : "";
		}
	}

	/**
	 * Gets the configured prefix for a display (rank or title).
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

	@Override
	public String getName() {
		return "TotalFreedomMod";
	}

	@Override
	public boolean isEnabled() {
		return plugin.getServer().getPluginManager().isPluginEnabled(plugin);
	}

	@Override
	public String getPlayerPrefix(String world, String player) {
		Player p = plugin.getServer().getPlayerExact(player);
		if (p != null && p.isOnline()) {
			return buildPrefixString(p);
		}
		return "";
	}

	@Override
	public void setPlayerPrefix(String world, String player, String prefix) {
		// Not used - prefixes are managed internally by TotalFreedomMod
		// This method exists for Vault API compatibility
	}

	@Override
	public String getPlayerPrefix(String world, OfflinePlayer player) {
		Player p = plugin.getServer().getPlayer(player.getUniqueId());
		if (p != null && p.isOnline()) {
			return buildPrefixString(p);
		}
		return "";
	}

	@Override
	public void setPlayerPrefix(String world, OfflinePlayer player, String prefix) {
		// Not used - prefixes are managed internally by TotalFreedomMod
	}

	@Override
	public String getPlayerPrefix(Player player) {
		return buildPrefixString(player);
	}

	@Override
	public void setPlayerPrefix(Player player, String prefix) {
		// Not used - prefixes are managed internally by TotalFreedomMod
	}

	// Add missing method for String world + Player (EssentialsChat may call this)
	public String getPlayerPrefix(String world, Player player) {
		// Ignore world parameter - we don't support world-specific prefixes
		return getPlayerPrefix(player);
	}

	@Override
	public String getPlayerSuffix(String world, String player) {
		return "";
	}

	@Override
	public void setPlayerSuffix(String world, String player, String suffix) {
		// Not used
	}

	@Override
	public String getPlayerSuffix(String world, OfflinePlayer player) {
		return "";
	}

	@Override
	public void setPlayerSuffix(String world, OfflinePlayer player, String suffix) {
		// Not used
	}

	@Override
	public String getPlayerSuffix(Player player) {
		return "";
	}

	@Override
	public void setPlayerSuffix(Player player, String suffix) {
		// Not used
	}

	@Override
	public String getGroupPrefix(String world, String group) {
		return "";
	}

	@Override
	public void setGroupPrefix(String world, String group, String prefix) {
		// Not used
	}

	@Override
	public String getGroupSuffix(String world, String group) {
		return "";
	}

	@Override
	public void setGroupSuffix(String world, String group, String suffix) {
		// Not used
	}

	@Override
	public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
		return defaultValue;
	}

	@Override
	public void setPlayerInfoInteger(String world, String player, String node, int value) {
		// Not used
	}

	@Override
	public int getPlayerInfoInteger(String world, OfflinePlayer player, String node, int defaultValue) {
		return defaultValue;
	}

	@Override
	public void setPlayerInfoInteger(String world, OfflinePlayer player, String node, int value) {
		// Not used
	}

	@Override
	public int getPlayerInfoInteger(Player player, String node, int defaultValue) {
		return defaultValue;
	}

	@Override
	public void setPlayerInfoInteger(Player player, String node, int value) {
		// Not used
	}

	@Override
	public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
		return defaultValue;
	}

	@Override
	public void setPlayerInfoDouble(String world, String player, String node, double value) {
		// Not used
	}

	@Override
	public double getPlayerInfoDouble(String world, OfflinePlayer player, String node, double defaultValue) {
		return defaultValue;
	}

	@Override
	public void setPlayerInfoDouble(String world, OfflinePlayer player, String node, double value) {
		// Not used
	}

	@Override
	public double getPlayerInfoDouble(Player player, String node, double defaultValue) {
		return defaultValue;
	}

	@Override
	public void setPlayerInfoDouble(Player player, String node, double value) {
		// Not used
	}

	@Override
	public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
		return defaultValue;
	}

	@Override
	public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {
		// Not used
	}

	@Override
	public boolean getPlayerInfoBoolean(String world, OfflinePlayer player, String node, boolean defaultValue) {
		return defaultValue;
	}

	@Override
	public void setPlayerInfoBoolean(String world, OfflinePlayer player, String node, boolean value) {
		// Not used
	}

	@Override
	public boolean getPlayerInfoBoolean(Player player, String node, boolean defaultValue) {
		return defaultValue;
	}

	@Override
	public void setPlayerInfoBoolean(Player player, String node, boolean value) {
		// Not used
	}

	@Override
	public String getPlayerInfoString(String world, String player, String node, String defaultValue) {
		return defaultValue;
	}

	@Override
	public void setPlayerInfoString(String world, String player, String node, String value) {
		// Not used
	}

	@Override
	public String getPlayerInfoString(String world, OfflinePlayer player, String node, String defaultValue) {
		return defaultValue;
	}

	@Override
	public void setPlayerInfoString(String world, OfflinePlayer player, String node, String value) {
		// Not used
	}

	@Override
	public String getPlayerInfoString(Player player, String node, String defaultValue) {
		return defaultValue;
	}

	@Override
	public void setPlayerInfoString(Player player, String node, String value) {
		// Not used
	}

	@Override
	public int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
		return defaultValue;
	}

	@Override
	public void setGroupInfoInteger(String world, String group, String node, int value) {
		// Not used
	}

	@Override
	public double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
		return defaultValue;
	}

	@Override
	public void setGroupInfoDouble(String world, String group, String node, double value) {
		// Not used
	}

	@Override
	public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
		return defaultValue;
	}

	@Override
	public void setGroupInfoBoolean(String world, String group, String node, boolean value) {
		// Not used
	}

	@Override
	public String getGroupInfoString(String world, String group, String node, String defaultValue) {
		return defaultValue;
	}

	@Override
	public void setGroupInfoString(String world, String group, String node, String value) {
		// Not used
	}
}
