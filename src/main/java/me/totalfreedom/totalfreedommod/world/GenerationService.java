package me.totalfreedom.totalfreedommod.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;

import com.google.gson.JsonObject;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.world.adapter.ProfileChunkGenerator;
import me.totalfreedom.totalfreedommod.world.profile.ProfileException;
import me.totalfreedom.totalfreedommod.world.profile.ProfileLoader;
import me.totalfreedom.totalfreedommod.world.profile.ProfileParser;

/**
 * Holds the profile registry. Files are read and parsed at startup, and only worlds that parsed
 * cleanly end up in here.
 * <p>
 * A profile that fails is logged with every problem found and then skipped, so one bad file costs
 * you that world and nothing else. The rest of the plugin does not care that world generation had a
 * bad day.
 * <p>
 * Sits behind the plugin's getDefaultWorldGenerator hook, which is what lets a profile drive a
 * world created through bukkit.yml or a world manager instead of only ones we create ourselves.
 */
public final class GenerationService extends FreedomService
{
    /** World names TFM already manages itself, outside the profile system; see {@link Flatlands} and {@link AdminWorld}. */
    private static final Set<String> RESERVED_WORLD_NAMES = Set.of("flatlands", "adminworld");

    private final ProfileLoader loader;
    private final ProfileParser parser;
    private final Map<String, GenerationProfile> profiles;

    public GenerationService(final TotalFreedomMod plugin)
    {
        super(plugin);

        this.loader = new ProfileLoader(plugin);
        this.parser = new ProfileParser();
        this.profiles = new HashMap<>();
    }

    @Override
    protected void onStart()
    {
        final Map<String, JsonObject> biomeLibrary;

        try 
        {
            biomeLibrary = this.loader.biomeLibrary();
        }
        catch (final ProfileException ex)
        {
            FLog.severe("Failed to load biome library: " + ExceptionUtils.getRootCauseMessage(ex));
            Bukkit.getPluginManager().disablePlugin(plugin); // we don't want to load TFM because no worlds can be loaded.
            return;
        }

        this.loader
            .available()
            .forEach(name -> this.loadProfile(name, biomeLibrary));

    }

    @Override
    protected void onStop()
    {

    }

    public Optional<GenerationProfile> profile(final String worldName)
    {
        return Optional.ofNullable(profiles.get(worldName));
    }

    /** Empty if no profile covers the world, or if its file failed to parse. */
    public Optional<ChunkGenerator> generatorFor(final String worldName)
    {
        return profile(worldName).map(p -> new ProfileChunkGenerator(p));
    }

    /** Only worlds whose profiles parsed. A file that failed does not appear here. */
    public Set<String> available()
    {
        return profiles.keySet();
    }

    /**
     * Re-parses every available profile and drops any no longer on disk. A profile that fails to
     * re-parse keeps its last good copy, since {@link #loadProfile} only overwrites an entry once the
     * new one parses cleanly.
     */
    public void reload()
    {
        final Map<String, JsonObject> biomeLibrary;

        try
        {
            biomeLibrary = this.loader.biomeLibrary();
        }
        catch (final ProfileException ex)
        {
            FLog.warning("Failed to reload biome library: " + ExceptionUtils.getRootCauseMessage(ex));
            // we don't want to disable the plugin here because this executes assuming worlds have already loaded.
            return;
        }

        final Set<String> available = this.loader.available();

        this.profiles.keySet().retainAll(available);
        available.forEach(name -> this.loadProfile(name, biomeLibrary));
    }

    private void loadProfile(final String worldName, final Map<String, JsonObject> biomeLibrary)
    {
        if (RESERVED_WORLD_NAMES.contains(worldName))
        {
            FLog.warning("Skipping profile \"" + worldName + "\": that name is reserved for TFM's own " + worldName + " world and can never be generated from a profile.");
            return;
        }

        try
        {
            final Optional<JsonObject> jsonRoot = this.loader.read(worldName);

            if (jsonRoot.isEmpty())
                return;

            this.profiles.put(worldName, parser.parse(worldName, jsonRoot.get(), biomeLibrary));
        }
        catch (final ProfileException ex)
        {
            FLog.warning(String.format("Failed to parse json object for %s: \n%s", worldName, ExceptionUtils.getRootCauseMessage(ex)));
        }
    }
}
