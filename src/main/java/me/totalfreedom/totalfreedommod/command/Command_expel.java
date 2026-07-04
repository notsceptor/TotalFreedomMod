package me.totalfreedom.totalfreedommod.command;

import java.util.List;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.fun.expel")
@CommandParameters(description = "Push people away from you.", usage = "/<command> [radius] [strength]")
public class Command_expel extends FreedomCommand
{
    @CommandDispatchTarget
    public boolean expel(CommandContext ctx)
    {
        return expelWithRadius(ctx, 20.0, 5.0);
    }

    @CommandDispatchTarget(pattern = "<radius:Double> <strength:Double>")
    public boolean expelWithRadius(CommandContext ctx, Double radius, Double strength)
    {
        radius = Math.clamp(radius, 1.0, 100.0);
        strength = Math.clamp(strength, 1.0, 100.0);

        final Vector senderPos = playerSender.getLocation().toVector();
        final double finalStrength = strength;

        final List<Component> sent = ctx.getPlayerSender().getWorld().getNearbyPlayers(ctx.getPlayerSender().getLocation(), radius)
                .stream().filter(player -> !player.equals(ctx.getPlayerSender())).map(player ->
        {
            final Location targetPos = player.getLocation();
            final Vector targetPosVec = targetPos.toVector();

            player.getWorld().createExplosion(targetPos, 0.0f, false);
            FUtil.setFlying(player, false);
            player.setVelocity(targetPosVec.subtract(senderPos).normalize().multiply(finalStrength));

            return player.displayName()
                    .colorIfAbsent(NamedTextColor.WHITE)
                    .hoverEvent(HoverEvent.showText(Component.text(player.getName())));
        }).toList();

        if (sent.isEmpty())
        {
            msg(ctx.getSender(), "No players pushed.");
        }
        else
        {
            msg(ctx.getSender(), Component.text("Pushed ", NamedTextColor.GRAY)
                    .append(Component.text(sent.size()))
                    .append(Component.text(" players: "))
                    .append(Component.join(JoinConfiguration.commas(true), sent)));
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
