package me.totalfreedom.totalfreedommod.world.profile;

import java.io.File;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonObject;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;

/**
 * Reads the .json files in the data folder's worlds directory. A file's name is the world's name,
 * and every file in there is a world we manage.
 * <p>
 * {@link #copyTemplate} is the only way a template would ever reach the disk, 
 * and once it does it stops being a template and becomes that world's profile.
 * <p>
 * Only reads and parses JSON, so it is safe off the main thread. Turning that JSON into a profile
 * is {@link ProfileParser}, which is not.
 */
public final class ProfileLoader
{
    private static final String WORLDS_DIRECTORY = "worlds";

    private final TotalFreedomMod plugin;
    private final File directory;

    public ProfileLoader(final TotalFreedomMod plugin)
    {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), WORLDS_DIRECTORY);
    }

    /** Every world with a profile file on disk. */
    public Set<String> available()
    {

    }

    /**
     * One world's raw JSON, off disk. Empty if it has no file, which is not an error.
     *
     * @throws ProfileException if the file exists but is not readable JSON
     */
    public Optional<JsonObject> read(final String worldName) throws ProfileException
    {

    }

    /** Names of the templates bundled in the jar. Never worlds. */
    public Set<String> templates()
    {

    }

    /**
     * Writes a bundled template out as a new world's profile. This is what creates a managed world,
     * so refuse if a file for that world already exists rather than overwriting someone's edits.
     *
     * @param templateName one of {@link #templates()}
     * @param worldName    the world to create, which becomes the file name
     */
    public boolean copyTemplate(final String templateName, final String worldName)
    {

    }
}
