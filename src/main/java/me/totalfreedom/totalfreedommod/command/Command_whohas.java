package me.totalfreedom.totalfreedommod.command;

import java.util.List;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.whohas")
@CommandParameters(description = "See who has a block and optionally clears the item.", usage = "/<command> <item> [-clear]", aliases = "wh")
public class Command_whohas extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<material:Material:items>", switches = "clear")
    public boolean clear(CommandContext ctx, Material material, boolean clear)
    {
        final List<Player> players = server.getOnlinePlayers().stream().filter(player -> player.getInventory().contains(material))
                .map(player -> (Player) player)
                .peek(player ->
                {
                    if (clear)
                    {
                        player.getInventory().remove(material);
                    }
                })
                .toList();

        if (players.isEmpty())
        {
            msg(ctx.getSender(), "There are no players with that item type.");
        }
        else
        {
            msg(ctx.getSender(), Component.text("Players with item type ", NamedTextColor.GRAY)
                    .append(Component.text(material.key().asString(), NamedTextColor.WHITE))
                    .append(Component.text(": "))
                    .append(Component.join(JoinConfiguration.commas(true), players.stream().map(player ->
                            ctx.isSenderConsole() ? Component.text(player.getName(), NamedTextColor.WHITE) :
                            player.displayName()
                                    .colorIfAbsent(NamedTextColor.WHITE)
                                    .hoverEvent(HoverEvent.showText(Component.text(player.getName())))
                                    .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                            ClickEvent.Payload.string("/invsee " + player.getName())))).toList())));
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
