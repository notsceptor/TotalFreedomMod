package me.totalfreedom.totalfreedommod.command;

import io.papermc.paper.plugin.configuration.PluginMeta;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.Arrays;

@CommandPermissions(level = Rank.SENIOR_ADMIN, source = SourceType.ONLY_CONSOLE, permission = "tfm.admin.telnet.plugincontrol")
@CommandParameters(description = "Manage plugins", usage = "/<command> <<enable | disable | reload> <pluginname>> | list>", aliases = "plc")
public class Command_plugincontrol extends FreedomCommand
{
    private final PluginManager pluginManager = server.getPluginManager();

    @CommandDispatchTarget(pattern = "list")
    public boolean listPlugins(CommandContext ctx)
    {
        msg(ctx.getSender(), Component.join(JoinConfiguration.newlines(), Arrays.stream(pluginManager.getPlugins())
                .map(otherPlugin ->
                {
                    final PluginMeta meta = otherPlugin.getPluginMeta();

                    return Component.text("- ", NamedTextColor.GRAY)
                            .append(Component.text(meta.getDisplayName(), otherPlugin.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED))
                            .append(Component.text(" v", NamedTextColor.GRAY).append(Component.text(meta.getVersion())))
                            .append(meta.getAuthors().isEmpty() ?
                                    Component.empty() :
                                    Component.text(" by ")
                                            .append(Component.join(JoinConfiguration.commas(true),
                                                    meta.getAuthors().stream().map(author -> Component.text(author, NamedTextColor.WHITE)).toList())));
                }).toList()));

        return true;
    }

    @CommandDispatchTarget(pattern = "enable <plugin:Plugin>")
    public boolean enablePlugin(CommandContext ctx, Plugin otherPlugin)
    {
        if (otherPlugin.equals(plugin))
        {
            msg(ctx.getSender(), "I'm pretty sure the TotalFreedomMod is enabled already.", NamedTextColor.RED);
            return true;
        }

        if (otherPlugin.isEnabled())
        {
            msg(ctx.getSender(), otherPlugin.getName() + " is already enabled.");
            return true;
        }

        pluginManager.enablePlugin(otherPlugin);

        msg(ctx.getSender(), otherPlugin.isEnabled() ?
                Component.text(otherPlugin.getName(), NamedTextColor.GREEN)
                        .append(Component.text(" has been successfully enabled.")) :
                Component.text("A problem occurred whilst attempting to enable ",NamedTextColor.RED)
                        .append(Component.text(otherPlugin.getName())));

        return true;
    }

    @CommandDispatchTarget(pattern = "disable <plugin:Plugin>")
    public boolean disablePlugin(CommandContext ctx, Plugin otherPlugin)
    {
        if (otherPlugin.getName().equalsIgnoreCase(plugin.getName()))
        {
            msg(ctx.getSender(), "Did you really think that was going to work?", NamedTextColor.RED);
            return true;
        }

        if (!otherPlugin.isEnabled())
        {
            msg(ctx.getSender(), otherPlugin.getName() + " is already disabled.");
            return true;
        }

        pluginManager.enablePlugin(otherPlugin);

        msg(ctx.getSender(), otherPlugin.getName() + " has been disabled.");
        return true;
    }

    @CommandDispatchTarget(pattern = "reload <plugin:Plugin>")
    public boolean reloadPlugin(CommandContext ctx, Plugin otherPlugin)
    {
        if (otherPlugin.getName().equalsIgnoreCase(plugin.getName()))
        {
            return ((Command_totalfreedommod) CommandHandler.getByName("totalfreedommod")).reloadPlugin(ctx);
        }

        pluginManager.disablePlugin(otherPlugin);
        pluginManager.enablePlugin(otherPlugin);

        msg(ctx.getSender(), otherPlugin.getName() + " has been reloaded.");
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
