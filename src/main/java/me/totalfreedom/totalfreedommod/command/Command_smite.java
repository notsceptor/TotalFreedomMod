package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.fun.smite")
@CommandParameters(description = "Someone being a little bitch? Smite them down...", usage = "/<command> <player> [reason]")
public class Command_smite extends FreedomCommand
{

    @CommandDispatchTarget(pattern = "<player:Player> <reason..>")
    public boolean smite(CommandContext ctx, Player player, String reason)
    {
        if (plugin.al.isAdmin(player))
        {
            msg(ctx.getSender(), "This command cannot be used on other admins.");
            return true;
        }
        
        FUtil.bcastMsg(player.getName() + " has been a naughty, naughty boy.", NamedTextColor.RED);

        if (reason != null)
        {
            FUtil.bcastMsg("  Reason: " + reason, NamedTextColor.YELLOW);
        }

        plugin.db.sendActionMessage(sender.getName(), player.getName(), reason, ConfigEntry.DISCORD_PLAYER_SMITE_MESSAGE);

        // Deop
        player.setOp(false);

        // Set gamemode to survival
        player.setGameMode(GameMode.SURVIVAL);

        // Clear inventory
        player.getInventory().clear();

        // Strike with lightning effect
        final Location targetPos = player.getLocation();
        final World world = player.getWorld();
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                final Location strike_pos = new Location(world, targetPos.getBlockX() + x, targetPos.getBlockY(), targetPos.getBlockZ() + z);
                world.strikeLightning(strike_pos);
            }
        }

        // Kill
        player.setHealth(0.0);

        if (reason != null)
        {
            player.sendMessage(Component.text("You've been smitten. Reason: ", NamedTextColor.RED)
                    .append(FUtil.colorizeWithLinks(reason, NamedTextColor.YELLOW)));
        }
        return true;
    }

    @CommandDispatchTarget(pattern = "<player:Player>")
    public boolean smiteNoReason(CommandContext ctx, Player player)
    {
        return smite(ctx, player, null);
    }


    @Override
    protected boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}