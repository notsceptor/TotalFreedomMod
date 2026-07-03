package me.totalfreedom.totalfreedommod.command;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@CommandPermissions(level = Rank.OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.player.enchant")
@CommandParameters(description = "Enchant items.", usage = "/<command> <list | addall | reset | add <enchantment> [level] | remove <enchantment>>")
public class Command_enchant extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "list")
    public boolean listPossibleEnchantments(CommandContext ctx)
    {
        final ItemStack item = ctx.getPlayerSender().getEquipment().getItemInMainHand();

        if (item.getType() == Material.AIR)
        {
            msg("You have to hold an item to enchant it.");
            return true;
        }

        final List<TextComponent> keys = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).stream()
                .filter(enchantment -> enchantment.canEnchantItem(item))
                .map(enchantment -> Component.text().append(enchantment.description())
                        .color(NamedTextColor.WHITE)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, ClickEvent.Payload.string("/" + ctx.getCommandLabel() + " add " + enchantment.key().asString() + " ")))
                        .hoverEvent(HoverEvent.showText(Component.text().append(enchantment.description())
                                .appendNewline()
                                .append(Component.text(enchantment.key().asString()).color(NamedTextColor.DARK_GRAY))))
                        .build())
                .toList();

        if (!keys.isEmpty())
        {
            msg("Possible enchantments for held item:");
            msg(Component.join(JoinConfiguration.commas(true), keys).color(NamedTextColor.GRAY));
        }
        else
        {
            msg("This item can't be enchanted.");
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "addall")
    public boolean addAllPossibleEnchantments(CommandContext ctx)
    {
        final ItemStack item = ctx.getPlayerSender().getEquipment().getItemInMainHand();

        RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).stream()
                .filter(enchantment -> enchantment.canEnchantItem(item))
                .forEach(enchantment -> item.addEnchantment(enchantment, enchantment.getMaxLevel()));

        msg(ctx.getSender(), "Added all possible enchantments for this item.");
        return true;
    }

    @CommandDispatchTarget(pattern = "reset")
    public boolean removeAllEnchantments(CommandContext ctx)
    {
        final ItemStack item = ctx.getPlayerSender().getEquipment().getItemInMainHand();

        item.removeEnchantments();

        msg(ctx.getSender(), "Removed all enchantments from this item.");
        return true;
    }

    @CommandDispatchTarget(pattern = "add <enchantment:Enchantment>")
    public boolean addEnchantment(CommandContext ctx, Enchantment enchantment)
    {
        return addEnchantment(ctx, enchantment, enchantment.getMaxLevel());
    }

    @CommandDispatchTarget(pattern = "add <enchantment:Enchantment> <level>")
    public boolean addEnchantment(CommandContext ctx, Enchantment enchantment, int level)
    {
        final ItemStack item = ctx.getPlayerSender().getEquipment().getItemInMainHand();

        if (item.getType() == Material.AIR)
        {
            msg(ctx.getSender(), "You have to hold an item to enchant it.");
            return true;
        }

        if (enchantment.canEnchantItem(item))
        {
            item.addEnchantment(enchantment, level);
            msg(ctx.getSender(), Component.text("Added enchantment ")
                    .append(enchantment.description())
                    .append(Component.text(" to this item.")).color(NamedTextColor.GRAY));
        }
        else
        {
            msg(ctx.getSender(), "This enchantment can't be applied to the item you're holding.");
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "remove <enchantment:Enchantment>")
    public boolean removeEnchantment(CommandContext ctx, Enchantment enchantment)
    {
        final ItemStack item = ctx.getPlayerSender().getEquipment().getItemInMainHand();

        item.removeEnchantment(enchantment);
        msg(Component.text("Removed enchantment ")
                .append(enchantment.description())
                .append(Component.text(" from this item.")).color(NamedTextColor.GRAY));

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
