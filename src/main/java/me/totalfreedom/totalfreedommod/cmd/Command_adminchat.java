package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.player.FPlayer;

@Command(name = "adminchat",
        description = "AdminChat - Talk privately with other admins. Using the command by itself will toggle AdminChat on and off for all messages.",
        usage = "/<command> [message...]",
        aliases = {"o", "ac"})
@Permission(source = SourceType.BOTH, permission = "tfm.admin.adminchat")
public class Command_adminchat extends FCommand
{

    @Callback
    @Permission(permission = "tfm.admin.adminchat", source = SourceType.ONLY_IN_GAME)
    public void toggle(Player sender)
    {
        final FPlayer fplayer = plugin().pl.getPlayer(sender);
        boolean mode = !fplayer.inAdminChat();

        fplayer.setAdminChat(mode);
        msg(sender, "<gray>Toggled Admin Chat <mode:on:off>.", Formatter.booleanChoice("mode", mode));
    }

    @Callback
    public void sendMessage(CommandSender sender, @Greedy String message)
    {
        plugin().cm.adminChat(sender, message);
    }
}
