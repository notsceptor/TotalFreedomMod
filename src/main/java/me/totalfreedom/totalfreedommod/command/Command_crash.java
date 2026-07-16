package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.fuckup")
@CommandParameters(description = "Crashes the specified player", usage = "/<command> <player>", aliases = "fuckup")
public class Command_crash extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<player:Player>")
    public boolean crash(CommandContext ctx, Player player)
    {
        if (plugin.al.isAdmin(player))
        {
            msg(ctx.getSender(), "This command cannot be used on other admins.");
            return true;
        }
        player.spawnParticle(
                Particle.ASH,
                player.getLocation(),
                1_000_000_000,
                0.0,
                0.0,
                0.0,
                1.0,
                null,
                true
        );

        Component c = Component.text("GET ABSOLUTELY FUCKED!");
        for (int i = 0; i < 50; ++i)
            c = Component.translatable("%1$s%1$s%1$s", "%1$s%1$s%1$s", c);

        player.sendMessage(c);

        sender.sendMessage(
                Component.text("Crashed ", NamedTextColor.GRAY)
                        .append(Component.text(player.getName(), NamedTextColor.GRAY))
                        .append(Component.text(".", NamedTextColor.GRAY))
        );

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}