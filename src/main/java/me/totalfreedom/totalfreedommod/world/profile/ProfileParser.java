package me.totalfreedom.totalfreedommod.world.profile;

import com.google.gson.JsonObject;

import me.totalfreedom.totalfreedommod.world.GenerationProfile;

/**
 * Turns a profile file into a checked {@link GenerationProfile}, or explains why it cannot.
 * <p>
 * A missing required key is an error; a missing optional one is {@code Optional.empty()}. 
 * Nothing is quietly defaulted into something that generates the wrong terrain.
 * <p>
 * Collect every problem before giving up, so one run of the server tells an admin everything wrong
 * with the file. Stopping at the first error means fixing typos one server restart at a time.
 * <p>
 * Main thread only, since block and biome names are looked up here. Reading the file is not, so do
 * that first and hand the parsed JSON in.
 */
public final class ProfileParser
{
    /**
     * @param worldName the file's name, which becomes the world's name
     * @throws ProfileException carrying every problem found, never just the first
     */
    public GenerationProfile parse(final String worldName, final JsonObject root) throws ProfileException
    {

    }
}
