package me.totalfreedom.totalfreedommod.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.fun.cookie")
@CommandParameters(description = "For those who have no friends.", usage = "/<command>")
public class Command_cookie extends FreedomCommand
{

    public static final String COOKIE_LYRICS = "Imagine that you have zero cookies and you split them evenly among zero friends. How many cookies does each person get? See? It doesn't make sense. And Cookie Monster is sad that there are no cookies, and you are sad that you have no friends.";
    public static final String LORE = "But, you can have a cookie anyways,\nsince you are sad you are have no friends.";

    private static final Random RANDOM = new Random();

    private static final NamedTextColor[] COLORS = new NamedTextColor[]
            {
                    NamedTextColor.DARK_RED,
                    NamedTextColor.RED,
                    NamedTextColor.GOLD,
                    NamedTextColor.YELLOW,
                    NamedTextColor.DARK_GREEN,
                    NamedTextColor.GREEN,
                    NamedTextColor.AQUA,
                    NamedTextColor.DARK_AQUA,
                    NamedTextColor.DARK_BLUE,
                    NamedTextColor.BLUE,
                    NamedTextColor.DARK_PURPLE,
                    NamedTextColor.LIGHT_PURPLE
            };

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        final ItemStack heldItem = new ItemStack(Material.COOKIE);
        final ItemMeta heldItemMeta = heldItem.getItemMeta();

        if (heldItemMeta == null)
        {
            msg(Component.text("Unable to create the cookie.", NamedTextColor.RED));
            return true;
        }

        final Component name = Component.empty()
                .append(Component.text("C", NamedTextColor.DARK_RED))
                .append(Component.text("o", NamedTextColor.GOLD))
                .append(Component.text("o", NamedTextColor.YELLOW))
                .append(Component.text("k", NamedTextColor.DARK_GREEN))
                .append(Component.text("i", NamedTextColor.DARK_BLUE))
                .append(Component.text("e", NamedTextColor.DARK_PURPLE));

        final List<Component> lore = new ArrayList<>();

        for (final String line : LORE.split("\n"))
        {
            lore.add(colorizeWords(line));
        }

        heldItemMeta.displayName(name);
        heldItemMeta.lore(lore);
        heldItem.setItemMeta(heldItemMeta);

        for (final Player player : server.getOnlinePlayers())
        {
            final int firstEmpty = player.getInventory().firstEmpty();

            if (firstEmpty >= 0)
            {
                player.getInventory().setItem(firstEmpty, heldItem.clone());
            }
        }

        FUtil.bcastMsg(colorizeWords(COOKIE_LYRICS));

        return true;
    }

    private Component colorizeWords(String text)
    {
        final String[] words = text.split(" ");
        Component output = Component.empty();

        for (int i = 0; i < words.length; i++)
        {
            output = output.append(Component.text(words[i], randomColor()));

            if (i < words.length - 1)
            {
                output = output.append(Component.space());
            }
        }

        return output;
    }

    private NamedTextColor randomColor()
    {
        return COLORS[RANDOM.nextInt(COLORS.length)];
    }
}
