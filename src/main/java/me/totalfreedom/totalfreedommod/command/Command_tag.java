package me.totalfreedom.totalfreedommod.command;

import java.util.Arrays;
import java.util.List;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.player.tag")
@CommandParameters(description = "Sets yourself a prefix", usage = "/<command> [-s[ave]] <set <tag..> | list | gradient <hex> <hex> <tag..> | off | clear <player> | clearall>")
public class Command_tag extends FreedomCommand
{

    public static final int MAX_TAG_LENGTH = 20;

    public static final List<String> FORBIDDEN_WORDS = Arrays.asList(new String[]
            {
                    "[SA]", "[SrA]", "[Dev]", "[Exec]", "[Owner]", "admin", "owner", "moderator", "developer", "console"
            });

    public static boolean containsForbidden(String plainText)
    {
        final String raw = plainText.toLowerCase();
        for (String word : FORBIDDEN_WORDS)
        {
            if (raw.contains(word))
            {
                return true;
            }
        }
        return false;
    }

    private String getClearedTagValue()
    {
        Boolean enforcePrefixConfig = ConfigEntry.VAULT_CHAT_ENFORCE_PREFIX.getBoolean();
        boolean enforcePrefix = enforcePrefixConfig != null ? enforcePrefixConfig : false;
        return !enforcePrefix ? "" : null;
    }

    private static boolean hasTag(String tag)
    {
        return tag != null && !tag.isEmpty();
    }

    private void clearPlayerTag(Player player)
    {
        plugin.pl.getPlayer(player).setTag(getClearedTagValue());
        plugin.pl.clearSavedTag(player);
    }

