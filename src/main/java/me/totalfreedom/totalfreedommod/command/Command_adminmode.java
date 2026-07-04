package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_CONSOLE, permission = "tfm.server.adminmode")
@CommandParameters(description = "Close the server to non-admins.", usage = "/<command> [on | off]")
public class Command_adminmode extends FreedomCommand
{
    @CommandDispatchTarget
    public boolean query(CommandContext ctx)
    {
        msg(ctx.getSender(), Component.text("Admins-only mode is currently ", NamedTextColor.GRAY)
                .append(ConfigEntry.ADMIN_ONLY_MODE.getBoolean() ?
                        Component.text("enabled", NamedTextColor.RED) :
                        Component.text("disabled", NamedTextColor.GREEN))
                .append(Component.text(".")));
        return true;
    }

    @CommandDispatchTarget(pattern = "<value:Boolean>")
    public boolean setMode(CommandContext ctx, Boolean value)
    {
        FUtil.adminAction(ctx.getSender().getName(), value ? "Closing the server to non-admins" : "Opening the server to all players", true);
        ConfigEntry.ADMIN_ONLY_MODE.setBoolean(value);

        if (value)
        {
            server.getOnlinePlayers().stream().filter(player -> !plugin.al.isAdmin(player)).forEach(player ->
                    player.kick(Component.text("The server is now closed to non-admins.", NamedTextColor.RED)));
        }

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
