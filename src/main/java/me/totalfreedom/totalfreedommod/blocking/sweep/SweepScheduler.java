package me.totalfreedom.totalfreedommod.blocking.sweep;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;

public class SweepScheduler extends FreedomService
{

    private static final long DRAIN_BUDGET_NANOS = 5_000_000L;

    private final Map<Integer, Registered> registry = new LinkedHashMap<>();
    private final Deque<Job> pending = new ArrayDeque<>();
    private final Set<String> pendingKeys = new HashSet<>();

    private int nextVisitorId;
    private int drainTaskId = -1;
    private boolean started;

    public SweepScheduler(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        started = true;
        ensureDrainTask();
        for (Registered registered : registry.values())
        {
            activate(registered);
        }
        FLog.info("[SweepScheduler] active [budget=5ms/tick]");
    }

    @Override
    protected void onStop()
    {
        started = false;
        if (drainTaskId != -1)
        {
            server.getScheduler().cancelTask(drainTaskId);
            drainTaskId = -1;
        }
        for (Registered registered : registry.values())
        {
            if (registered.periodicTaskId != -1)
            {
                server.getScheduler().cancelTask(registered.periodicTaskId);
                registered.periodicTaskId = -1;
            }
        }
        registry.clear();
        pending.clear();
        pendingKeys.clear();
    }

    public void register(EntityVisitor visitor)
    {
        Objects.requireNonNull(visitor);
        Registered registered = new Registered(nextVisitorId++, visitor::enabled, visitor.sweepIntervalTicks(),
                (chunk, context) ->
                {
                    Entity[] entities;
                    try
                    {
                        entities = chunk.getEntities();
                    }
                    catch (Throwable ignored)
                    {
                        return;
                    }
                    for (Entity entity : entities)
                    {
                        visitor.visit(entity, context);
                    }
                });
        registry.put(registered.id, registered);
        if (started)
        {
            activate(registered);
        }
    }

    public void register(TileEntityVisitor visitor)
    {
        Objects.requireNonNull(visitor);
        Registered registered = new Registered(nextVisitorId++, visitor::enabled, visitor.sweepIntervalTicks(),
                (chunk, context) ->
                {
                    Collection<BlockState> states;
                    try
                    {
                        states = chunk.getTileEntities(visitor.blockFilter(), false);
                    }
                    catch (Throwable ignored)
                    {
                        try
                        {
                            states = Arrays.asList(chunk.getTileEntities(false));
                        }
                        catch (Throwable ignoredAgain)
                        {
                            return;
                        }
                    }
                    for (BlockState state : states)
                    {
                        if (state != null && visitor.blockFilter().test(state.getBlock()))
                        {
                            visitor.visit(state, context);
                        }
                    }
                });
        registry.put(registered.id, registered);
        if (started)
        {
            activate(registered);
        }
    }

    private void activate(Registered registered)
    {
        if (!registered.enabled.getAsBoolean())
        {
            return;
        }
        enqueueAllLoaded(registered, SweepContext.INITIAL);
        if (registered.intervalTicks > 0L)
        {
            registered.periodicTaskId = server.getScheduler().runTaskTimer(plugin,
                    () -> enqueueAllLoaded(registered, SweepContext.PERIODIC),
                    registered.intervalTicks,
                    registered.intervalTicks).getTaskId();
        }
    }

    private void enqueueAllLoaded(Registered registered, SweepContext context)
    {
        if (!started || !registered.enabled.getAsBoolean())
        {
            return;
        }
        for (World world : server.getWorlds())
        {
            for (Chunk chunk : world.getLoadedChunks())
            {
                enqueue(registered, chunk, context);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event)
    {
        if (!started || event.isNewChunk())
        {
            return;
        }
        Chunk chunk = event.getChunk();
        server.getScheduler().runTask(plugin, () ->
        {
            if (!chunk.isLoaded())
            {
                return;
            }
            for (Registered registered : registry.values())
            {
                if (registered.enabled.getAsBoolean())
                {
                    enqueue(registered, chunk, SweepContext.CHUNK_LOAD);
                }
            }
        });
    }

    private void enqueue(Registered registered, Chunk chunk, SweepContext context)
    {
        if (chunk == null || !chunk.isLoaded())
        {
            return;
        }
        String key = registered.id + ":" + chunk.getWorld().getUID() + ":" + chunk.getX() + ":" + chunk.getZ();
        if (!pendingKeys.add(key))
        {
            return;
        }
        pending.add(new Job(registered.id, chunk, key, context));
        ensureDrainTask();
    }

    private void ensureDrainTask()
    {
        if (!started || drainTaskId != -1)
        {
            return;
        }
        drainTaskId = server.getScheduler().runTaskTimer(plugin, this::drain, 1L, 1L).getTaskId();
    }

    private void drain()
    {
        if (pending.isEmpty())
        {
            return;
        }
        long deadline = System.nanoTime() + DRAIN_BUDGET_NANOS;
        do
        {
            Job job = pending.poll();
            if (job == null)
            {
                return;
            }
            pendingKeys.remove(job.key);
            Registered registered = registry.get(job.visitorId);
            if (registered != null && registered.enabled.getAsBoolean())
            {
                Chunk chunk = job.chunk;
                if (chunk != null && chunk.isLoaded())
                {
                    try
                    {
                        registered.applier.apply(chunk, job.context);
                    }
                    catch (Throwable t)
                    {
                        FLog.warning("[SweepScheduler] Visitor failed in " + job.context.label() + ": " + t.getMessage());
                    }
                }
            }
        }
        while (!pending.isEmpty() && System.nanoTime() < deadline);
    }

    @FunctionalInterface
    private interface ChunkApplier
    {
        void apply(Chunk chunk, SweepContext context);
    }

    private static final class Registered
    {
        private final int id;
        private final BooleanSupplier enabled;
        private final long intervalTicks;
        private final ChunkApplier applier;
        private int periodicTaskId = -1;

        private Registered(int id, BooleanSupplier enabled, long intervalTicks, ChunkApplier applier)
        {
            this.id = id;
            this.enabled = enabled;
            this.intervalTicks = intervalTicks;
            this.applier = applier;
        }
    }

    private static final class Job
    {
        private final int visitorId;
        private final Chunk chunk;
        private final String key;
        private final SweepContext context;

        private Job(int visitorId, Chunk chunk, String key, SweepContext context)
        {
            this.visitorId = visitorId;
            this.chunk = chunk;
            this.key = key;
            this.context = context;
        }
    }
}