    private boolean applyTag(Component colorizedTag, boolean save)
    {
        colorizedTag = colorizedTag.append(Component.text("").color(NamedTextColor.WHITE));

        final String outputTag = AdventureUtil.componentToLegacySection(colorizedTag);
        final String rawTag = AdventureUtil.componentToPlainText(colorizedTag).toLowerCase();

        if (rawTag.isEmpty())
        {
            msg("Your tag cannot be empty.");
            return true;
        }

        if (rawTag.length() > MAX_TAG_LENGTH)
        {
            msg("That tag is too long (Max is " + MAX_TAG_LENGTH + " characters).");
            return true;
        }

        if (!plugin.al.isAdmin(sender) && containsForbidden(rawTag))
        {
            msg("That tag contains a forbidden word.");
            return true;
        }

        plugin.pl.getPlayer(playerSender).setTag(outputTag);

        msg(Component.text("Tag set to '", NamedTextColor.GRAY)
                .append(colorizedTag)
                .append(Component.text("'.", NamedTextColor.GRAY)));

        if (save)
        {
            if (!plugin.pl.saveCurrentTag(playerSender))
            {
                msg("Could not save your tag.");
                return true;
            }

            msg("Your tag has been saved and will persist until cleared.");
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "list")
    public boolean listTags(CommandContext ctx)
    {
        msg("Tags for all online players:");

        for (final Player player : server.getOnlinePlayers())
        {
            final FPlayer playerdata = plugin.pl.getPlayer(player);
            if (playerdata.getTag() != null)
            {
                msg(player.getName() + ": " + playerdata.getTag());
            }
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "set <tag..>", switches = "s,save")
    public boolean setTag(CommandContext ctx, String tag, boolean shortSave, boolean longSave)
    {
        if (ctx.isSenderConsole())
        {
            msg("\"/tag set\" can't be used from the console.");
            return true;
        }

        Component colorizedTag = FUtil.colorize(StringUtils.replaceEachRepeatedly(StringUtils.strip(tag),
                new String[]
                        {
                                "\u00A7"
                        },
                new String[]
                        {
                                ""
                        }));

        return applyTag(colorizedTag, shortSave || longSave);
    }

    @CommandDispatchTarget(pattern = "gradient <startHex> <endHex> <tag..>", switches = "s,save")
    public boolean gradientTag(CommandContext ctx, String startHex, String endHex, String tag, boolean shortSave, boolean longSave)
    {
        if (ctx.isSenderConsole())
        {
            msg("\"/tag gradient\" can't be used from the console.");
            return true;
        }

        if (!isValidHex(startHex) || !isValidHex(endHex))
        {
            msg("Both colors must be valid six-digit hex colors. Example: #FF0000");
            return true;
        }

        final String cleanedTag = StringUtils.replaceEachRepeatedly(StringUtils.strip(tag),
                new String[]
                        {
                                "\u00A7"
                        },
                new String[]
                        {
                                ""
                        });

        final String plainTag = AdventureUtil.componentToPlainText(FUtil.colorize(cleanedTag));
        final Component gradientTag = createGradient(plainTag, parseHex(startHex), parseHex(endHex));

        return applyTag(gradientTag, shortSave || longSave);
    }

    @CommandDispatchTarget(pattern = "clearall")
    public boolean clearallTags(CommandContext ctx)
    {
        if (!plugin.al.isAdmin(sender))
        {
            noPerms();
            return true;
        }

        FUtil.adminAction(sender.getName(), "Removing all tags", false);

        String clearedTagValue = getClearedTagValue();
        int count = 0;

        for (final Player player : server.getOnlinePlayers())
        {
            final FPlayer playerdata = plugin.pl.getPlayer(player);
            final PlayerData data = plugin.pl.getData(player);

            if (hasTag(playerdata.getTag()) || hasTag(data.getSavedTag()))
            {
                count++;
                playerdata.setTag(clearedTagValue);
                plugin.pl.clearSavedTag(player);
            }
        }

        msg(count + " tag(s) removed.");

        return true;
    }

    @CommandDispatchTarget(pattern = "off")
    public boolean removeTag(CommandContext ctx)
    {
        if (ctx.isSenderConsole())
        {
            msg("\"/tag off\" can't be used from the console. Use \"/tag clear <player>\" or \"/tag clearall\" instead.");
        }
        else
        {
            clearPlayerTag(playerSender);
            msg("Your tag has been removed.");
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "clear <player>")
    public boolean clearTag(CommandContext ctx, String playerName)
    {
        if (!plugin.al.isAdmin(sender))
        {
            noPerms();
            return true;
        }

        final Player player = getPlayer(playerName);

        if (player == null)
        {
            msg(FreedomCommand.PLAYER_NOT_FOUND);
            return true;
        }

        clearPlayerTag(player);
        msg("Removed " + player.getName() + "'s tag.");

        return true;
    }

    private static boolean isValidHex(String hex)
    {
        return hex != null && hex.matches("^#?[0-9a-fA-F]{6}$");
    }

    private static int parseHex(String hex)
    {
        final String value = hex.startsWith("#") ? hex.substring(1) : hex;
        return Integer.parseInt(value, 16);
    }

    private static Component createGradient(String text, int startColor, int endColor)
    {
        Component result = Component.empty();

        final int[] characters = text.codePoints().toArray();

        final int startRed = startColor >> 16 & 0xFF;
        final int startGreen = startColor >> 8 & 0xFF;
        final int startBlue = startColor & 0xFF;

        final int endRed = endColor >> 16 & 0xFF;
        final int endGreen = endColor >> 8 & 0xFF;
        final int endBlue = endColor & 0xFF;

        for (int i = 0; i < characters.length; i++)
        {
            final double ratio = characters.length == 1
                    ? 0.0
                    : (double) i / (characters.length - 1);

            final int red = (int) Math.round(startRed + (endRed - startRed) * ratio);
            final int green = (int) Math.round(startGreen + (endGreen - startGreen) * ratio);
            final int blue = (int) Math.round(startBlue + (endBlue - startBlue) * ratio);

            result = result.append(Component.text(
                    new String(Character.toChars(characters[i])),
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
