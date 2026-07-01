package me.totalfreedom.totalfreedommod.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.nickclean")
@CommandParameters(description = "Essentials Interface Command - Remove distracting things from nicknames of all players on server.", usage = "/<command>", aliases = "nc")
public class Command_nickclean extends FreedomCommand
{

    private static final ChatColor[] BLOCKED = new ChatColor[]
    {
        ChatColor.MAGIC,
        ChatColor.STRIKETHROUGH,
        ChatColor.ITALIC,
        ChatColor.UNDERLINE,
        ChatColor.BLACK
    };
    private static final Pattern REGEX = Pattern.compile(ChatColor.COLOR_CHAR + "[" + StringUtils.join(BLOCKED, "") + "]", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        FUtil.adminAction(sender.getName(), "Cleaning all nicknames", false);

        for (final Player player : server.getOnlinePlayers())
        {
            final String playerName = player.getName();
            final PlayerData data = plugin.pl.getData(player);
            final Component nickName = data.getNickname();
            final String legacyNickname = AdventureUtil.componentToLegacy(nickName);
            if (nickName != null && !legacyNickname.isEmpty() && !legacyNickname.equalsIgnoreCase(playerName))
            {
                final Matcher matcher = REGEX.matcher(legacyNickname);
                if (matcher.find())
                {
                    final String newNickName = matcher.replaceAll("");
                    msg(ChatColor.RESET + playerName + ": \"" + nickName + ChatColor.RESET + "\" -> \"" + newNickName + ChatColor.RESET + "\".");
                    data.setNickname(AdventureUtil.legacyToComponent(newNickName));
                }
            }
        }

        return true;
    }
}
