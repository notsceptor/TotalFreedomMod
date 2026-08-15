package me.totalfreedom.api;

import org.bukkit.GameRule;
import org.bukkit.World;

public interface IGameRuleHandler
{
    void enforceGameRuleDefaultsForWorld(World world);

    <T> void setGameRule(GameRule<T> rule, T newValue);
}
