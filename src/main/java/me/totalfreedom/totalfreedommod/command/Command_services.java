package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.ServiceChecker.ServiceCheckCallback;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.server.services")
@CommandParameters(description = "Check Mojang/Microsoft service status.", usage = "/<command>")
public class Command_services extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        msg("Checking Mojang/Microsoft services...", ChatColor.YELLOW);

        plugin.sc.checkServicesAsync(new ServiceCheckCallback()
        {
            @Override
            public void onResult(String status, boolean success)
            {
                if (success)
                {
                    msg("Service Status:", ChatColor.GREEN);
                    msg(status, ChatColor.WHITE);
                }
                else
                {
                    msg(status, ChatColor.RED);
                }
            }
        });

        return true;
    }
}

