package me.totalfreedom.totalfreedommod.discord;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FUtil;

/**
 * Bidirectional chat relay for the public chat channel.
 */
public class DiscordChatRelay extends AbstractDiscordChatRelay
{
    public DiscordChatRelay(TotalFreedomMod plugin, DiscordBridge bridge)
    {
        super(bridge::currentPublicChannel,
            ConfigEntry.DISCORD_CHANNEL_FORMAT.getString(),
            ConfigEntry.DISCORD_CHAT_FORMAT.getString(),
            component -> FUtil.bcastMsg(component),
            plugin,
            bridge);
    }
}
