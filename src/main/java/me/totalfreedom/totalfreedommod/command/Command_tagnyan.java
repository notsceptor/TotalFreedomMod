package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
@CommandPermissions(level = Rank.OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.player.tagnyan")
@CommandParameters(description = "Gives you a tag with random colors", usage = "/<command> <tag>", aliases = "tn")
public class Command_tagnyan extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length < 1)
        {
            return false;
        }

        final String plainText = AdventureUtil.stripColor(StringUtils.join(args, " "));

        if (plainText.length() > Command_tag.MAX_TAG_LENGTH)
        {
            msg("That tag is too long (Max is " + Command_tag.MAX_TAG_LENGTH + " characters).");
            return true;
        }

        if (!plugin.al.isAdmin(sender) && Command_tag.containsForbidden(plainText))
        {
            msg("That tag contains a forbidden word.");
            return true;
        }

        final TextComponent.Builder builder = Component.text();

        for (char c : plainText.toCharArray())
        {
            builder.append(Component.text(c, FUtil.randomChatColor()));
        }

        final Component result = builder.build();

        final FPlayer data = plugin.pl.getPlayer(playerSender);
        data.setTag(AdventureUtil.componentToLegacy(result));

        msg(Component.text("Set tag to ").append(result));

        return true;
    }
}
