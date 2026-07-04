package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

@CommandPermissions(level = Rank.OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.fun.spawnmob")
@CommandParameters(description = "Make an announcement", usage = "/<command> <mobtype> [amount]")
public class Command_spawnmob extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<type:EntityType:mobs>")
    public boolean spawnSingle(CommandContext ctx, EntityType type)
    {
        return spawnAmount(ctx, type, 1);
    }

    @CommandDispatchTarget(pattern = "<type:EntityType:mobs> <amount:Integer>")
    public boolean spawnAmount(CommandContext ctx, EntityType type, Integer amount)
    {
        amount = Math.clamp(amount, 1, 10);

        msg(ctx.getPlayerSender(), Component.text("Spawning ", NamedTextColor.GRAY).append(Component.text(amount))
                .append(Component.text(" of type ").append(ctx.isSenderConsole() ? Component.text(type.name()) :
                        Component.translatable(type.translationKey()))));

        final Location playerLoc = ctx.getPlayerSender().getLocation();

        for (int i = 0; i < amount; i++)
        {
            playerLoc.getWorld().spawnEntity(playerLoc, type, CreatureSpawnEvent.SpawnReason.COMMAND);
        }

        return true;
    }

    @Override
    protected boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }

}
