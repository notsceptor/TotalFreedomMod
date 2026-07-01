package me.totalfreedom.totalfreedommod.command;

import java.util.List;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "")
@CommandParameters(description = "Spawns the inspector stick to query block history.", usage = "/<command>")
public class Command_logstick extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (!sender.hasPermission("customplugin.logstick"))
        {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        final ItemStack stick = new ItemStack(Material.STICK);
        final ItemMeta meta = stick.getItemMeta();
        if (meta != null)
        {
            meta.displayName(AdventureUtil.translateAlternateColorCodes('§', "§b§lLogstick"));
            meta.lore(List.of(
                AdventureUtil.translateAlternateColorCodes('§', "§7Right-click a block to inspect its history.")
            ));
            final NamespacedKey key = new NamespacedKey(plugin, "logstick");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            stick.setItemMeta(meta);
        }

        playerSender.getInventory().addItem(stick);
        playerSender.sendMessage(Component.text("You have been given the Logstick!", NamedTextColor.GREEN));

        return true;
    }
}
