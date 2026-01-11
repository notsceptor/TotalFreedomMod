package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.fun.birthday")
@CommandParameters(description = "Celebrate a player's birthday", usage = "/<command> <playername>")
public class Command_birthday extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length != 1)
        {
            return false;
        }

        Player player = getPlayer(args[0]);
        if (player == null)
        {
            msg(PLAYER_NOT_FOUND);
            return true;
        }

        // Smite the birthday player
        Command_smite.smite(player, "Happy Birthday!");

        // Give cake to all players and award advancements
        for (Player onlinePlayer : server.getOnlinePlayers())
        {
            onlinePlayer.getInventory().addItem(new ItemStack(Material.CAKE, 1));

            // Award advancements (modern API - Minecraft 1.13+)
            try
            {
                // Try to award balanced diet advancement (includes cake)
                Advancement balancedDiet = server.getAdvancement(NamespacedKey.minecraft("husbandry/balanced_diet"));
                if (balancedDiet != null)
                {
                    AdvancementProgress progress = onlinePlayer.getAdvancementProgress(balancedDiet);
                    if (!progress.isDone())
                    {
                        // Award the cake criteria if not already done
                        progress.awardCriteria("cake");
                    }
                }
            }
            catch (Exception ignored)
            {
            }
        }

        FUtil.bcastMsg(player.getName() + " Happy Birthday!", ChatColor.AQUA);
        return true;
    }
}

