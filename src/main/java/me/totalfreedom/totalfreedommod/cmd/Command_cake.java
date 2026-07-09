package me.totalfreedom.totalfreedommod.cmd;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Command(name = "cake", description = "For the people that are still alive.", usage = "/cake")
@Permission(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.fun.cake")
public class Command_cake extends FCommand 
{
    public static final String CAKE_LYRICS = "But there's no sense crying over every mistake. You just keep on trying till you run out of cake.";
    private final Random random = new Random();

    @Callback
    public void cake(CommandSender sender) 
    {
        final StringBuilder legacy = new StringBuilder();
        for (final String word : CAKE_LYRICS.split(" "))
        {
            legacy.append('&').append(Integer.toHexString(1 + random.nextInt(14))).append(word).append(' ');
        }

        final ItemStack heldItem = new ItemStack(Material.CAKE);
        final ItemMeta heldItemMeta = heldItem.getItemMeta();
        heldItemMeta.displayName(Component.text("The ", NamedTextColor.WHITE)
                .append(Component.text("Lie", NamedTextColor.DARK_GRAY)));
        heldItem.setItemMeta(heldItemMeta);

        for (final Player player : server.getOnlinePlayers())
        {
            final int firstEmpty = player.getInventory().firstEmpty();
            if (firstEmpty >= 0)
            {
                player.getInventory().setItem(firstEmpty, heldItem);
            }
            
        }

        FUtil.bcastMsg(AdventureUtil.legacyToComponent(legacy.toString()));
    }

}
