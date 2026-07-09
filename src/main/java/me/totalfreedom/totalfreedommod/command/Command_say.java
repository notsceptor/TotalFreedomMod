package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.totalfreedommod.util.ChatMentionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.say")
@CommandParameters(description = "Broadcasts the given message as the console, includes sender name.", usage = "/<command> <message>")
public class Command_say extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<message..>")
    public boolean broadcastMessage(CommandContext ctx, String message)
    {
        final Component broadcast = Component.text("[Server:", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(ctx.getSender().getName()))
                .append(Component.text("] "))
                .append(ChatMentionUtil.highlightAndPing(plugin, FUtil.colorizeWithLinks(message, NamedTextColor.LIGHT_PURPLE), true));

        FUtil.bcastMsg(broadcast);
        plugin.db.sendBroadcastMessage(ctx.getSender().getName(), AdventureUtil.componentToPlainText(broadcast), ConfigEntry.DISCORD_SAY_MESSAGE);
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
