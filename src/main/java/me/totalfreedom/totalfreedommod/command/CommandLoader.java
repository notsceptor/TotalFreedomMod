package me.totalfreedom.totalfreedommod.command;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.command.resolver.*;
import me.totalfreedom.totalfreedommod.util.FLog;

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
        handler.setPermissionMessage("\u00A7cYou do not have permission to use this command.");
        handler.setOnlyConsoleMessage("\u00A7cThis command can only be used from the console.");
        handler.setOnlyPlayerMessage("\u00A7cThis command can only be used by players.");
        
        // Argument resolver registration
        handler.registerArgumentResolver(PlayerArgumentResolver.class);
        handler.registerArgumentResolver(PlayerListArgumentResolver.class);
        handler.registerArgumentResolver(KeyArgumentResolver.class);
        handler.registerArgumentResolver(EnchantmentArgumentResolver.class);
        handler.registerArgumentResolver(PotionEffectTypeArgumentResolver.class);
        handler.registerArgumentResolver(EntityTypeArgumentResolver.class);
        handler.registerArgumentResolver(MaterialArgumentResolver.class);
        handler.registerArgumentResolver(MaterialQueryArgumentProvider.class);
        handler.registerArgumentResolver(PluginArgumentResolver.class);
        handler.registerArgumentResolver(InetAddressResolver.class);
        handler.registerArgumentResolver(InetAddressListResolver.class);
        handler.registerArgumentResolver(BooleanArgumentResolver.class);
        handler.registerArgumentResolver(IntegerArgumentResolver.class);
        handler.registerArgumentResolver(DoubleArgumentResolver.class);

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
