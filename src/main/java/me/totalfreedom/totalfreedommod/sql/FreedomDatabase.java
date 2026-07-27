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
     */
    public void initialize() throws SQLException
    {
        if (initialized)
        {
            FLog.warning("DatabaseManager already initialized");
            return;
        }

        FLog.info("Initializing database...");

        // Create connection handler
        connectionHandler = new ConnectionHandler(plugin);

        // Check database type
        SQLProperties properties = connectionHandler.getSqlProperties();
        DatabaseType dbType = properties.getDatabaseType();

        // Build the connection pool
        connectionHandler.connect();

        // Create statement handler
        statementHandler = new StatementHandler(connectionHandler);

        // Create the adapter using the factory
        adapter = AdapterFactory.createAdapter(plugin, properties, connectionHandler, statementHandler);

        // Initialize the adapter (runs migrations)
        adapter.initialize();

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
