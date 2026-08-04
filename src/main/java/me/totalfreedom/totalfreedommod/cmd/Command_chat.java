package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;

@Command(
    name = "chat", 
    description = "Send a chat message without needing to verify your ID with Microsoft.",
    usage = "/chat <message>",
    aliases = {"c","fuckofcom"})
@Permission(permission = "tfm.player.chat", source = SourceType.ONLY_IN_GAME)
public final class Command_chat extends FCommand 
{
    @Callback
    public void chat(final Player player, final @Greedy String message)
    {
        player.chat(message);
    }
}