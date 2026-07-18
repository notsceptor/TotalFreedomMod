package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Subcommand;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.discord.DiscordBridge;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

/*
 * See https://github.com/TotalFreedom/License - This file may not be edited or removed.
 */
@Command(name = "totalfreedommod", description = "Shows information about TotalFreedomMod or reloads it", usage = "/totalfreedommod [reload]", aliases = {"tfm"})
@Permission(permission = "tfm.server.info", level = Rank.NON_OP)
public class Command_totalfreedommod extends FCommand
{
    @Subcommand("reload")
    @Permission(level = Rank.SUPER_ADMIN, permission = "tfm.server.info")
    @Callback
    public void reloadPlugin(CommandSender sender)
    {
        if (!plugin().al.isAdmin(sender))
        {
            showPluginInformation(sender);
            return;
        }

        plugin().config.load();
        FPlayer.refreshConfig();
        plugin().csr.load();
        DiscordBridge.reloading = true;
        try
        {
            plugin().services.stop();
            plugin().services.start();
        }
        finally
        {
            DiscordBridge.reloading = false;
        }

        final String message = String.format("<gray>%s v%s reloaded.",
                TotalFreedomMod.pluginName,
                TotalFreedomMod.pluginVersion);

        msg(sender, message);
    }

    @Callback
    public void showPluginInformation(CommandSender sender)
    {
        TotalFreedomMod.BuildProperties build = TotalFreedomMod.build;
        msg(sender, "<gold>TotalFreedomMod for 'Total Freedom', the original all-op server.", NamedTextColor.GOLD);
        msg(sender,"<gold>Running on <server>.", Placeholder.unparsed("server", ConfigEntry.SERVER_NAME.getString()));
        msg(sender,"<gold>Created by Madgeek1450 and Prozza.");
        msg(sender, "<gold>Version <blue><codename> - <version> Build <number> <gold>(<blue><head><gold>)",
                Placeholder.unparsed("codename", build.codename),
                Placeholder.unparsed("version", build.version),
                Placeholder.unparsed("number", build.number),
                Placeholder.unparsed("head", build.head));
        msg(sender, "<gold>Compiled <blue><date> <gold>by <blue><author>",
                Placeholder.unparsed("date", build.date),
                Placeholder.unparsed("author", build.author));
        msg(sender, "<green>Visit <aqua>https://github.com/tfreedomorg/totalfreedommod <green>for more information.");
    }
}