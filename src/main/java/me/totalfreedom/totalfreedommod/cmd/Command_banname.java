package me.totalfreedom.totalfreedommod.cmd;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.banning.Ban;
import java.util.List;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;

@Command(name = "banname", description = "Bans the specified name.", usage = "/banname <name> [reason]")
@Permission(permission = "tfm.admin.ban")
public class Command_banname extends FCommand
{
    @Completer(value = "", position = 0)
    public List<String> completeTarget(CommandSender sender, String partial)
    {
        return NameCandidates.online(server(), partial);
    }

    @Callback
    public void banName(CommandSender sender, String name)
    {
        banNameWithReason(sender, name, null);
    }

    @Callback
    public void banNameWithReason(CommandSender sender, String name, @Greedy String reason)
    {
        if (isProtectedAdminByName(sender, name))
            return;

        if (plugin().bm.getByUsername(name) != null)
        {
            msg(sender, "<gray><name> is already banned.", Placeholder.unparsed("name", name));
            return;
        }

        if (reason == null || reason.isEmpty())
            reason = "This username has been banned.";

        final Ban ban = Ban.forPlayerName(name, sender, null, reason);

        plugin().bm.addBan(ban);

        adminAction(sender, "<red>Banning the username <name>",
                Placeholder.unparsed("name", name));


        final Player player = server().getPlayer(name);
        if (player != null)
        {
            kickPlayer(player, MessageUtils.toPlainText(ban.bakeKickMessage()));
        }
    }
}
