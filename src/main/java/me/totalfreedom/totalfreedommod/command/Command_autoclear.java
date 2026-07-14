package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.autoclear")
@CommandParameters(description = "Toggle whether or not a player has their inventory automatically cleared when they join.", usage = "/<command> <player>")
public class Command_autoclear extends FreedomCommand
{

    @CommandDispatchTarget(pattern = "<player>")
    public boolean autoclear(CommandContext ctx, String playerName)
    {
        final Player player = getPlayer(playerName);

        if (player == null)
        {
            msg(PLAYER_NOT_FOUND);
            return true;
        }

        final String name = player.getName();
        /*final boolean enabled = plugin.lp.CLEAR_ON_JOIN.stream()
                .anyMatch(entry -> entry.equalsIgnoreCase(name));

        if (enabled)
        {
            plugin.lp.CLEAR_ON_JOIN.removeIf(entry -> entry.equalsIgnoreCase(name));
        }
        else
        {
            plugin.lp.CLEAR_ON_JOIN.add(name);
        }

        msg(Component.text(name, NamedTextColor.GOLD)
                .append(Component.text(
                        enabled
                                ? " will no longer have their inventory cleared when they join."
                                : " will now have their inventory cleared when they join.",
                        NamedTextColor.AQUA)));*/

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
