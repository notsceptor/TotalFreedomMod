package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.ONLY_IN_GAME, permission = "tfm.player.tagrainbow")
@CommandParameters(description = "Give yourself a prefix with rainbow colors.", usage = "/<command> <tag..>")
public class Command_tagrainbow extends FreedomCommand
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

    @CommandDispatchTarget(pattern = "<tag..>")
    public boolean tagRainbow(CommandContext ctx, String tag)
    {
        final String cleanedTag = StringUtils.replaceEachRepeatedly(StringUtils.strip(tag),
                new String[]
                        {
                                "\u00A7"
                        },
                new String[]
                        {
                                ""
                        });

        final String plainTag = AdventureUtil.componentToPlainText(FUtil.colorize(cleanedTag)).trim();

        if (plainTag.isEmpty())
        {
            msg(Component.text("Your tag cannot be empty.", NamedTextColor.RED));
            return true;
        }

        if (plainTag.length() > Command_tag.MAX_TAG_LENGTH)
        {
            msg(Component.text(
                    "That tag is too long (Max is " + Command_tag.MAX_TAG_LENGTH + " characters).",
                    NamedTextColor.RED));
            return true;
        }

        if (!plugin.al.isAdmin(sender) && Command_tag.containsForbidden(plainTag))
        {
            msg(Component.text("That tag contains a forbidden word.", NamedTextColor.RED));
            return true;
        }

        final Component rainbowTag = rainbowify(plainTag)
                .append(Component.text("", NamedTextColor.WHITE));

        final String outputTag = AdventureUtil.componentToLegacySection(rainbowTag);

        plugin.pl.getPlayer(playerSender).setTag(outputTag);

        msg(Component.text("Tag set to '", NamedTextColor.GRAY)
                .append(rainbowTag)
                .append(Component.text("'.", NamedTextColor.GRAY)));

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
