package me.totalfreedom.totalfreedommod.banning;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.sql.adapter.PermbanRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.totalfreedommod.util.JsonUtil;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class PermbanList extends FreedomService
{

    public static final String CONFIG_FILENAME = "permbans.json";
    private static final Type PERMBAN_MAP_TYPE = new TypeToken<Map<String, PermBan>>() {}.getType();
    private static final long SHUTDOWN_FLUSH_TIMEOUT_MS = 10L * 1000L;

    private final Set<String> permbannedNames = Sets.newHashSet();
    private final Set<String> permbannedIps = Sets.newHashSet();
    private final Map<String, PermBan> permbansByName = Maps.newHashMap();
    private final File configFile;
    private final Object lock = new Object();
    private final Object persistenceLock = new Object();

    private Mono<Void> persistenceChain = Mono.empty();
    private boolean usingSql = false;

    public PermbanList(TotalFreedomMod plugin)
    {
        super(plugin);
        this.configFile = new File(plugin.getDataFolder(), CONFIG_FILENAME);
    }

    @Override
    protected void onStart()
    {
        if (plugin.dm != null && plugin.dm.isInitialized())
            loadFromSql();
        else
            loadFromJson();
    }

    /**
     * Load permbans from SQL database.
     */
    private void loadFromSql()
    {
        try
        {
            PermbanRepository repo = plugin.dm.getPermbanRepository();
            List<PermBan> loadedPermbans = repo.findAll().block();

            synchronized (lock)
            {
                permbannedNames.clear();
                permbannedIps.clear();
                permbansByName.clear();

                for (PermBan permban : loadedPermbans)
                {
                    String name = permban.getUsername().toLowerCase().trim();
                    permbannedNames.add(name);
                    permbannedIps.addAll(permban.getIps());
                    permbansByName.put(name, permban);
                }

                usingSql = true;
                FLog.info("Loaded " + permbannedIps.size() + " perm IP bans and " + permbannedNames.size() + " perm username bans from SQL database.");
            }

            reconcileFromJsonIfNewer(repo);
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to load permbans from SQL, falling back to JSON: " + ex.getMessage());
            loadFromJson();
        }
    }

    /**
     * If permbans.json was written more recently than the database's last update, re-import it into SQL.
     */
    private void reconcileFromJsonIfNewer(PermbanRepository repo)
    {
        if (!configFile.exists())
            return;

        try
        {
            Long sqlUpdatedAt = repo.getMaxUpdatedAt();
            if (sqlUpdatedAt != null && configFile.lastModified() <= sqlUpdatedAt)
                return;

            Map<String, PermBan> jsonPermbans = readJsonPermbans();
            if (jsonPermbans.isEmpty())
                return;

            FLog.info("permbans.json is newer than the database; re-importing " + jsonPermbans.size() + " permban(s) from it.");
            for (PermBan permban : jsonPermbans.values())
                repo.save(permban).block();

            synchronized (lock)
            {
                permbannedNames.clear();
                permbannedIps.clear();
                permbansByName.clear();
                for (PermBan permban : jsonPermbans.values())
                {
                    String name = permban.getUsername().toLowerCase().trim();
                    permbannedNames.add(name);
                    permbannedIps.addAll(permban.getIps());
                    permbansByName.put(name, permban);
                }
            }
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to reconcile permbans.json into the database: " + ex.getMessage());
        }
    }

    private Map<String, PermBan> readJsonPermbans() throws IOException
    {
        try (FileReader reader = new FileReader(configFile))
        {
            Map<String, PermBan> loaded = JsonUtil.GSON.fromJson(reader, PERMBAN_MAP_TYPE);
            return loaded != null ? loaded : Maps.newHashMap();
        }
    }

    /**
     * Load permbans from the JSON file (fallback).
     */
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
                FLog.severe("Could not create " + CONFIG_FILENAME);
            }
        }

        synchronized (lock)
        {
            permbannedNames.clear();
            permbannedIps.clear();
            permbansByName.clear();

            try
            {
                for (Map.Entry<String, PermBan> entry : readJsonPermbans().entrySet())
                {
                    PermBan permban = entry.getValue();
                    permbannedNames.add(entry.getKey());
                    permbannedIps.addAll(permban.getIps());
                    permbansByName.put(entry.getKey(), permban);
                }
            }
            catch (IOException ex)
            {
                FLog.severe("Could not read " + CONFIG_FILENAME + ": " + ex.getMessage());
            }

            usingSql = false;
            FLog.info("Loaded " + permbannedIps.size() + " perm IP bans and " + permbannedNames.size() + " perm username bans from JSON.");
        }
    }

    private void saveToJson()
    {
        final Map<String, PermBan> snapshot;
        synchronized (lock)
        {
            snapshot = Maps.newHashMap(permbansByName);
        }

        try (FileWriter writer = new FileWriter(configFile))
        {
            JsonUtil.GSON.toJson(snapshot, PERMBAN_MAP_TYPE, writer);
        }
        catch (IOException ex)
        {
            FLog.severe("Could not save " + CONFIG_FILENAME);
        }
    }

    @Override
    protected void onStop()
    {
        awaitPendingWrites(SHUTDOWN_FLUSH_TIMEOUT_MS);

        if (usingSql)
            saveAllToSql();
        else
            saveToJson();
        
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
            FLog.warning(String.format("Gave up after %dms waiting for pending permban writes (%s); flushing anyway",
                    timeoutMs, ex.getMessage()));
        }
        catch (RuntimeException ex)
        {
            FLog.warning("A queued permban write failed before shutdown: " + ex.getMessage());
        }
    }

    /**
     * Append {@code work} to the persistence chain and subscribe. Every queued write runs
     * after the one before it, so a batch can never be reordered behind a single-row update
     * that was requested later.
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

    private void collapseChain(Mono<Void> completed)
    {
        synchronized (persistenceLock)
        {
            if (persistenceChain == completed)
            {
                persistenceChain = Mono.empty();
            }
        }
    }

    private Mono<Void> writeJsonAsync()
    {
        return Mono.<Void>fromRunnable(this::saveToJson)
                   .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Blocking write of every permban record to SQL. Startup/shutdown only.
     */
    private void saveAllToSql()
    {
        if (plugin.dm == null || !plugin.dm.isInitialized())
            return;

        final List<PermBan> snapshot;
        synchronized (lock)
        {
            snapshot = new ArrayList<>(permbansByName.values());
        }

        try
        {
            PermbanRepository repo = plugin.dm.getPermbanRepository();
            for (PermBan permban : snapshot)
                repo.save(permban).block();

            FLog.debug("Saved " + snapshot.size() + " permbans to SQL database");
            saveToJson();
        }
        catch (Exception ex)
        {
            FLog.warning("Failed to save permbans to SQL: " + ex.getMessage());
        }
    }

    public void reload()
    {
        onStop();
        onStart();
    }
    
    /**
     * Add a permban.
     */
    public void addPermban(PermBan permban)
    {
        final boolean sql;
        synchronized (lock)
        {
            String name = permban.getUsername().toLowerCase().trim();
            permbannedNames.add(name);
            permbannedIps.addAll(permban.getIps());
            permbansByName.put(name, permban);
            sql = usingSql;
        }

        if (sql)
            savePermbanToSqlAsync(permban);
        else
            saveToJson();
    }

    /**
     * Remove a permban by username.
     */
    public boolean removePermban(String username)
    {
        final String name = username.toLowerCase().trim();
        final PermBan permban;
        final boolean sql;
        synchronized (lock)
        {
            permban = permbansByName.remove(name);
            if (permban == null)
                return false;

            permbannedNames.remove(name);
            permbannedIps.removeAll(permban.getIps());
            sql = usingSql;
        }

        if (sql)
            removePermbanFromSqlAsync(name);
        else
            saveToJson();

        return true;
    }

    /**
     * Remove every permban that has an IP matching the given address or range.
     */
    public List<String> removePermbansByIp(String ip)
    {
        final List<String> removedNames = new ArrayList<>();
        final boolean sql;
        synchronized (lock)
        {
            for (PermBan permban : new ArrayList<>(permbansByName.values()))
            {
                boolean matches = false;
                for (String banIp : permban.getIps())
                {
                    if (FUtil.fuzzyIpMatch(ip, banIp, 4))
                    {
                        matches = true;
                        break;
                    }
                }

                if (!matches)
                    continue;

                final String name = permban.getUsername().toLowerCase().trim();
                permbansByName.remove(name);
                permbannedNames.remove(name);
                removedNames.add(permban.getUsername());
            }

            // Rebuild the IP view so IPs shared with surviving permbans are retained.
            permbannedIps.clear();
            for (PermBan permban : permbansByName.values())
                permbannedIps.addAll(permban.getIps());

            sql = usingSql;
        }

        if (sql)
            for (String name : removedNames)
                removePermbanFromSqlAsync(name.toLowerCase().trim()); // why does this look so nice without brackets lmao

        else if (!removedNames.isEmpty())
            saveToJson();

        return removedNames;
    }

    /**
     * Get a permban by username.
     */
    public PermBan getPermban(String username)
    {
        synchronized (lock)
        {
            return permbansByName.get(username.toLowerCase().trim());
        }
    }

    /**
     * Returns a snapshot of the permbanned usernames.
     */
    public Set<String> getPermbannedNames()
    {
        synchronized (lock)
        {
            return new HashSet<>(permbannedNames);
        }
    }

    /**
     * Returns a snapshot of the permbanned IPs.
     */
    public Set<String> getPermbannedIps()
    {
        synchronized (lock)
        {
            return new HashSet<>(permbannedIps);
        }
    }

    /**
     * Queue a single permban for an off-thread SQL write, followed by a refresh of the JSON
     * snapshot. Safe to call from commands and event handlers.
     */
    private void savePermbanToSqlAsync(PermBan permban)
    {
        if (plugin.dm == null || !plugin.dm.isInitialized())
        {
            FLog.warning("SQL not available; permban change was not saved");
            return;
        }

        final PermbanRepository repo = plugin.dm.getPermbanRepository();

        enqueue(repo.save(permban)
                .onErrorResume(ex ->
                {
                    FLog.warning("Failed to save permban to SQL: " + ex.getMessage());
                    return Mono.<Integer>empty();
                })
                .then(writeJsonAsync()));
    }

    /**
     * Queue a single permban removal for an off-thread SQL write, followed by a refresh of
     * the JSON snapshot. Safe to call from commands and event handlers.
     */
    private void removePermbanFromSqlAsync(String name)
    {
        if (plugin.dm == null || !plugin.dm.isInitialized())
        {
            FLog.warning("SQL not available; permban removal was not saved");
            return;
        }

        final PermbanRepository repo = plugin.dm.getPermbanRepository();

        enqueue(Mono.fromCallable(() -> repo.deleteByUsername(name))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex ->
                {
                    FLog.warning("Failed to remove permban from SQL: " + ex.getMessage());
                    return Mono.<Boolean>empty();
                })
                .then(writeJsonAsync()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event)
    {
        final String username = event.getName();
        final String ip = event.getAddress().getHostAddress().trim();

        // Permbanned IPs
        for (String testIp : getPermbannedIps())
        {
            if (FUtil.fuzzyIpMatch(testIp, ip, 4))
            {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        Component.text("Your IP address is permanently banned from this server.\n"
                                + "Release procedures are available at\n", NamedTextColor.RED)
                                .append(Component.text(ConfigEntry.SERVER_PERMBAN_URL.getString(), NamedTextColor.GOLD)));
                return;
            }
        }

        // Permbanned usernames
        for (String testPlayer : getPermbannedNames())
        {
            if (testPlayer.equalsIgnoreCase(username))
            {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        Component.text("Your username is permanently banned from this server.\n"
                                + "Release procedures are available at\n", NamedTextColor.RED)
                                .append(Component.text(ConfigEntry.SERVER_PERMBAN_URL.getString(), NamedTextColor.GOLD)));
                return;
            }
        }

    }

}
