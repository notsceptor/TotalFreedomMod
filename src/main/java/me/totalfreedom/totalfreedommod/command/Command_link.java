package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.admin.discordlink")
@CommandParameters(description = "Generate a one-time code for admins to link their Discord account.", usage = "/<command>")
public class Command_link extends FreedomCommand
{

    @CommandDispatchTarget
    public boolean createDiscordLink(CommandContext ctx)
    {
        if (plugin.db == null || !plugin.db.isReady())
        {
            msg(ctx.getSender(), "Discord bridge is not enabled or not ready.", NamedTextColor.RED);
            return true;
        }

        final Admin admin = plugin.al.getAdmin(ctx.getSender());
        if (admin == null)
        {
            msg(ctx.getSender(), "You're not in the admin list.", NamedTextColor.RED);
            return true;
        }

        final String code = plugin.db.createPendingLink(admin.getUuid());
        final int ttlSeconds = plugin.db.getLinkCodeTtlSeconds();

        msg(ctx.getSender(), "Your Discord link code is:", NamedTextColor.GREEN);
        msg(ctx.getSender(), code, NamedTextColor.YELLOW);
        msg(ctx.getSender(), "On the Discord server, you may run /link " + code + " to link your Discord account.",
                NamedTextColor.GRAY);
        msg(ctx.getSender(), "Code expires in " + ttlSeconds + " seconds.", NamedTextColor.GRAY);

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
