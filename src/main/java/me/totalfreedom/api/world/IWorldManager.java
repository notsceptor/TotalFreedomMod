package me.totalfreedom.api.world;

import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.world.GeneratedWorld;

public interface IWorldManager
{
    GeneratedWorld flatlands();

    GeneratedWorld adminworld();

    /** The AdminWorld's access gate, or null before onStart's deferred setup has run. */
    IWorldAccessGate adminGate();

    void gotoWorld(Player player, String targetWorld);
}
