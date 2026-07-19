package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.Rank;

import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffect;

@Command(name = "purgeall", description = "Superadmin command - Purge everything! (except for bans).", usage = "/<command>")
@Permission(level = Rank.SUPER_ADMIN, permission = "tfm.admin.purgeall")
public class Command_purgeall extends FCommand
{
    @Callback
    public void purgeAll(CommandSender sender)
    {
        adminAction(sender, "<red>Purging all player data");

        plugin().ew.wipeEntities(true);

        server().getOnlinePlayers().forEach(player -> 
        {
            FPlayer fPlayer = fplayer(player);

            if (fPlayer.isMuted())
            {
                fPlayer.setMuted(false);
            }

            if (fPlayer.allCommandsBlocked())
            {
                fPlayer.setCommandsBlocked(false);
            }

            if (fPlayer.isOrbiting())
            {
                fPlayer.stopOrbiting();
            }

            if (fPlayer.getFreezeData().isFrozen())
            {
                fPlayer.getFreezeData().setFrozen(false);
            }

            for (PotionEffect potionEffect : player.getActivePotionEffects())
            {
                player.removePotionEffect(potionEffect.getType());
            }

            if (fPlayer.getCageData().isCaged())
            {
                fPlayer.getCageData().setCaged(false);
            }
        });

        plugin().fm.setGlobalFreeze(false);

        Command_mobpurge.purgeMobs();
    }
}
