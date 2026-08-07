package me.totalfreedom.totalfreedommod.world.profile;

/**
 * Which generator a profile uses. Read once when the profile compiles, to pick the stages.
 * <p>
 * FLAT samples no noise at all. HEIGHTMAP is 2D, has no overhangs, and covers most survival worlds.
 * DENSITY is 3D, gets you overhangs, and takes roughly fifty times the samples.
 */
public enum GenerationMode
{
    FLAT,
    HEIGHTMAP,
    DENSITY
}
