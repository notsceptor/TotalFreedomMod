package me.totalfreedom.totalfreedommod.command;

import java.util.ArrayList;
import java.util.List;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.nickclean")
@CommandParameters(description = "Remove distracting formatting from the nicknames of all online players.", usage = "/<command>", aliases = "nc")
public class Command_nickclean extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        FUtil.adminAction(sender.getName(), "Cleaning all nicknames", false);

        int cleaned = 0;

        for (final Player player : server.getOnlinePlayers())
        {
            final PlayerData data = plugin.pl.getData(player);
            final Component nickname = data.getNickname();

            if (nickname == null || !data.hasCustomNickname())
            {
                continue;
            }

            final Component cleanedNickname = cleanComponent(nickname);

            if (nickname.equals(cleanedNickname))
            {
                continue;
            }

            msg(Component.text(player.getName() + ": \"", NamedTextColor.GRAY)
                    .append(nickname)
                    .append(Component.text("\" -> \"", NamedTextColor.GRAY))
                    .append(cleanedNickname)
                    .append(Component.text("\".", NamedTextColor.GRAY)));

            data.setNickname(cleanedNickname);
            cleaned++;
        }

        msg(Component.text(
                cleaned + " nickname" + (cleaned == 1 ? " was" : "s were") + " cleaned.",
                NamedTextColor.GRAY));

        return true;
    }

    private Component cleanComponent(Component component)
    {
        Style style = component.style()
                .decoration(TextDecoration.OBFUSCATED, TextDecoration.State.FALSE)
                .decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                .decoration(TextDecoration.UNDERLINED, TextDecoration.State.FALSE);

        if (NamedTextColor.BLACK.equals(style.color()))
        {
            style = style.color(null);
        }

        final List<Component> cleanedChildren = new ArrayList<>();

        for (Component child : component.children())
        {
            cleanedChildren.add(cleanComponent(child));
        }

        return component.style(style).children(cleanedChildren);
    }
}
