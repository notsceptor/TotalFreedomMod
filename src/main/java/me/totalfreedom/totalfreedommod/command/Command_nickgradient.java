 package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.player.nickgradient")
@CommandParameters(description = "Sets your nickname to a hex color gradient.", usage = "/<command> <hex> <hex> <nick>")
public class Command_nickgradient extends FreedomCommand
{

    @CommandDispatchTarget(pattern = "<startHex> <endHex> <nickname>")
    public boolean nickGradient(CommandContext ctx, String startHex, String endHex, String nickname)
    {
        final Player player = ctx.getPlayerSender();
        final String plainNickname = nickname.trim();

        if (!isValidHex(startHex) || !isValidHex(endHex))
        {
            msg("Both colors must be valid six-digit hex colors. Example: #FF0000");
            return true;
        }

        if (!plainNickname.matches("^[a-zA-Z0-9_]+$"))
        {
            msg("That nickname contains invalid characters.");
            return true;
        }

        if (plainNickname.length() < 4 || plainNickname.length() > Command_nickname.MAX_NICKNAME_LENGTH)
        {
            msg("Your nickname must be between 4 and " + Command_nickname.MAX_NICKNAME_LENGTH + " characters long.");
            return true;
        }

        if (!plugin.al.isAdmin(sender) && Command_nickname.containsForbidden(plainNickname))
        {
            msg("That nickname contains a forbidden word.");
            return true;
        }

        for (Player otherPlayer : server.getOnlinePlayers())
        {
            if (otherPlayer == player)
            {
                continue;
            }

            final PlayerData otherData = plugin.pl.getData(otherPlayer);
            final String otherNickname = otherData.getNickname() == null
                    ? otherPlayer.getName()
                    : AdventureUtil.componentToPlainText(otherData.getNickname()).trim();

            if (otherPlayer.getName().equalsIgnoreCase(plainNickname)
                    || otherNickname.equalsIgnoreCase(plainNickname))
            {
                msg("That nickname is already in use.");
                return true;
            }
        }

        final int startColor = parseHex(startHex);
        final int endColor = parseHex(endHex);
        final Component gradientNickname = createGradient(plainNickname, startColor, endColor);
        final PlayerData data = plugin.pl.getData(player);

        data.setNickname(gradientNickname);

        msg(Component.text("Your nickname is now: ")
                .append(gradientNickname));

        return true;
    }

    private static boolean isValidHex(String hex)
    {
        return hex != null && hex.matches("^#?[0-9a-fA-F]{6}$");
    }

    private static int parseHex(String hex)
    {
        String value = hex.startsWith("#") ? hex.substring(1) : hex;
        return Integer.parseInt(value, 16);
    }

    private static Component createGradient(String text, int startColor, int endColor)
    {
        Component result = Component.empty();

        final int startRed = startColor >> 16 & 0xFF;
        final int startGreen = startColor >> 8 & 0xFF;
        final int startBlue = startColor & 0xFF;

        final int endRed = endColor >> 16 & 0xFF;
        final int endGreen = endColor >> 8 & 0xFF;
        final int endBlue = endColor & 0xFF;

        for (int i = 0; i < text.length(); i++)
        {
            final double ratio = text.length() == 1
                    ? 0.0
                    : (double) i / (text.length() - 1);

            final int red = (int) Math.round(startRed + (endRed - startRed) * ratio);
            final int green = (int) Math.round(startGreen + (endGreen - startGreen) * ratio);
            final int blue = (int) Math.round(startBlue + (endBlue - startBlue) * ratio);

            result = result.append(Component.text(
                    String.valueOf(text.charAt(i)),
                    TextColor.color(red, green, blue)));
        }

        return result;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
