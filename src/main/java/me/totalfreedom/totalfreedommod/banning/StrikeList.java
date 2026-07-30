package me.totalfreedom.totalfreedommod.banning;

import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.sql.adapter.StrikeRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.JsonUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class StrikeList extends FreedomService
{
    private static final Type STRIKE_MAP_TYPE = new TypeToken<Map<String, StrikeRecord>>() {}.getType();
    private static final long SHUTDOWN_FLUSH_TIMEOUT_MS = 10L * 1000L;

    private final Map<String, StrikeRecord> strikes = Maps.newHashMap();
    private final File configFile;
    private final Object persistenceLock = new Object();

    private boolean usingSql = false;
    private boolean persistEnabled = true;
    private Mono<Void> persistenceChain = Mono.empty();

    public StrikeList(TotalFreedomMod plugin)
    {
        super(plugin);
        this.configFile = new File(plugin.getDataFolder(), "strikes.json");
    }

    @Override
    protected void onStart()
    {
        strikes.clear();
        usingSql = false;

        final Boolean persist = ConfigEntry.AUTOEJECT_PERSIST.getBoolean();
        persistEnabled = persist == null || persist;

        if (!persistEnabled)
        {
            FLog.info("Strike persistence disabled; running in-memory.");
            return;
        }

        if (plugin.dm != null && plugin.dm.isInitialized())
        {
            loadFromSql();
        }
        else
        {
            loadFromJson();
        }

        pruneDecayed();
        FLog.info("Loaded " + strikes.size() + " strike records.");
    }

    @Override
    protected void onStop()
    {
        if (!persistEnabled) 
            return;

        awaitPendingWrites(SHUTDOWN_FLUSH_TIMEOUT_MS);

        if (!usingSql) 
            saveToJson();
        
    }

    /**
     * Re-run the startup load. Used after a one-time YAML-to-SQL migration so this manager's
     * in-memory state picks up the freshly-migrated rows.
     */
    public void reload()
    {
        onStop();
        onStart();
    }

    /**
     * Wait for queued async writes to land, up to {@code timeoutMs}. Without this a shutdown
     * flush can race the queue and let an older queued snapshot overwrite the state we just wrote.
     */
    public void awaitPendingWrites(long timeoutMs)
    {
        final Mono<Void> pending;
        synchronized (persistenceLock)
        {
            pending = persistenceChain;
        }

        try
        {
            pending.block(Duration.ofMillis(timeoutMs));
        }
        catch (IllegalStateException ex)
        {
            FLog.warning(String.format("Gave up after %dms waiting for pending strike writes (%s); flushing anyway",
                    timeoutMs, ex.getMessage()));
        }
        catch (RuntimeException ex)
        {
            FLog.warning("A queued strike write failed before shutdown: " + ex.getMessage());
        }
    }

    /**
     * Append {@code work} to the persistence chain and subscribe. Every queued write runs
     * after the one before it, so a decay-prune can never race a concurrent re-strike on the
     * same IP into landing out of order.
     */
    private void enqueue(Mono<Void> work)
    {
        synchronized (persistenceLock)
        {
            final Mono<Void> queued = persistenceChain
                    .onErrorResume(ignored -> Mono.empty())
                    .then(work)
                    .cache();

            persistenceChain = queued;
            queued.doFinally(signal -> collapseChain(queued)).subscribe();
        }
    }

    /**
     * Drop the retained operator chain once its tail has completed. The chain is strictly
     * sequential, so a completed tail means every write before it is done and nothing needs
     * to wait on them any more.
     */
    private void collapseChain(Mono<Void> completed)
    {
        synchronized (persistenceLock)
        {
            if (persistenceChain == completed)
                persistenceChain = Mono.empty();
        }
    }

    private Mono<Void> writeJsonAsync()
    {
        return Mono.<Void>fromRunnable(this::saveToJson)
                   .subscribeOn(Schedulers.boundedElastic());
    }

    private void loadFromSql()
    {
        try
        {
            StrikeRepository repo = plugin.dm.getStrikeRepository();
            Map<String, StrikeRecord> loaded = repo.loadAllAsync().block();
            strikes.putAll(loaded);
            usingSql = true;

            reconcileFromJsonIfNewer(repo);
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to load strikes from SQL, falling back to JSON: " + ex.getMessage());
            loadFromJson();
        }
    }

    /**
     * If strikes.json was written more recently than the database's last update, re-import it into SQL.
     */
    private void reconcileFromJsonIfNewer(StrikeRepository repo)
    {
        if (!configFile.exists())
            return;

        try
        {
            Long sqlUpdatedAt = repo.getMaxUpdatedAt();
            if (sqlUpdatedAt != null && configFile.lastModified() <= sqlUpdatedAt)
                return;

            Map<String, StrikeRecord> jsonStrikes = readJsonStrikes();
            if (jsonStrikes.isEmpty())
                return;

            FLog.info("strikes.json is newer than the database; re-importing " + jsonStrikes.size() + " strike record(s) from it.");
            for (StrikeRecord r : jsonStrikes.values())
                repo.upsertAsync(r).block();

            strikes.clear();
            strikes.putAll(jsonStrikes);
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to reconcile strikes.json into the database: " + ex.getMessage());
        }
    }

    private Map<String, StrikeRecord> readJsonStrikes() throws IOException
    {
        try (FileReader reader = new FileReader(configFile))
        {
            Map<String, StrikeRecord> loaded = JsonUtil.GSON.fromJson(reader, STRIKE_MAP_TYPE);
            return loaded != null ? loaded : Maps.newHashMap();
        }
    }

    private void loadFromJson()
    {
        if (!configFile.exists())
        {
            try
            {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }
            catch (IOException ex)
            {
                FLog.severe("Could not create strikes.json");
            }
        }

        try
        {
            strikes.putAll(readJsonStrikes());
        }
        catch (IOException ex)
        {
            FLog.severe("Could not read strikes.json: " + ex.getMessage());
        }
        usingSql = false;
    }

    private void pruneDecayed()
    {
        final int decayHours = decayHours();
        if (decayHours <= 0)
            return;

        final List<String> prunedIps = new ArrayList<>();
        for (Iterator<Map.Entry<String, StrikeRecord>> it = strikes.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<String, StrikeRecord> e = it.next();
            if (e.getValue().effectiveCount(decayHours) == 0)
            {
                it.remove();
                prunedIps.add(e.getKey());
            }
        }

        if (prunedIps.isEmpty())
            return;

        if (usingSql && plugin.dm != null && plugin.dm.isInitialized())
        {
            final StrikeRepository repo = plugin.dm.getStrikeRepository();
            enqueue(Flux.fromIterable(prunedIps)
                        .concatMap(ip -> repo.deleteByIpAsync(ip)
                                             .onErrorResume(ex ->
                                             {
                                                 FLog.warning(String.format(
                                                                            "Failed to prune decayed strike for %s: %s", 
                                                                            ip, 
                                                                            ex.getMessage()
                                                                            )
                                                                );
                                                 return Mono.<Boolean>empty();
                                             })
                                    )
                        .then(writeJsonAsync()));
        }
        else
        {
            enqueue(writeJsonAsync());
        }

        FLog.info("Pruned " + prunedIps.size() + " decayed strike record(s).");
    }

    private int decayHours()
    {
        final Integer h = ConfigEntry.AUTOEJECT_TIME_WINDOW.getInteger();
        return h == null ? 0 : h;
    }

    public synchronized int recordStrikeAndGet(String ip, String username)
    {
        StrikeRecord r = strikes.get(ip);
        final int decay = decayHours();
        int base = 0;
        if (r != null)
            base = r.effectiveCount(decay);
        
        if (r == null)
        {
            r = new StrikeRecord(ip);
            strikes.put(ip, r);
        }

        r.setCount(base + 1);
        r.setLastStrikeUnix(System.currentTimeMillis() / 1000L);
        if (username != null)
            r.setLastUsername(username);

        if (persistEnabled)
            persist(r);

        return r.getCount();
    }

    public synchronized int peek(String ip)
    {
        StrikeRecord r = strikes.get(ip);
        if (r == null)
            return 0;

        return r.effectiveCount(decayHours());
    }

    public synchronized boolean clear(String ip)
    {
        StrikeRecord removed = strikes.remove(ip);
        if (removed == null)
            return false;

        if (!persistEnabled)
            return true;

        if (usingSql && plugin.dm != null && plugin.dm.isInitialized())
        {
            final StrikeRepository repo = plugin.dm.getStrikeRepository();
            enqueue(repo.deleteByIpAsync(ip)
                        .onErrorResume(ex ->
                        {
                            FLog.warning("Failed to clear strike from SQL: " + ex.getMessage());
                            return Mono.<Boolean>empty();
                        })
                        .then(writeJsonAsync()));
        }
        else enqueue(writeJsonAsync()); // else on same line maybe??? that is nice tbh

        return true;
    }

    public synchronized Map<String, StrikeRecord> snapshot()
    {
        return Collections.unmodifiableMap(new HashMap<>(strikes));
    }

    private void persist(StrikeRecord r)
    {
        if (usingSql && plugin.dm != null && plugin.dm.isInitialized())
        {
            final StrikeRepository repo = plugin.dm.getStrikeRepository();
            enqueue(repo.upsertAsync(r)
                        .onErrorResume(ex ->
                        {
                            FLog.warning("Failed to persist strike to SQL: " + ex.getMessage());
                            return Mono.empty();
                        })
                        .then(writeJsonAsync()));
        }
        else enqueue(writeJsonAsync());
    }

    private synchronized void saveToJson()
    {
        try (FileWriter writer = new FileWriter(configFile))
        {
            JsonUtil.GSON.toJson(strikes, STRIKE_MAP_TYPE, writer);
        }
        catch (IOException ex)
        {
            FLog.severe("Could not save strikes.json");
        }
    }
}
