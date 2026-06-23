package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.admin.potspy")
@CommandParameters(description = "Spy on potion usage", usage = "/<command>", aliases = "potspy")
public class Command_potionspy extends FreedomCommand
{

    @CommandDispatchTarget
    public boolean toggle(CommandContext ctx)
    {
        final PlayerData data = plugin.pl.getData(ctx.getPlayerSender());
        data.setPotionSpy(!data.isPotionSpy());
        msg(String.format("PotionSpy %s.", (data.isPotionSpy() ? "enabled" : "disabled")));
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
