package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.announce")
@CommandParameters(description = "Make an announcement", usage = "/<command> <message>")
public class Command_announce extends FreedomCommand
{

    @CommandDispatchTarget(pattern = "<message..>")
    public boolean makeAnnouncement(CommandContext ctx, String greedyContent)
    {
        plugin.an.announce(greedyContent);
        return true;
    }


    @Override
    protected boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }

}
