package me.totalfreedom.totalfreedommod;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class GameRuleHandler extends FreedomService
{

    private static final int DEFAULT_RANDOM_TICK_SPEED = 3;
    private static final int DEFAULT_MAX_NUMERIC_VALUE = 1024;

    private static final Set<String> INT_RULES = Set.of(
            "maxentitycramming",
            "spawnchunkradius",
            "snowaccumulationheight",
            "playerssleepingpercentage",
            "minecartmaxspeed",
            "spawnradius");

    private final Map<GameRule, Boolean> rules = new EnumMap<>(GameRule.class);

    private int sweepTaskId = -1;

    public GameRuleHandler(TotalFreedomMod plugin)
    {
        super(plugin);

        for (GameRule gameRule : GameRule.values())
        {
            rules.put(gameRule, gameRule.getDefaultValue());
        }
    }

    @Override
    protected void onStart()
    {
        /*setGameRule(GameRule.DO_DAYLIGHT_CYCLE, !ConfigEntry.DISABLE_NIGHT.getBoolean(), false);
        setGameRule(GameRule.DO_FIRE_TICK, ConfigEntry.ALLOW_FIRE_SPREAD.getBoolean(), false);
        setGameRule(GameRule.DO_MOB_LOOT, false, false);
        setGameRule(GameRule.DO_MOB_SPAWNING, !ConfigEntry.MOB_LIMITER_ENABLED.getBoolean(), false);
        setGameRule(GameRule.DO_TILE_DROPS, false, false);
        setGameRule(GameRule.MOB_GRIEFING, false, false);
        setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, false);
        setGameRule(GameRule.NATURAL_REGENERATION, true, false);
        Bukkit.getScheduler().runTask(plugin, () ->
        {
            commitGameRules();
            enforceNumericLimits();
        });
        schedulePeriodicEnforcement();*/
    }

    @Override
    protected void onStop()
    {
        /*if (sweepTaskId != -1)
        {
            server.getScheduler().cancelTask(sweepTaskId);
            sweepTaskId = -1;
        }*/
    }

    private void schedulePeriodicEnforcement()
    {
        long interval = ConfigEntry.CRASH_GAMERULES_SWEEP_TICKS.getInteger();
        if (interval <= 0)
        {
            return;
        }
        sweepTaskId = server.getScheduler()
                .runTaskTimer(plugin, this::enforceNumericLimits, interval, interval)
                .getTaskId();
    }

    private int randomTickSpeedCap()
    {
        int v = ConfigEntry.CRASH_GAMERULES_RANDOM_TICK_SPEED.getInteger();
        return v > 0 ? v : DEFAULT_RANDOM_TICK_SPEED;
    }

    private int maxNumericValue()
    {
        int v = ConfigEntry.CRASH_GAMERULES_MAX_NUMERIC_VALUE.getInteger();
        return v > 0 ? v : DEFAULT_MAX_NUMERIC_VALUE;
    }
    private void enforceNumericLimits()
    {
        final int rtsCap = randomTickSpeedCap();
        final int maxNumeric = maxNumericValue();
        for (World world : Bukkit.getWorlds())
        {
            for (org.bukkit.GameRule<?> rule : org.bukkit.GameRule.values())
            {
                if (rule.getType() != Integer.class)
                {
                    continue;
                }
                final String name = rule.getName().toLowerCase(Locale.ROOT);
                final boolean randomTick = "randomtickspeed".equals(name);
                if (!randomTick && !INT_RULES.contains(name))
                {
                    continue;
                }
                @SuppressWarnings("unchecked")
                final org.bukkit.GameRule<Integer> intRule = (org.bukkit.GameRule<Integer>) rule;
                final Integer current;
                try
                {
                    current = world.getGameRuleValue(intRule);
                }
                catch (Throwable t)
                {
                    continue;
                }
                if (current == null)
                {
                    continue;
                }
                final int cap = randomTick ? rtsCap : maxNumeric;
                if (current <= cap && current >= 0)
                {
                    continue;
                }
                final int clamped = current < 0 ? 0 : cap;
                try
                {
                    world.setGameRule(intRule, clamped);
                    FLog.warning("[GameRuleHandler] Clamped " + rule.getName() + " from " + current
                            + " to " + clamped + " in world '" + world.getName() + "'.");
                }
                catch (Throwable ignored)
                {
                }
            }
        }
    }

    public void setGameRule(GameRule gameRule, boolean value)
    {
        setGameRule(gameRule, value, true);
    }

    public void setGameRule(GameRule gameRule, boolean value, boolean doCommit)
    {
        rules.put(gameRule, value);
        if (doCommit)
        {
            commitGameRules();
        }
    }

    public void commitGameRules()
    {
        List<World> worlds = Bukkit.getWorlds();
        Iterator<Map.Entry<GameRule, Boolean>> it = rules.entrySet().iterator();
        while (it.hasNext())
        {

            Map.Entry<GameRule, Boolean> gameRuleEntry = it.next();
            String gameRuleName = gameRuleEntry.getKey().getGameRuleName();
            String gameRuleValue = gameRuleEntry.getValue().toString();

            for (World world : worlds)
            {
                world.setGameRuleValue(gameRuleName, gameRuleValue);
                if (gameRuleEntry.getKey() == GameRule.DO_DAYLIGHT_CYCLE && !gameRuleEntry.getValue())
                {
                    try
                    {
                        long time = world.getTime();
                        time -= time % 24000;
                        world.setTime(time + 24000 + 6000);
                    }
                    catch (IllegalArgumentException ignored)
                    {
                    }
                }
            }

        }
    }

    public enum GameRule
    {

        DO_FIRE_TICK("do_fire_tick", true),
        MOB_GRIEFING("mob_griefing", true),
        KEEP_INVENTORY("keep_inventory", false),
        DO_MOB_SPAWNING("do_mob_spawning", true),
        DO_MOB_LOOT("do_mob_loot", true),
        DO_TILE_DROPS("do_tile_drops", true),
        COMMAND_BLOCK_OUTPUT("command_block_output", true),
        NATURAL_REGENERATION("natural_regeneration", true),
        DO_DAYLIGHT_CYCLE("do_daylight_cycle", true);
        //
        private final String gameRuleName;
        private final boolean defaultValue;

        private GameRule(String gameRuleName, boolean defaultValue)
        {
            this.gameRuleName = gameRuleName;
            this.defaultValue = defaultValue;
        }

        public String getGameRuleName()
        {
            return gameRuleName;
        }

        public boolean getDefaultValue()
        {
            return defaultValue;
        }
    }

}
