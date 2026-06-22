package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public abstract class AbstractCommandBase<T extends TotalFreedomMod>
{

    protected T plugin;
    protected org.bukkit.Server server;
    protected CommandSender sender;
    protected Command command;
    protected String label;
    protected String[] args;
    protected Player playerSender;
    protected CommandHandler<T> handler;

    protected void setVariables(CommandSender sender, Command command, String label, String[] args)
    {
        this.sender = sender;
        this.command = command;
        this.label = label;
        this.args = args;
        this.playerSender = sender instanceof Player ? (Player) sender : null;
    }

    protected void setHandler(CommandHandler<T> handler)
    {
        this.handler = handler;
    }

    protected void setPlugin(T plugin)
    {
        this.plugin = plugin;
        this.server = plugin != null ? plugin.getServer() : null;
    }

    protected CommandHandler<T> getHandler()
    {
        return handler;
    }

    protected boolean isConsole()
    {
        return !(sender instanceof Player);
    }

    public abstract boolean runCommand(CommandSender sender, Command command, String label, String[] args);

    public List<String> tabComplete(CommandSender sender, String label, String[] args)
    {
        if (server == null || args.length == 0)
        {
            return List.of();
        }

        String partial = args[args.length - 1].toLowerCase(Locale.ROOT);

        return server.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partial))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
