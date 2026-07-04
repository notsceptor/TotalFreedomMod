package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.autotp")
@CommandParameters(description = "Toggle whether or not a player is automatically teleported when they join.", usage = "/<command> <player>")
public class Command_autotp extends FreedomCommand
{

    @CommandDispatchTarget(pattern = "<player:Player>")
    public boolean autotp(CommandContext ctx, Player player)
    {
        final String playerName = player.getName();
        final boolean enabled = plugin.lp.TELEPORT_ON_JOIN.contains(playerName);

        if (enabled)
        {
            plugin.lp.TELEPORT_ON_JOIN.remove(playerName);
        }
        else
        {
            plugin.lp.TELEPORT_ON_JOIN.add(playerName);
        }

        msg(Component.text(playerName, NamedTextColor.GOLD)
                .append(Component.text(
                        enabled
                                ? " will no longer be automatically teleported when they join."
                                : " will now be automatically teleported when they join.",
                        NamedTextColor.AQUA)));

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}