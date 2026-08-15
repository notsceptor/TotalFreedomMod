package me.totalfreedom.totalfreedommod.freeze;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;

import lombok.Getter;

import static me.totalfreedom.totalfreedommod.player.FPlayer.AUTO_PURGE_TICKS;

public class FreezeData
{

    private final FPlayer fPlayer;
    //
    @Getter
    private Location location = null;
    private BukkitTask unfreeze = null;

    public FreezeData(FPlayer fPlayer)
    {
        this.fPlayer = fPlayer;
    }

    public boolean isFrozen()
    {
        return unfreeze != null;
    }

    public void setFrozen(boolean freeze)
    {
        final Player player = fPlayer.getPlayer();
        if (player == null)
        {
            FLog.info("Could not freeze " + fPlayer.getName() + ". Player not online!");
            return;
        }

        final PlayerData data = fPlayer.getPlugin().getPlayerData(player);
        if (data.isFrozen() != freeze)
        {
            data.setFrozen(freeze);
            fPlayer.getPlugin().savePlayerData(data);
        }

        FUtil.cancel(unfreeze);
        unfreeze = null;
        location = null;

        if (!freeze)
        {
            if (fPlayer.getPlayer().getGameMode() != GameMode.CREATIVE)
            {
                FUtil.setFlying(player, false);
            }

            return;
        }

        location = player.getLocation(); // Blockify location
        FUtil.setFlying(player, true); // Avoid infinite falling

        if (fPlayer.getPlugin().admins().isAdminImpostor(player))
        {
            return; // Don't run unfreeze task for impostors
        }

        unfreeze = fPlayer.getPlugin().getServer().getScheduler().runTaskLater(fPlayer.getPlugin(), () ->
        {
            FUtil.adminAction("TotalFreedom", "Unfreezing " + player.getName(), false);
            setFrozen(false);
        }, AUTO_PURGE_TICKS);
    }

}
