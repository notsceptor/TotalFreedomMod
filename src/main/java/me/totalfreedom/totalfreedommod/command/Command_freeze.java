package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.freeze")
@CommandParameters(description = "Freeze players. Append \"on\" or \"off\" at the end to set a specific state.",
        usage = "/<command> <[on | off] | <player> [on | off]>", aliases = "fr")
public class Command_freeze extends FreedomCommand
{
    @CommandDispatchTarget
    public boolean toggleGlobalFreeze(CommandContext ctx)
    {
        return setGlobalFreeze(ctx, !plugin.fm.isGlobalFreeze());
    }

    @CommandDispatchTarget(pattern = "<value:Boolean>")
    public boolean setGlobalFreeze(CommandContext ctx, Boolean value)
    {
        FUtil.adminAction(ctx.getSender().getName(), (value ? "En" : "Dis") + "abling global player freeze", false);
        plugin.fm.setGlobalFreeze(value);

        if (value)
        {
            server.getOnlinePlayers().stream().filter(player -> !plugin.al.isAdmin(player)).forEach(player ->
                    msg(player, "You have been temporarily frozen due to rulebreakers. You will be unfrozen soon.", NamedTextColor.RED));
        }

        msg(ctx.getSender(), "Players are now " + (value ? "frozen" : "free to move") + ".");
        return true;
    }

    @CommandDispatchTarget(pattern = "purge")
    public boolean unfreezeAll(CommandContext ctx)
    {
        FUtil.adminAction(ctx.getSender().getName(), "Unfreezing all players", false);
        plugin.fm.purge();
        return true;
    }

    @CommandDispatchTarget(pattern = "<player:Player>")
    public boolean togglePlayerFreeze(CommandContext ctx, Player player)
    {
        return setFreezeForPlayer(ctx, player, plugin.pl.getPlayer(player).getFreezeData().isFrozen());
    }

    @CommandDispatchTarget(pattern = "<player:Player> <value:Boolean>")
    public boolean setFreezeForPlayer(CommandContext ctx, Player player, Boolean value)
    {
        plugin.pl.getPlayer(player).getFreezeData().setFrozen(value);

        msg(ctx.getSender(), player.getName() + " has been " + (value ? "frozen" : "unfrozen") + ".");
        msg(player, "You have been " + (value ? "frozen" : "unfrozen") + ".", NamedTextColor.AQUA);
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
