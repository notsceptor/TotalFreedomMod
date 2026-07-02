package me.totalfreedom.totalfreedommod;

import com.google.common.collect.Range;
import io.papermc.paper.event.world.WorldGameRuleChangeEvent;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoolerGameRuleHandler extends FreedomService
{
    private final Map<GameRule<?>, Object> defaultGameRuleValues = new HashMap<>();
    private final List<Key> maliciousGameRules = new ArrayList<>();
    private final Map<GameRule<?>, Range<Integer>> gameRuleCaps = new HashMap<>();
    //
    private BukkitTask commitTask = null;

    public CoolerGameRuleHandler(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        // Clear the gamerule defaults and known malicious entries from memory
        defaultGameRuleValues.clear();
        maliciousGameRules.clear();
        gameRuleCaps.clear();

        // Don't do anything if we're no longer planning on managing gamerules
        if (!ConfigEntry.GAMERULES_ENABLED.getBoolean())
        {
            return;
        }

        // Load from configuration
        final ConfigurationSection defaults = ConfigEntry.GAMERULES_DEFAULTS.getSection();
        for (final String key : defaults.getKeys(false))
        {
            // Ignore invalid namespaces
            if (!Key.parseable(key))
            {
                continue;
            }

            // IntelliJ, who cares if the string is unsubstituted, who asked for your opinion?
            final GameRule<?> rule = Registry.GAME_RULE.get(Key.key(key));

            // Ignore gamerules that don't exist
            if (rule == null)
            {
                FLog.warning("Ignoring default gamerule " + key + " as it doesn't exist");
                continue;
            }

            final Object value = defaults.get(key);

            // IntelliJ complained about nullability even though that makes no sense given how this code is done
            if (value == null)
            {
                FLog.warning("Ignoring default gamerule " + key + " as the value is somehow null");
                continue;
            }

            // Ignore gamerules with invalid values
            if (!rule.getType().isInstance(value))
            {
                FLog.warning("Ignoring default gamerule " + key + " as it uses an invalid type - Expected " + rule.getType() + ", got " + value.getClass().getName());
                continue;
            }

            defaultGameRuleValues.put(rule, value);
        }

        // Load known malicious entries from memory
        maliciousGameRules.addAll(ConfigEntry.GAMERULES_MALICIOUS.getStringList().stream().map(Key::key).toList());

        // Set up periodic enforcement
        if (ConfigEntry.GAMERULES_ENFORCEMENT_DELAY.getInteger() > 0)
        {
            // Just in case...
            if (commitTask != null && !commitTask.isCancelled())
            {
                commitTask.cancel();
            }

            commitTask = server.getScheduler().runTaskTimer(plugin,
                    this::enforceGameRuleDefaults,
                    0L,
                    ConfigEntry.GAMERULES_ENFORCEMENT_DELAY.getInteger());
        }
    }

    @Override
    protected void onStop()
    {
        commitTask.cancel();
        commitTask = null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onGameRuleChange(WorldGameRuleChangeEvent event)
    {
        // Ignore if we're not bothering with gamerule management
        if (!ConfigEntry.GAMERULES_ENABLED.getBoolean())
        {
            return;
        }

        final CommandSender source = event.getCommandSender();
        final GameRule<?> gameRule = event.getGameRule();

        if (source != null && !plugin.rm.hasPermission(source, "tfm.world.gamerule"))
        {
            // Cancel the event, we don't want players without this permission to be able to mess with this
            event.setCancelled(true);

            // Let's also log it, just in case
            FLog.warning(source.getName()
                    + " just tried to change "
                    + (maliciousGameRules.contains(gameRule.getKey().key()) ? "malicious" : "")
                    + "gamerule "
                    + gameRule.getKey().asString()
                    + " to value "
                    + event.getValue());
        }
    }

    private void enforceGameRuleDefaults()
    {
        FLog.info("Debug - Enforcing defaults now!");

        // For each world...
        for (World world : Bukkit.getWorlds())
        {
            // For each game rule...
            for (GameRule<?> key : defaultGameRuleValues.keySet())
            {
                // Enforce the default
                // This is stupid, but fuck it. I don't care
                enforceGameRuleDefault(world, key);
            }
        }
    }

    private <T> void enforceGameRuleDefault(World world, GameRule<T> rule)
    {
        world.setGameRule(rule, rule.getType().cast(defaultGameRuleValues.get(rule)));
    }
}
