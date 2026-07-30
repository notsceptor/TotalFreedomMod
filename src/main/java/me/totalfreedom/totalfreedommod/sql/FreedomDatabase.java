package me.totalfreedom.totalfreedommod.sql;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.sql.SQLProperties.DatabaseType;
import me.totalfreedom.totalfreedommod.sql.adapter.AdapterFactory;
import me.totalfreedom.totalfreedommod.sql.adapter.AdminRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.BanRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.DatabaseAdapter;
import me.totalfreedom.totalfreedommod.sql.adapter.DiscordLinkRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.PermbanRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.PlayerRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.ProtectedAreaRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.RankRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.SavedFlagRepository;
import me.totalfreedom.totalfreedommod.sql.adapter.StrikeRepository;
import me.totalfreedom.totalfreedommod.util.FLog;

import java.sql.SQLException;
import java.time.Duration;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Central database management service.
 * Handles database initialization, adapter creation, and provides access to repositories.
 * 
 * This is the main entry point for database operations in TotalFreedomMod.
 * It initializes the appropriate database adapter based on configuration and
 * provides repository access for Admin, Ban, and Permban data.
 */
public class FreedomDatabase extends FreedomService
{
    /**
     * Upper bound on how long {@link #initialize()} waits for the connection pool and schema
     * migrations to finish. The actual work runs on a background thread either way.
     */
    private static final long INIT_TIMEOUT_SECONDS = 45L;

    private ConnectionHandler connectionHandler;
    private StatementHandler statementHandler;
    private DatabaseAdapter adapter;
    private boolean initialized = false;

    public FreedomDatabase(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        try
        {
            initialize();
        }
        catch (Exception ex)
        {
            FLog.severe("Failed to initialize database: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    protected void onStop()
    {
        shutdown();
    }

    /**
     * Initialize the database connection and adapter.
     * <p>
     * The actual connection-pool bootstrap and schema migration run on a background thread,
     * not the calling thread. This is called synchronously from {@link #onStart()} during
     * {@code onEnable}, which runs on the main thread, and a slow or unreachable host must
     * not hang the whole server boot. The wait is bounded by {@link #INIT_TIMEOUT_SECONDS};
     * if it elapses, this throws and every domain falls back to its JSON snapshot, same as
     * any other connection failure.
     */
    public void initialize() throws SQLException
    {
        if (initialized)
        {
            FLog.warning("DatabaseManager already initialized");
            return;
        }

        FLog.info("Initializing database...");

        connectionHandler = new ConnectionHandler(plugin);
        SQLProperties properties = connectionHandler.getSqlProperties();
        DatabaseType dbType = properties.getDatabaseType();

        final DatabaseAdapter built;
        try
        {
            built = Mono.fromCallable(() ->
            {
                connectionHandler.connect();
                statementHandler = new StatementHandler(connectionHandler);
                DatabaseAdapter created = AdapterFactory.createAdapter(plugin, properties, connectionHandler, statementHandler);
                created.initialize();
                return created;
            })
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(Duration.ofSeconds(INIT_TIMEOUT_SECONDS));
        }
        catch (Exception ex)
        {
            throw new SQLException(String.format(
                    "Database did not finish initializing within %ds", INIT_TIMEOUT_SECONDS), ex);
        }

        if (built == null)
        {
            throw new SQLException("Database initialization completed with no adapter");
        }

        adapter = built;
        initialized = true;
        FLog.info("Database initialized successfully (" + dbType.getName() + ")");
    }

    /**
     * Shutdown the database connection.
     */
    public void shutdown()
    {
        if (!initialized)
        {
            return;
        }

        FLog.info("Shutting down database...");

        if (adapter != null)
        {
            adapter.shutdown();
        }

        if (statementHandler != null)
        {
            statementHandler.close();
        }

        initialized = false;
        FLog.info("Database shutdown complete");
    }

    /**
     * Check if the database is initialized.
     */
    public boolean isInitialized()
    {
        return initialized;
    }

    /**
     * Get the database adapter.
     */
    public DatabaseAdapter getAdapter()
    {
        return adapter;
    }

    /**
     * Get the connection handler.
     */
    public ConnectionHandler getConnectionHandler()
    {
        return connectionHandler;
    }

    /**
     * Get the statement handler.
     */
    public StatementHandler getStatementHandler()
    {
        return statementHandler;
    }

    /**
     * Get the admin repository.
     */
    public AdminRepository getAdminRepository()
    {
        if (adapter == null)
        {
            throw new IllegalStateException("Database not initialized");
        }
        return adapter.getAdminRepository();
    }

    /**
     * Get the ban repository.
     */
    public BanRepository getBanRepository()
    {
        if (adapter == null)
        {
            throw new IllegalStateException("Database not initialized");
        }
        return adapter.getBanRepository();
    }

    /**
     * Get the permban repository.
     */
    public PermbanRepository getPermbanRepository()
    {
        if (adapter == null)
        {
            throw new IllegalStateException("Database not initialized");
        }
        return adapter.getPermbanRepository();
    }

    public StrikeRepository getStrikeRepository()
    {
        if (adapter == null)
        {
            throw new IllegalStateException("Database not initialized");
        }
        return adapter.getStrikeRepository();
    }

    public DiscordLinkRepository getDiscordLinkRepository()
    {
        if (adapter == null)
        {
            throw new IllegalStateException("Database not initialized");
        }
        return adapter.getDiscordLinkRepository();
    }

    public RankRepository getRankRepository()
    {
        if (adapter == null)
        {
            throw new IllegalStateException("Database not initialized");
        }
        return adapter.getRankRepository();
    }

    public ProtectedAreaRepository getProtectedAreaRepository()
    {
        if (adapter == null)
        {
            throw new IllegalStateException("Database not initialized");
        }
        return adapter.getProtectedAreaRepository();
    }

    public SavedFlagRepository getSavedFlagRepository()
    {
        if (adapter == null)
        {
            throw new IllegalStateException("Database not initialized");
        }
        return adapter.getSavedFlagRepository();
    }

    public PlayerRepository getPlayerRepository()
    {
        if (adapter == null)
        {
            throw new IllegalStateException("Database not initialized");
        }
        return adapter.getPlayerRepository();
    }

    /**
     * Snapshot of live connection pool and fairness-queue health, or {@code null} if the
     * database isn't initialized.
     */
    public ConnectionHandler.PoolStats getPoolStats()
    {
        if (!initialized || connectionHandler == null)
        {
            return null;
        }
        return connectionHandler.getPoolStats();
    }

    /**
     * Get the database type.
     */
    public DatabaseType getDatabaseType()
    {
        if (connectionHandler == null)
        {
            return DatabaseType.SQLITE; // Default
        }
        return connectionHandler.getSqlProperties().getDatabaseType();
    }
}
