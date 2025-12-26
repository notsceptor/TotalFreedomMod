package me.totalfreedom.totalfreedommod.command;

import lombok.Getter;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.ChatColor;

public class CommandLoader extends FreedomService
{

    @Getter
    private final CommandHandler<TotalFreedomMod> handler;

    public CommandLoader(TotalFreedomMod plugin)
    {
        super(plugin);

        handler = new CommandHandler<>(plugin);
    }

    @Override
    protected void onStart()
    {
        handler.clearCommands();
        handler.setExecutorFactory(new FreedomCommandExecutor.FreedomExecutorFactory(plugin));
        handler.setCommandClassPrefix("Command_");
        handler.setPermissionMessage(ChatColor.RED + "You do not have permission to use this command.");
        handler.setOnlyConsoleMessage(ChatColor.RED + "This command can only be used from the console.");
        handler.setOnlyPlayerMessage(ChatColor.RED + "This command can only be used by players.");

        int loaded = handler.loadFrom(FreedomCommand.class.getPackage());
        handler.registerAll("TotalFreedomMod", true);

        FLog.info("Loaded " + loaded + " commands.");
    }

    @Override
    protected void onStop()
    {
        handler.clearCommands();
    }

}
