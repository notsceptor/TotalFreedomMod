package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.totalfreedommod.util.ChatMentionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.say")
@CommandParameters(description = "Broadcasts the given message as the console, includes sender name.", usage = "/<command> <message>")
public class Command_say extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length == 0)
        {
            return false;
        }

        Component formattedMessage = FUtil.colorizeWithLinks(StringUtils.join(args, " "), NamedTextColor.LIGHT_PURPLE);
        formattedMessage = ChatMentionUtil.highlightAndPing(plugin, formattedMessage, true);
        Component broadcast = Component.text("[Server:" + sender.getName() + "] ", NamedTextColor.LIGHT_PURPLE)
                .append(formattedMessage);
        FUtil.bcastMsg(broadcast);
        plugin.db.sendBroadcastMessage(sender.getName(),  PlainTextComponentSerializer.plainText().serialize(broadcast), ConfigEntry.DISCORD_SAY_MESSAGE);

        return true;
    }
}
