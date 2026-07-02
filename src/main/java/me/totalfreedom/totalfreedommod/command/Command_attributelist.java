package me.totalfreedom.totalfreedommod.command;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.totalfreedom.totalfreedommod.rank.Rank;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.player.attributelist")
@CommandParameters(description = "Lists all possible attributes.", usage = "/<command>")
public class Command_attributelist extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        final List<NamespacedKey> attributes = Registry.ATTRIBUTE.keyStream()
                .sorted(Comparator.comparing(NamespacedKey::toString))
                .toList();

        Component message = Component.text("All possible attributes: ", NamedTextColor.GOLD);

        for (int i = 0; i < attributes.size(); i++)
        {
            final String attributeName = attributes.get(i).getKey().toUpperCase(Locale.ROOT);

            message = message.append(Component.text(attributeName, NamedTextColor.YELLOW));

            if (i < attributes.size() - 1)
            {
                message = message.append(Component.text(", ", NamedTextColor.GRAY));
            }
        }

        msg(message);
        return true;
    }
}