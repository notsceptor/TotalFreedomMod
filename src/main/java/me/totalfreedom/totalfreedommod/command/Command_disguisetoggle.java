package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.command.Command;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.disguisetoggle")
@CommandParameters(description = "Toggle the disguise plugin", usage = "/<command>", aliases = "dtoggle")
public class Command_disguisetoggle extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (!plugin.ldb.isPluginEnabled())
        {
            msg(ChatColor.RED + "LibsDisguises is not enabled.");
            return true;
        }

        boolean currentlyEnabled = plugin.ldb.isDisguisesEnabled();
        String action = currentlyEnabled ? "Disabling" : "Enabling";

        FUtil.adminAction(sender.getName(), action + " Disguises", false);

        if (currentlyEnabled)
        {
            plugin.ldb.undisguiseAll(true);
            plugin.ldb.setDisguisesEnabled(false);
        }
        else
        {
            plugin.ldb.setDisguisesEnabled(true);
        }

        msg("Disguises are now " + (plugin.ldb.isDisguisesEnabled() ? "enabled." : "disabled."));

        return true;
    }
}
