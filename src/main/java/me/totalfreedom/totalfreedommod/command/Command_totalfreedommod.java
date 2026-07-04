package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.config.MainConfig;
import me.totalfreedom.totalfreedommod.discord.DiscordBridge;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/*
 * See https://github.com/TotalFreedom/License - This file may not be edited or removed.
 */
@CommandPermissions(level = Rank.NON_OP, source = SourceType.BOTH, permission = "tfm.server.info")
@CommandParameters(description = "Shows information about TotalFreedomMod or reloads it", usage = "/<command> [reload]", aliases = "tfm")
public class Command_totalfreedommod extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "reload")
    public boolean reloadPlugin(CommandContext ctx)
    {
        if (!plugin.al.isAdmin(ctx.getSender()))
        {
            return showPluginInformation(ctx);
        }

        plugin.config.load();
        FPlayer.refreshConfig();
        plugin.csr.load();
        DiscordBridge.reloading = true;
        try
        {
            plugin.services.stop();
            plugin.services.start();
        }
        finally
        {
            DiscordBridge.reloading = false;
        }

        final String message = String.format("%s v%s reloaded.",
                TotalFreedomMod.pluginName,
                TotalFreedomMod.pluginVersion);

        msg(ctx.getSender(), message);
        return true;
    }

    @CommandDispatchTarget
    public boolean showPluginInformation(CommandContext ctx)
    {
        TotalFreedomMod.BuildProperties build = TotalFreedomMod.build;
        msg(ctx.getSender(), "TotalFreedomMod for 'Total Freedom', the original all-op server.", NamedTextColor.GOLD);
        msg(ctx.getSender(),"Running on " + ConfigEntry.SERVER_NAME.getString() + ".", NamedTextColor.GOLD);
        msg(ctx.getSender(),"Created by Madgeek1450 and Prozza.", NamedTextColor.GOLD);
        msg(ctx.getSender(), Component.text("Version ", NamedTextColor.GOLD)
                .append(Component.text(build.codename + " - " + build.version + " Build " + build.number + " ", NamedTextColor.BLUE))
                .append(Component.text("(", NamedTextColor.GOLD))
                .append(Component.text(build.head, NamedTextColor.BLUE))
                .append(Component.text(")", NamedTextColor.GOLD)));
        msg(ctx.getSender(), Component.text("Compiled ", NamedTextColor.GOLD)
                .append(Component.text(build.date, NamedTextColor.BLUE))
                .append(Component.text(" by ", NamedTextColor.GOLD))
                .append(Component.text(build.author, NamedTextColor.BLUE)));
        msg(ctx.getSender(), Component.text("Visit ", NamedTextColor.GREEN)
                .append(Component.text("https://github.com/tfreedomorg/totalfreedommod", NamedTextColor.AQUA))
                .append(Component.text(" for more information.", NamedTextColor.GREEN)));

        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
