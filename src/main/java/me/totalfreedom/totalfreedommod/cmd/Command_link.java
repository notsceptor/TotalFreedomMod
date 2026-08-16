package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.discord.DiscordBridge;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.SourceType;
import me.totalfreedom.api.cmd.annotation.*;

@Command(name = "link", description = "Generate a one-time code for admins to link their Discord account.", usage = "/link")
@Permission(permission = "tfm.admin.discordlink", source = SourceType.ONLY_IN_GAME)
public class Command_link extends FCommand
{
    @Callback
    public void link(Player player)
    {
        if (plugin().services().require(DiscordBridge.class) == null || !plugin().services().require(DiscordBridge.class).isReady())
        {
            msg(player, "<red>Discord bridge is not enabled or not ready.");
            return;
        }

        Admin admin = plugin().admins().getAdmin(player);
        if (admin == null)
        {
            msg(player, "<red>You're not in the admin list.");
            return;
        }

        String code = plugin().services().require(DiscordBridge.class).createPendingLink(admin.getUuid());
        int ttlSeconds = plugin().services().require(DiscordBridge.class).getLinkCodeTtlSeconds();

        msg(
            player,
            """
            <green>Your Discord link code is:
            <yellow><code>
            <gray>On the Discord server, you may run /link <code> to link your Discord account.
            <gray>Code expires in <seconds> seconds.""",
            Placeholder.unparsed("code", code),
            Formatter.number("seconds", ttlSeconds)
        );
    }
}
