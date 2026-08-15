package me.totalfreedom.api.blocking.sweep;

import org.bukkit.Chunk;

public interface ISweepScheduler
{
    void register(EntityVisitor visitor);

    void register(TileEntityVisitor visitor);

    void enqueueChunk(Chunk chunk, SweepContext ctx);
}
