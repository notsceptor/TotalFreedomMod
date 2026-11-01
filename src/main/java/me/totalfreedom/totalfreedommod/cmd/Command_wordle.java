package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.entity.Player;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.SourceType;
import me.totalfreedom.api.cmd.annotation.Callback;
import me.totalfreedom.api.cmd.annotation.Command;
import me.totalfreedom.api.cmd.annotation.Permission;
import me.totalfreedom.totalfreedommod.wordle.WordleManager;

@Command(name = "wordle", description = "Play the daily Wordle game.", usage = "/wordle")
@Permission(permission = "tfm.player.wordle", source = SourceType.ONLY_IN_GAME)
public class Command_wordle extends FCommand
{
    @Callback
    public void wordle(final Player player)
    {
        plugin().services().require(WordleManager.class).open(player);
    }
}
