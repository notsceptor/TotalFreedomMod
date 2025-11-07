package me.totalfreedom.totalfreedommod;

import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;

public class ServerInterface extends FreedomService
{

    public static final String COMPILE_NMS_VERSION = "v1_21_R1";

    public ServerInterface(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
    }

    @Override
    protected void onStop()
    {
    }

    public static void warnVersion()
    {
        final String nms = FUtil.getNmsVersion();

        // Paper 1.21.10 uses "craftbukkit" as package name, which is compatible
        // Only warn if it's clearly a different major version
        if (!COMPILE_NMS_VERSION.equals(nms) && !nms.equals("craftbukkit"))
        {
            // Check if it's a compatible version (same major.minor)
            boolean isCompatible = nms.startsWith("v1_21") || nms.contains("1.21");
            
            if (!isCompatible)
            {
                FLog.warning(TotalFreedomMod.pluginName + " is compiled for " + COMPILE_NMS_VERSION + " but the server is running version " + nms + "!");
                FLog.warning("This might result in unexpected behaviour!");
            }
        }
    }

//    public void setOnlineMode(boolean mode)
//    {
//        final PropertyManager manager = getServer().getPropertyManager();
//        manager.setProperty("online-mode", mode);
//        manager.savePropertiesFile();
//    }
//
//    public int purgeWhitelist()
//    {
//        String[] whitelisted = getServer().getPlayerList().getWhitelisted();
//        int size = whitelisted.length;
//        for (EntityPlayer player : getServer().getPlayerList().players)
//        {
//            getServer().getPlayerList().getWhitelist().remove(player.getProfile());
//        }
//
//        try
//        {
//            getServer().getPlayerList().getWhitelist().save();
//        }
//        catch (Exception ex)
//        {
//            FLog.warning("Could not purge the whitelist!");
//            FLog.warning(ex);
//        }
//        return size;
//    }
//
//    public boolean isWhitelisted()
//    {
//        return getServer().getPlayerList().getHasWhitelist();
//    }
//
//    public List<?> getWhitelisted()
//    {
//        return Arrays.asList(getServer().getPlayerList().getWhitelisted());
//    }
//
//    public String getVersion()
//    {
//        return getServer().getVersion();
//    }
//
//    private MinecraftServer getServer()
//    {
//        return ((CraftServer) Bukkit.getServer()).getServer();
//    }

}
