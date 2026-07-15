package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;

@Command(name = "hack", description = "Easter egg command - pretends to hack the server", usage = "/hack")
@Permission(permission = "tfm.fun.hack", source = SourceType.ONLY_IN_GAME)
public class Command_hack extends FCommand
{
    @Callback
    public void hack(Player player)
    {
        msg(player, "<red>Initializing hack sequence...");
        msg(player, "<yellow>Bypassing security protocols...");
        msg(player, "<gold>Accessing server files...");
        msg(player, "<green>Uploading virus...");
        msg(player, "<dark_red>ERROR: Hack failed!");
        msg(player, "<red>You have been detected and will be kicked.");

        kickPlayer(player, "<red>Nice try, hacker!\n<yellow>The server is protected by TotalFreedomMod.");
    }
}
