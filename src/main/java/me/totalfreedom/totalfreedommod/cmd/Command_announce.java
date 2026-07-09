package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;

@Command(name = "announce", description = "Make an announcement", usage = "/announce <message>")
@Permission(level = Rank.SUPER_ADMIN, permission = "tfm.admin.announce")
public class Command_announce extends FCommand 
{
    @Callback
    public void broadcast(CommandSender sender, @Greedy String content)
    {
        plugin.an.announce(content);
    }
}
