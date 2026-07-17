package me.totalfreedom.totalfreedommod.command;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.totalfreedommod.world.AdminWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.ro")
@CommandParameters(description = "Remove all blocks of a certain type in the radius of certain players.", usage = "/<command> <blocks> [radius] [players]")
public class Command_ro extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<blocks:MaterialQuery:mode=blocks,limit=24L,nonEmpty>")
    public boolean allPlayersDefault(CommandContext ctx, List<Material> blocks)
    {
        return allPlayersWithRadius(ctx, blocks, 50);
    }

    @CommandDispatchTarget(pattern = "<blocks:MaterialQuery:mode=blocks,limit=24L,nonEmpty> <radius:Integer>")
    public boolean allPlayersWithRadius(CommandContext ctx, List<Material> blocks, Integer radius)
    {
        return removeNear(ctx, blocks, radius, server.getOnlinePlayers().stream()
                .filter(player -> !player.getWorld().getName().equalsIgnoreCase(plugin.wm.adminworld.getName()))
                .map(player -> (Player) player)
                .toList());
    }

    @CommandDispatchTarget(pattern = "<blocks:MaterialQuery:mode=blocks,limit=24L,nonEmpty> <radius:Integer> <players:PlayerList>")
    public boolean removeNear(CommandContext ctx, List<Material> blocks, Integer radius, List<Player> players)
    {
        if (blocks.isEmpty())
        {
            msg(ctx.getSender(), "No blocks could be found matching your query.");
            return true;
        }

        radius = Math.clamp(radius, 1, 100);

        FUtil.adminAction(ctx.getSender().getName(), buildRemovingMessage(blocks, radius, server.getOnlinePlayers()),
                NamedTextColor.AQUA);

        final AtomicInteger affected = new AtomicInteger(0);
        final Integer finalRadius = radius;

        players.stream()
                .map(Entity::getLocation)
                .flatMap(location -> blocks.stream().map(block -> replaceBlocks(location, block, Material.AIR, finalRadius)))
                .forEach(affected::addAndGet);

        msg(ctx.getSender(), "Remove complete. " + affected.get() + " blocks were removed.");
        return true;
    }

    private Component buildRemovingMessage(List<Material> blocks, int radius, Collection<? extends Player> players)
    {
        return Component.text("Removing all blocks of ")
                .append(blocks.size() > 4 ?
                        Component.text(blocks.size()).append(Component.text(" type(s)")) :
                        Component.join(JoinConfiguration.commas(true),
                        blocks.stream().map(block -> Component.text(block.name())).toList()))
                .append(Component.text(" within ").append(Component.text(radius)))
                .append(Component.text(" blocks of "))
                .append(players.size() == server.getOnlinePlayers().size() ?
                        Component.text("all players... Brace for lag!") :
                        players.size() > 4 ?
                                Component.text(players.size()).append(Component.text(" players")) :
                                Component.join(JoinConfiguration.commas(true),
                                        players.stream().map(player -> Component.text(player.getName())).toList()));
    }

    public static int replaceBlocks(Location center, Material fromMaterial, Material toMaterial, int radius)
    {
        int affected = 0;

        Block centerBlock = center.getBlock();
        for (int xOffset = -radius; xOffset <= radius; xOffset++)
        {
            for (int yOffset = -radius; yOffset <= radius; yOffset++)
            {
                for (int zOffset = -radius; zOffset <= radius; zOffset++)
                {
                    Block block = centerBlock.getRelative(xOffset, yOffset, zOffset);

                    if (block.getType().equals(fromMaterial))
                    {
                        if (block.getLocation().distanceSquared(center) < (radius * radius))
                        {
                            block.setType(toMaterial);
                            affected++;
                        }
                    }
                }
            }
        }

        return affected;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
