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
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.player.tag")
@CommandParameters(description = "Sets yourself a prefix", usage = "/<command> <set <tag..> | save | off | clear <player> | clearall>")
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

    @CommandDispatchTarget(pattern = "set <tag..>")
    public boolean setTag(CommandContext ctx, String tag)
    {
        Component colorizedTag = FUtil.colorize(StringUtils.replaceEachRepeatedly(StringUtils.strip(tag),
                new String[]
                {
                    "\u00A7"
                },
                new String[]
                {
                    ""
                }));
        colorizedTag = colorizedTag.append(Component.text("").color(NamedTextColor.WHITE));
        final String outputTag = AdventureUtil.componentToLegacySection(colorizedTag);
        final String rawTag = AdventureUtil.componentToPlainText(colorizedTag).toLowerCase();

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

        return true;
    }

    @CommandDispatchTarget(pattern = "save")
    public boolean saveTag(CommandContext ctx)
    {
        if (ctx.isSenderConsole())
        {
            msg("\"/tag save\" can't be used from the console.");
            return true;
        }

        final FPlayer fPlayer = plugin.pl.getPlayer(playerSender);
        if (!hasTag(fPlayer.getTag()))
        {
            msg("You don't have a tag set. Use \"/tag set <tag>\" first.");
            return true;
        }

        if (!plugin.pl.saveCurrentTag(playerSender))
        {
            msg("Could not save your tag.");
            return true;
        }

        msg("Your tag has been saved and will persist until cleared.");
        return true;
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

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
