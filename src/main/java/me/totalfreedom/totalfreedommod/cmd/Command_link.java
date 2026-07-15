package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@Command(name = "link", description = "Generate a one-time code for admins to link their Discord account.", usage = "/link")
@Permission(permission = "tfm.admin.discordlink", level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME)
public class Command_link extends FCommand
{
    @Callback
    public void link(Player player)
    {
        if (plugin.db == null || !plugin.db.isReady())
        {
            msg(player, "<red>Discord bridge is not enabled or not ready.");
            return;
        }

        Admin admin = plugin.al.getAdmin(player);
        if (admin == null)
        {
            msg(player, "<red>You're not in the admin list.");
            return;
        }

        String code = plugin.db.createPendingLink(admin.getUuid());
        int ttlSeconds = plugin.db.getLinkCodeTtlSeconds();

        // OKAY SO i forgot about string blocks; 
        // the newline byte sent by this IS parsed by MiniMessage (unlike \n).
        // We should actually use this for multi-lines :)
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
