package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.util.FUtil;

@Command(name = "rawsay", description = "Broadcasts the given message. Supports colors.", usage = "/rawsay <message>")
@Permission(permission = "tfm.admin.senior.rawsay")
public class Command_rawsay extends FCommand
{
    @Callback
    public void rawsay(CommandSender sender, @Greedy String message)
    {
        FUtil.bcastMsg(MessageUtils.parse(message));
    }
}
