package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.api.player.PlayerData;

@Command(name = "joinmessages", description = "Toggle visibility of other players' join/leave messages.", usage = "/joinmessages", aliases = {"jlm", "togglejoinmessages"})
@Permission(permission = "tfm.player.joinmessages", source = SourceType.ONLY_IN_GAME)
public class Command_joinmessages extends FCommand
{
    @Callback
    public void toggle(final Player player)
    {
        final FPlayer fp = plugin().players().getPlayer(player);
        final PlayerData pd = plugin().players().getData(player);
        final boolean enabled = !fp.joinLeaveMessagesEnabled();

        fp.setJoinLeaveMessagesEnabled(enabled);
        pd.setJoinLeaveMessagesEnabled(enabled);
        plugin().players().saveAsync();

        msg(player, enabled
            ? "<gray>You will now see other players' join/leave messages."
            : "<gray>You will no longer see other players' join/leave messages. Admin joins/leaves will still show.");
    }
}
