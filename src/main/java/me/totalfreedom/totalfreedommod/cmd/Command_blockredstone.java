package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;

@Command(name = "blockredstone", description = "Blocks redstone on the server.", usage = "/<command> [value]", aliases = {"bre"})
@Permission(level = Rank.SUPER_ADMIN, permission = "tfm.admin.blockredstone")
public class Command_blockredstone extends FCommand 
{
    @Callback
    public void toggle(final CommandSender sender)
    {
        setRedstoneConfig(sender, ConfigEntry.ALLOW_REDSTONE.getBoolean());
    }

    @Callback
    public void setRedstoneConfig(final CommandSender sender, final boolean block)
    {
        if (ConfigEntry.ALLOW_REDSTONE.getBoolean() == !block)
        {
            msg(
                sender, 
                "<gray>Redstone is already <value:disabled:enabled>.", 
                Formatter.booleanChoice("value", !block)
            );
            return;
        }

        adminAction(
            sender, 
            "<red><value:Blocking:Unblocking> all redstone.",
            Formatter.booleanChoice("value", !block)
        );

        ConfigEntry.ALLOW_REDSTONE.setBoolean(!block);
    }
}
