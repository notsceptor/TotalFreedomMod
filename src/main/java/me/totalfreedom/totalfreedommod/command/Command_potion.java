package me.totalfreedom.totalfreedommod.command;

import java.util.List;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.player.potion")
@CommandParameters(
        description = "Manipulate potion effects. Duration is measured in server ticks (~20 ticks per second).",
        usage = "/<command> <list | clear [target name] | clearall | add <type> <duration> <amplifier> [target name] | remove <type> [target name]>")
public class Command_potion extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "list")
    public boolean listEffects(CommandContext ctx)
    {
        final List<TextComponent> effects = Registry.POTION_EFFECT_TYPE.stream()
                .map(type -> Component.text().append(ctx.isSenderConsole() ?
                                Component.text(type.key().asString(), NamedTextColor.WHITE) :
                                Component.translatable(type.translationKey(), NamedTextColor.WHITE)
                                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, ClickEvent.Payload.string("/" + ctx.getCommandLabel() + " add " + type.getKey())))
                        .hoverEvent(HoverEvent.showText(Component.translatable(type.translationKey()).appendNewline().append(Component.text(type.key().asString(), NamedTextColor.DARK_GRAY))))).build())
                .toList();

        msg(ctx.getSender(), "Potion effect types:");
        msg(ctx.getSender(), Component.join(JoinConfiguration.commas(true), effects).color(NamedTextColor.GRAY));
        return true;
    }

    @CommandDispatchTarget(pattern = "clearall")
    public boolean clearAll(CommandContext ctx)
    {
        if (!plugin.al.isAdmin(ctx.getSender()) && !ctx.isSenderConsole())
        {
            return clearSelf(ctx);
        }

        FUtil.adminAction(ctx.getSender().getName(), "Clearing all potion effects from all players", true);
        server.getOnlinePlayers().forEach(LivingEntity::clearActivePotionEffects);
        return true;
    }

    @CommandDispatchTarget(pattern = "clear")
    public boolean clearSelf(CommandContext ctx)
    {
        if (ctx.isSenderConsole())
        {
            msg(ctx.getSender(), "You must specify a player when using this command from the console.");
            return true;
        }

        return clearPlayer(ctx, ctx.getPlayerSender());
    }

    @CommandDispatchTarget(pattern = "clear <player:Player>")
    public boolean clearPlayer(CommandContext ctx, Player player)
    {
        if (!plugin.al.isAdmin(playerSender) && (!ctx.isSenderConsole() && !playerSender.equals(player)))
        {
            msg(ctx.getSender(), Component.text("You don't have permission to clear effects from other players.", NamedTextColor.RED));
            return true;
        }

        player.clearActivePotionEffects();
        msg(ctx.getSender(), "Cleared all active potion effects from player " + player.getName() + ".", NamedTextColor.AQUA);
        return true;
    }

    @CommandDispatchTarget(pattern = "add <type:PotionEffectType> <duration:Integer> <amplifier:Integer>")
    public boolean addEffectToSelf(CommandContext ctx, PotionEffectType type, Integer duration, Integer amplifier)
    {
        if (ctx.isSenderConsole())
        {
            msg(ctx.getSender(), "You must specify a player when using this command from the console.");
            return true;
        }

        return addEffectToPlayer(ctx, type, duration, amplifier, ctx.getPlayerSender());
    }

    @CommandDispatchTarget(pattern = "add <type:PotionEffectType> <duration:Integer> <amplifier:Integer> <player:Player>")
    public boolean addEffectToPlayer(CommandContext ctx, PotionEffectType type, Integer duration, Integer amplifier, Player player)
    {
        // If they're trying to give it to another person, and they don't have permission, let's give it to them instead
        if (!plugin.al.isAdmin(ctx.getSender()) && (!ctx.isSenderConsole() && !ctx.getPlayerSender().equals(player)))
        {
            return addEffectToPlayer(ctx, type, duration, amplifier, ctx.getPlayerSender());
        }

        int clampedDuration = Math.min(duration, 100_000);
        int clampedAmplifier = Math.min(amplifier, 100_000);

        player.addPotionEffect(type.createEffect(clampedDuration, clampedAmplifier));
        msg(ctx.getSender(), Component.text("Added potion effect ", NamedTextColor.AQUA)
                .append(Component.translatable(type.translationKey()))
                .append(Component.text(", Duration: ").append(Component.text(clampedDuration)))
                .append(Component.text(", Amplifier: ").append(Component.text(clampedAmplifier)))
                .append(Component.text(" to player ").append(Component.text(player.getName())))
                .append(Component.text(".")));
        return true;
    }

    @CommandDispatchTarget(pattern = "remove <type:PotionEffectType>")
    public boolean removeEffectFromSelf(CommandContext ctx, PotionEffectType type)
    {
        if (ctx.isSenderConsole())
        {
            msg(ctx.getSender(), "You must specify a player when using this command from the console.");
            return true;
        }

        return removeEffectFromPlayer(ctx, type, ctx.getPlayerSender());
    }

    @CommandDispatchTarget(pattern = "remove <type:PotionEffectType> <player:Player>")
    public boolean removeEffectFromPlayer(CommandContext ctx, PotionEffectType type, Player player)
    {
        if (!plugin.al.isAdmin(ctx.getSender()) && (!ctx.isSenderConsole() && !ctx.getPlayerSender().equals(player)))
        {
            return removeEffectFromPlayer(ctx, type, ctx.getPlayerSender());
        }

        player.removePotionEffect(type);
        msg(ctx.getSender(), Component.text("Removed potion effect ", NamedTextColor.AQUA)
                .append(Component.translatable(type.translationKey()))
                .append(Component.text(" from player ").append(Component.text(player.getName())))
                .append(Component.text(".")));

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
