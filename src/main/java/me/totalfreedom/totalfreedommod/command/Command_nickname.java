package me.totalfreedom.totalfreedommod.command;

import java.util.Arrays;
import java.util.List;

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

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.player.nickname")
@CommandParameters(description = "Manages player nicknames", usage = "/<command> <<nickname..> | set <player> <nickname..> | clear [player] | clearall>", aliases = "nick")
public class Command_nickname extends FreedomCommand
{
    public static final int MAX_NICKNAME_LENGTH = 30;

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

    private void clearNickname(Player player)
    {
        plugin.pl.getData(player).setNickname(null);
    }

    @CommandDispatchTarget(pattern = "set <player:Player> <nickname..>")
    public boolean setPlayerNickname(CommandContext ctx, Player player, final String nickname)
    {
        final boolean isAdmin = plugin.al.isAdmin(sender);

        final boolean settingOwnNickname = player == null;

        if (ctx.isSenderConsole() && settingOwnNickname)
        {
            msg("The console may not have a nickname.");
            return true;
        }

        if (!isAdmin && !settingOwnNickname)
        {
            msg("You may not change someone else's nickname.");
            return true;
        }

        if (player == null)
            player = ctx.getPlayerSender();

        Component colorizedNickname = FUtil.colorize(StringUtils.replaceEachRepeatedly(StringUtils.strip(nickname),
                new String[]
                {
                    "\u00A7", "&k"
                },
                new String[]
                {
                    "", ""
                }));
        final String rawNickname = AdventureUtil.componentToPlainText(colorizedNickname).toLowerCase();

        if (rawNickname.length() > MAX_NICKNAME_LENGTH)
        {
            msg("That nickname is too long (Max is " + MAX_NICKNAME_LENGTH + " characters).");
            return true;
        }

        if (!isAdmin && containsForbidden(rawNickname))
        {
            msg("That tag contains a forbidden word.");
            return true;
        }

        final PlayerData data = plugin.pl.getData(player);

        data.setNickname(colorizedNickname);
        msg((player == playerSender ? Component.text("Nickname") : Component.text(player.getName()).append(Component.text("'s")))
            .append(Component.text(" set to '"))
            .color(NamedTextColor.GRAY)
            .append(data.getDisplayedNickname())
            .append(Component.text("'.", NamedTextColor.GRAY)));

        return true;
    }

    @CommandDispatchTarget(pattern = "clearall")
    public boolean clearAllNicknames(CommandContext ctx)
    {
        if (!plugin.al.isAdmin(sender))
        {
            noPerms();
            return true;
        }

        FUtil.adminAction(sender.getName(), "Removing all nicknames", true);

        int count = 0;
        for (final Player player : server.getOnlinePlayers())
        {
            final PlayerData data = plugin.pl.getData(player);
            if (data.getNickname() == null)
                continue;
            data.setNickname(null);
            count++;
        }

        msg(count + " nickname(s) removed.");

        return true;
    }

    @CommandDispatchTarget(pattern = "clear <player:Player>")
    public boolean clearPlayerNickname(CommandContext ctx, Player player)
    {
        if (player == null && ctx.isSenderConsole())
        {
            msg("The console may not have a nickname.");
            return true;
        }

        if (player != null && !plugin.al.isAdmin(ctx.getSender()))
        {
            noPerms();
            return true;
        }

        if (player == null)
            player = ctx.getPlayerSender();

        clearNickname(player);
        msg("Removed " + (player == ctx.getPlayerSender() ? "your" : (player.getName() + "'s")) + " nickname.");

        return true;
    }

    @CommandDispatchTarget(priority = DispatchTargetPriority.LOW, pattern = "<nickname..>")
    public boolean setOwnNickname(CommandContext ctx, final String nickname)
    {
        return setPlayerNickname(ctx, null, nickname);
    }

    @CommandDispatchTarget(pattern = "clear")
    public boolean clearOwnNickname(CommandContext ctx)
    {
        return clearPlayerNickname(ctx, null);
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
