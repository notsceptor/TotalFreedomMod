package me.totalfreedom.totalfreedommod.command;

import lombok.Getter;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.pravian.aero.command.handler.SimpleCommandHandler;
import org.bukkit.ChatColor;

public class CommandLoader extends FreedomService
{

    @Getter
    private final SimpleCommandHandler<TotalFreedomMod> handler;

    public CommandLoader(TotalFreedomMod plugin)
    {
        super(plugin);

        handler = new SimpleCommandHandler<>(plugin);
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

        // Aero 2.2+ returns int (number of commands loaded) instead of void
        // Use reflection to handle both old (void) and new (int) signatures
        try
        {
            java.lang.reflect.Method loadFromMethod = handler.getClass().getMethod("loadFrom", Package.class);
            loadFromMethod.invoke(handler, FreedomCommand.class.getPackage());
        }
        catch (Exception e)
        {
            FLog.severe("Failed to load commands via reflection: " + e.getMessage());
            throw new RuntimeException("Command loading failed", e);
        }
        handler.registerAll("TotalFreedomMod", true);

        FLog.info("Loaded " + handler.getExecutors().size() + " commands.");
    }

    @Override
    protected void onStop()
    {
        handler.clearCommands();
    }

}
