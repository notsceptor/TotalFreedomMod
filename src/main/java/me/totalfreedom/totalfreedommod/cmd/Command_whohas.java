package me.totalfreedom.totalfreedommod.cmd;

import java.util.List;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Permission(level = Rank.SUPER_ADMIN, permission = "tfm.admin.whohas")
@Command(name = "whohas", aliases = "wh", description = "See who has a block and optionally clears the item.", usage = "/<command> [-clear] <item>")
public class Command_whohas extends FCommand
{
    @Callback
    public void query(CommandSender sender, @Resolve("MaterialQuery") List<Material> materials, @Switch("clear") boolean clear)
    {
        final boolean senderIsConsole = !(sender instanceof Player);

        long count = materials.stream().filter(material ->
        {
            final List<? extends Player> players = server().getOnlinePlayers()
                                                           .stream()
                                                           .filter(player -> player.getInventory().contains(material))
                                                           .peek(player ->
                                                            {
                                                                if (clear)
                                                                    player.getInventory().remove(material);
                                                            })
                                                            .toList();

            if (!players.isEmpty())
            {
                final List<Component> segments = players.stream()
                                                        .map(player ->
                                                            senderIsConsole ? 
                                                            Component.text(player.getName(), NamedTextColor.WHITE) :
                                                            player.displayName()
                                                                  .colorIfAbsent(NamedTextColor.WHITE)
                                                                  .hoverEvent(HoverEvent.showText(Component.text(player.getName())))
                                                                  .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                                                                                    ClickEvent.Payload.string(plugin().esb.isEssentialsEnabled() ?
                                                                                                                              "/invsee " + player.getName() :
                                                                                                                              "/data get entity " + player.getUniqueId() + " Inventory")
                                                                                                 )
                                                                  )
                                                        )
                                                        .toList();

                msg(sender, "<gray>Players with item type <name>: <players>",
                        MessageUtils.unparsed("name", material.key().asString()),
                        MessageUtils.joinedComponents("players", segments));

                return true;
            }

            return false;
        }).count();

        if (count == 0)
        {
            msg(sender, "No results were found for your query.");
        }
    }
}
