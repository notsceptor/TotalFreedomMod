package me.totalfreedom.totalfreedommod.cmd.internal;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.cmd.SourceType;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchContext;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchSession;
import me.totalfreedom.totalfreedommod.rank.CustomRank;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.ssh.AttributedConsoleSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Enforces {@link Permission} declarations.
 */
public final class PermissionGate
{

    public static final Component ONLY_CONSOLE_MESSAGE =
        Component.text("This command can only be used from the console.", NamedTextColor.RED);
    public static final Component ONLY_PLAYER_MESSAGE =
        Component.text("This command can only be used by players.", NamedTextColor.RED);

    private PermissionGate() {}

    public static CommandSender resolveSender(CommandSender raw)
    {
        RemoteDispatchSession session = RemoteDispatchContext.getActiveSession();
        if (session != null && !(raw instanceof Player))
        {
            return new AttributedConsoleSender(raw, session.getDisplayName());
        }
        return raw;
    }

    /**
     * Tests whether {@code sender} may execute a handler guarded by {@code perm}.
     *
     * @param sendMsg whether to message the sender on failure (pass {@code false} for
     *                silent checks such as Brigadier {@code requires()} / tab visibility)
     */
    public static boolean test(TotalFreedomMod plugin, CommandSender sender, Permission perm, boolean sendMsg)
    {
        if (perm == null)
        {
            return true;
        }

        RemoteDispatchSession dispatch = RemoteDispatchContext.getActiveSession();
        if (dispatch != null)
        {
            if (dispatch.isIdentified())
            {
                String permNode = switch (dispatch.getChannel())
                {
                    case SSH -> "tfm.manage.ssh";
                    case DISCORD -> "tfm.manage.discord";
                };
                String channelLabel = switch (dispatch.getChannel())
                {
                    case SSH -> "SSH";
                    case DISCORD -> "Discord";
                };
                if (!plugin.rm.hasPermission(sender, permNode))
                {
                    if (sendMsg)
                    {
                        sender.sendMessage(Component.text("You do not have permission to run commands via " + channelLabel + ".", NamedTextColor.RED));
                    }
                    return false;
                }
            }
        }
        else if (!(sender instanceof Player) && plugin.al.getEntryByName(sender.getName()) != null)
        {
            if (!plugin.rm.hasPermission(sender, "tfm.manage.telnet"))
            {
                if (sendMsg)
                {
                    sender.sendMessage(Component.text("You do not have permission to run commands via telnet.", NamedTextColor.RED));
                }
                return false;
            }
        }

        final Player player = sender instanceof Player p ? p : null;

        if (perm.source() == SourceType.ONLY_CONSOLE && player != null)
        {
            if (sendMsg)
            {
                sender.sendMessage(ONLY_CONSOLE_MESSAGE);
            }
            return false;
        }

        if (perm.source() == SourceType.ONLY_IN_GAME && player == null)
        {
            if (sendMsg)
            {
                sender.sendMessage(ONLY_PLAYER_MESSAGE);
            }
            return false;
        }

        String tfmPermission = perm.permission();
        if (tfmPermission != null && !tfmPermission.isEmpty())
        {
            boolean result = plugin.rm.hasPermission(sender, tfmPermission);
            if (!result && sendMsg)
            {
                sender.sendMessage(Component.text(perm.message(), NamedTextColor.RED));
            }
            return result;
        }

        if (player != null)
        {
            boolean result = plugin.rm.getRank(player).isAtLeast(perm.level());
            if (!result && sendMsg)
            {
                sender.sendMessage(Component.text(perm.message(), NamedTextColor.RED));
            }
            return result;
        }

        Rank rank = plugin.rm.getRank(sender);
        CustomRank boundCustom = null;
        if (!RemoteDispatchContext.isActive())
        {
            String boundRankId = plugin.csr.getRankIdForSender(sender.getName());
            boundCustom = boundRankId != null ? plugin.rm.getCustomRank(boundRankId) : null;
        }
        boolean result;
        if (boundCustom != null)
        {
            result = boundCustom.isAtLeast(perm.level());
        }
        else
        {
            result = rank.isAtLeast(perm.level());
        }
        if (!result && sendMsg)
        {
            sender.sendMessage(Component.text(perm.message(), NamedTextColor.RED));
        }
        return result;
    }
}
