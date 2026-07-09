package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.fun.trail")
@CommandParameters(description = "Pretty rainbow trails.", usage = "/<command> [on | off]")
public class Command_trail extends FreedomCommand
{
    @CommandDispatchTarget
    public boolean toggle(CommandContext ctx)
    {
        return addOrRemove(ctx, plugin.tr.has(ctx.getPlayerSender()));
    }

    @CommandDispatchTarget(pattern = "<value:Boolean>")
    public boolean addOrRemove(CommandContext ctx, Boolean value)
    {
        if (value)
        {
            plugin.tr.remove(ctx.getPlayerSender());
            msg(ctx.getSender(), "Trail disabled.");
        }
        else
        {
            plugin.tr.add(ctx.getPlayerSender());
            msg(ctx.getSender(), "Trail enabled.");
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }

}
