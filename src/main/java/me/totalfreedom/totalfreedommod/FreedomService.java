package me.totalfreedom.totalfreedommod;

import me.totalfreedom.totalfreedommod.framework.AbstractService;
import org.bukkit.event.Listener;

public abstract class FreedomService extends AbstractService<TotalFreedomMod> implements Listener
{

    public FreedomService(TotalFreedomMod plugin)
    {
        super(plugin);
    }

}
