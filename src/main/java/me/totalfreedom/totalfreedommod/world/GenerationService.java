package me.totalfreedom.totalfreedommod.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.generator.ChunkGenerator;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
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

    }

    @Override
    protected void onStop()
    {

    }

    public Optional<GenerationProfile> profile(final String worldName)
    {

    }

    /** Empty if no profile covers the world, or if its file failed to parse. */
    public Optional<ChunkGenerator> generatorFor(final String worldName)
    {

    }

    /** Only worlds whose profiles parsed. A file that failed does not appear here. */
    public Set<String> available()
    {

    }

    /** Re-reads every profile file. Already-loaded worlds keep the profile they were built with. */
    public void reload()
    {

    }
}
