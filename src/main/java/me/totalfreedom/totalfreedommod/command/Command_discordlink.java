package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.admin.discordlink")
@CommandParameters(description = "Generate a one-time code for admins to link their Discord account.", usage = "/<command>")
public class Command_discordlink extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (plugin.db == null || !plugin.db.isReady())
        {
            msg("Discord bridge is not enabled or not ready.", NamedTextColor.RED);
            return true;
        }

        Admin admin = plugin.al.getAdmin(playerSender);
        if (admin == null)
        {
            msg("You are not in the admin list.", NamedTextColor.RED);
            return true;
        }

        String code = plugin.db.createPendingLink(admin.getUuid());
        int ttlSeconds = plugin.db.getLinkCodeTtlSeconds();

        msg("Your Discord link code is:", NamedTextColor.GREEN);
        msg(code, NamedTextColor.YELLOW);
        msg("On the Discord server, you may run /link " + code + " to link your Discord account.",
                NamedTextColor.GRAY);
        msg("Code expires in " + ttlSeconds + " seconds.", NamedTextColor.GRAY);
        return true;
    }
}
