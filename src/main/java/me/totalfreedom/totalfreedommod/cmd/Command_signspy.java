package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;

@Command(name = "signspy", description = "Spy on sign edits", usage = "/signspy", aliases = {"sspy"})
@Permission(permission = "tfm.admin.signspy", source = SourceType.ONLY_IN_GAME)
public class Command_signspy extends FCommand
{
    @Callback
    public void toggle(final Player player)
    {
        final PlayerData data = plugin().pl.getData(player);
        data.setSignSpy(!data.isSignSpy());
        plugin().pl.saveAsync();
        msg(
            player,
            "<gray>SignSpy <status:enabled:disabled>.",
            Formatter.booleanChoice("status", data.isSignSpy())
        );
    }
}
