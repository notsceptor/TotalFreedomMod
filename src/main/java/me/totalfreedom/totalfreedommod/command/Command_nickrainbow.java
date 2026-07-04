package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.player.nickrainbow")
@CommandParameters(description = "Rainbowify your nickname.", usage = "/<command> <nick>")
public class Command_nickrainbow extends FreedomCommand
{

    private static final NamedTextColor[] RAINBOW_COLORS = new NamedTextColor[]
            {
                    NamedTextColor.RED,
                    NamedTextColor.GOLD,
                    NamedTextColor.YELLOW,
                    NamedTextColor.GREEN,
                    NamedTextColor.AQUA,
                    NamedTextColor.BLUE,
                    NamedTextColor.LIGHT_PURPLE
            };

    @CommandDispatchTarget(pattern = "<nickname>")
    public boolean nickRainbow(CommandContext ctx, String nickname)
    {
        final String nickPlain = StringUtils.strip(nickname).replace("§", "").replace("&", "");

        if (!AdventureUtil.hasVisibleText(nickPlain))
        {
            msg(Component.text("You must provide a nickname.", NamedTextColor.RED));
            return true;
        }

        if (!nickPlain.matches("^[a-zA-Z0-9_]+$"))
        {
            msg(Component.text("That nickname contains invalid characters.", NamedTextColor.RED));
            return true;
        }

        if (nickPlain.length() < 3 || nickPlain.length() > Command_nickname.MAX_NICKNAME_LENGTH)
        {
            msg(Component.text(
                    "Your nickname must be between 3 and " + Command_nickname.MAX_NICKNAME_LENGTH + " characters long.",
                    NamedTextColor.RED));
            return true;
        }

        if (!plugin.al.isAdmin(sender) && Command_nickname.containsForbidden(nickPlain))
        {
            msg(Component.text("That nickname contains a forbidden word.", NamedTextColor.RED));
            return true;
        }

        for (Player player : server.getOnlinePlayers())
        {
            if (player == playerSender)
            {
                continue;
            }

            final PlayerData playerData = plugin.pl.getData(player);
            final Component playerNickname = playerData.getNickname();

            if (player.getName().equalsIgnoreCase(nickPlain))
            {
                msg(Component.text("That nickname is already in use.", NamedTextColor.RED));
                return true;
            }

            if (playerNickname != null
                    && AdventureUtil.componentToPlainText(playerNickname).trim().equalsIgnoreCase(nickPlain))
            {
                msg(Component.text("That nickname is already in use.", NamedTextColor.RED));
                return true;
            }
        }

        final Component rainbowNickname = rainbowify(nickPlain);
        final PlayerData data = plugin.pl.getData(playerSender);

        data.setNickname(rainbowNickname);

        msg(Component.text("Your nickname is now: ", NamedTextColor.GRAY)
                .append(data.getDisplayedNickname()));

        return true;
    }

    private Component rainbowify(String text)
    {
        Component result = Component.empty();
        final int[] characters = text.codePoints().toArray();

        for (int i = 0; i < characters.length; i++)
        {
            final NamedTextColor color = RAINBOW_COLORS[i % RAINBOW_COLORS.length];

            result = result.append(Component.text(
                    new String(Character.toChars(characters[i])),
                    color));
        }

        return result;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
