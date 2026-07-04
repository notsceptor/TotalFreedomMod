package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.blockredstone")
@CommandParameters(description = "Blocks redstone on the server.", usage = "/<command> [value]", aliases = "bre")
public class Command_blockredstone extends FreedomCommand
{

    @CommandDispatchTarget
    public boolean toggle(CommandContext ctx)
    {
        return setValue(ctx, ConfigEntry.ALLOW_REDSTONE.getBoolean());
    }

    @CommandDispatchTarget(pattern = "<value:Boolean>")
    public boolean setValue(CommandContext ctx, Boolean newValue)
    {
        FUtil.adminAction(ctx.getSender().getName(), (newValue ? "B" : "Unb") + "locking all redstone", true);
        ConfigEntry.ALLOW_REDSTONE.setBoolean(!newValue);
        return true;
    }

    @Override
    public boolean run(final CommandSender sender, final Player playerSender, final Command cmd, final String commandLabel, final String[] args, final boolean senderIsConsole)
    {
        return false;
    }
}