package me.totalfreedom.totalfreedommod.bridge;

import java.util.Collections;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.plugin.Plugin;

public class CoreProtectBridge extends FreedomService
{
    private static final int ROLLBACK_TIME = 2592000;
    private CoreProtectAPI coreProtectAPI;

    public CoreProtectBridge(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        coreProtectAPI = findCoreProtectAPI();

        if (coreProtectAPI != null)
        {
            FLog.info("CoreProtect integration enabled.");
        }
    }

    @Override
    protected void onStop()
    {
        coreProtectAPI = null;
    }

    public boolean isEnabled()
    {
        return getCoreProtectAPI() != null;
    }

    public boolean rollback(String username)
    {
        CoreProtectAPI api = getCoreProtectAPI();

        if (api == null)
        {
            return false;
        }

        server.getScheduler().runTaskAsynchronously(plugin, () ->
        {
            try
            {
                api.performRollback(
                        ROLLBACK_TIME,
                        Collections.singletonList(username),
                        null,
                        null,
                        null,
                        null,
                        0,
                        null);
            }
            catch (Exception ex)
            {
                FLog.severe(ex);
            }
        });

        return true;
    }

    public boolean restore(String username)
    {
        CoreProtectAPI api = getCoreProtectAPI();

        if (api == null)
        {
            return false;
        }

        server.getScheduler().runTaskAsynchronously(plugin, () ->
        {
            try
            {
                api.performRestore(
                        ROLLBACK_TIME,
                        Collections.singletonList(username),
                        null,
                        null,
                        null,
                        null,
                        0,
                        null);
            }
            catch (Exception ex)
            {
                FLog.severe(ex);
            }
        });

        return true;
    }

    public CoreProtectAPI getCoreProtectAPI()
    {
        if (coreProtectAPI != null && coreProtectAPI.isEnabled())
        {
            return coreProtectAPI;
        }

        coreProtectAPI = findCoreProtectAPI();
        return coreProtectAPI;
    }

    private CoreProtectAPI findCoreProtectAPI()
    {
        try
        {
            Plugin coreProtectPlugin = server.getPluginManager().getPlugin("CoreProtect");

            if (!(coreProtectPlugin instanceof CoreProtect coreProtect) || !coreProtect.isEnabled())
            {
                return null;
            }

            CoreProtectAPI api = coreProtect.getAPI();

            if (api == null || !api.isEnabled())
            {
                return null;
            }

            return api;
        }
        catch (Exception ex)
        {
            FLog.severe(ex);
            return null;
        }
    }
}
