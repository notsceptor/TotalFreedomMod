package me.totalfreedom.totalfreedommod.util;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Plaintext online player list shared by automated console contexts.
 */
public final class PlayerListUtil
{

    private PlayerListUtil()
    {
    }

    public static String buildPlainList()
    {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers())
        {
            names.add(player.getName());
        }
        int max = Bukkit.getMaxPlayers();
        return "There are " + names.size() + "/" + max + " players online:\n" + StringUtils.join(names, ", ");
    }
}
