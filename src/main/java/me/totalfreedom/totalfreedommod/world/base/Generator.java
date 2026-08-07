package me.totalfreedom.totalfreedommod.world.base;

import org.bukkit.generator.ChunkGenerator;

/**
 * Primary world designer; generates the base shape of a chunk out of stone, air, and water, and
 * handles all terrain shaping including rivers. Everything after this either changes what blocks
 * are made of or takes blocks away.
 * <p>
 * Reads its settings from a .json file in {@link org.bukkit.plugin.Plugin#getDataFolder}/worlds,
 * named after the world. One implementation per mode (flat, heightmap, density), picked when the
 * profile compiles.
 */
public interface Generator
{
    /**
     * Writes the chunk. Runs under generateNoise.
     * <p>
     * ChunkData takes local x/z (0-15) and absolute y. Use setRegion for runs of the same block up
     * a column, and sample noise on a grid and interpolate between the samples; a chunk is 98,304
     * blocks, so sampling every one of them is not an option.
     * <p>
     * Off the main thread. Only the context's WorldInfo is safe to touch, never World or entities.
     */
    void generateBase(ChunkContext context, ChunkGenerator.ChunkData data);

    /**
     * Terrain height at a world position, before carving. Pure, no chunk access.
     * <p>
     * Backs getBaseHeight, the spawn finder, and the context's column heights. Must agree with what
     * {@link #generateBase} writes or spawn lands in mid-air.
     */
    int surfaceHeight(ChunkContext context, int worldX, int worldZ);
}
