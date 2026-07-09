package me.totalfreedom.totalfreedommod.cmd;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Command(name = "attributelist", description = "List all possible attributes", usage = "/attributelist")
@Permission(permission = "tfm.player.attributelist")
public class Command_attributelist extends FCommand
{
    @Callback
    public void listAttributes(CommandSender sender)
    {
        final List<NamespacedKey> attributes = Registry.ATTRIBUTE
                                    .keyStream()
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

        msg(sender, message);
    }
}
