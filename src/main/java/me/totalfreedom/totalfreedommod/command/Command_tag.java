package me.totalfreedom.totalfreedommod.command;

import java.util.Arrays;
import java.util.List;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.FPlayer;
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
@CommandParameters(description = "Sets yourself a prefix", usage = "/<command> <set <tag..> | off | clear <player> | clearall>")
public class Command_tag extends FreedomCommand
{

    public static final List<String> FORBIDDEN_WORDS = Arrays.asList(new String[]
    {
        "admin", "owner", "moderator", "developer", "console"
    });

    private String getClearedTagValue()
    {
        Boolean enforcePrefixConfig = ConfigEntry.VAULT_CHAT_ENFORCE_PREFIX.getBoolean();
        boolean enforcePrefix = enforcePrefixConfig != null ? enforcePrefixConfig : false;
        return !enforcePrefix ? "" : null;
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
                    "\u00A7", "&k"
                },
                new String[]
                {
                    "", ""
                }));
        colorizedTag = colorizedTag.append(Component.text("").color(NamedTextColor.WHITE));
        final String outputTag = AdventureUtil.componentToLegacySection(colorizedTag);

        if (!plugin.al.isAdmin(sender))
        {
            final String rawTag = AdventureUtil.stripColor(outputTag).toLowerCase();

            if (rawTag.length() > 20)
            {
                msg("That tag is too long (Max is 20 characters).");
                return true;
            }

            for (String word : FORBIDDEN_WORDS)
            {
                if (rawTag.contains(word))
                {
                    msg("That tag contains a forbidden word.");
                    return true;
                }
            }
        }

        plugin.pl.getPlayer(playerSender).setTag(outputTag);
        msg("Tag set to '" + outputTag + "'.");

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
            if (playerdata.getTag() != null)
            {
                count++;
                playerdata.setTag(clearedTagValue);
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
            plugin.pl.getPlayer(playerSender).setTag(getClearedTagValue());
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

        plugin.pl.getPlayer(player).setTag(getClearedTagValue());
        msg("Removed " + player.getName() + "'s tag.");

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
