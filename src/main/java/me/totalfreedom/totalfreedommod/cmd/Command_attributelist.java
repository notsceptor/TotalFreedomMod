package me.totalfreedom.totalfreedommod.cmd;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;

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

        String message = attributes.stream()
            .map(attr -> attr.getKey().toUpperCase(Locale.ROOT))
            .map(name -> String.format("<yellow>%s", name))
            .collect(Collectors.joining("<gray>, "));

        msg(sender, "<gold>All possible attributes: %s", message);
    }
}
