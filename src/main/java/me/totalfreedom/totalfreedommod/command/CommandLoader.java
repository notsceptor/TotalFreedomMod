package me.totalfreedom.totalfreedommod.command;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.ChatColor;

@SuppressWarnings("UnstableApiUsage")
public class CommandLoader extends FreedomService
{

    @Getter
    private final CommandHandler<TotalFreedomMod> handler;
    private boolean started = false;

    public CommandLoader(TotalFreedomMod plugin)
    {
        super(plugin);
        handler = new CommandHandler<>(plugin);
    }

    @Override
    protected void onStart()
    {
        if (started)
        {
            return;
        }
        started = true;

        handler.setExecutorFactory(new FreedomCommandExecutor.FreedomExecutorFactory(plugin));
        handler.setCommandClassPrefix("Command_");
        handler.setPermissionMessage(ChatColor.RED + "You do not have permission to use this command.");
        handler.setOnlyConsoleMessage(ChatColor.RED + "This command can only be used from the console.");
        handler.setOnlyPlayerMessage(ChatColor.RED + "This command can only be used by players.");

        int loaded = handler.loadFrom(FreedomCommand.class.getPackage());
        FLog.info("Loaded " + loaded + " commands.");
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
        {
            handler.registerAllWithLifecycle(event.registrar());
        });
    }

    @Override
    protected void onStop()
    {
    }
}
