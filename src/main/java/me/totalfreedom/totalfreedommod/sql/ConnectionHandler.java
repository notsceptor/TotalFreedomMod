package me.totalfreedom.totalfreedommod.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.sql.SQLProperties.DatabaseType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.totalfreedom.totalfreedommod.util.FLog;

/**
 * Handles database connections for all supported database types.
 * Supports: SQLite, MySQL, PostgreSQL
 */
public class ConnectionHandler
{
    private final SQLProperties sqlProperties;
    private Connection connection = null;
    private final ExecutorService dbExecutor;

    public ConnectionHandler(@NotNull final TotalFreedomMod plugin)
    {
        this.sqlProperties = new SQLProperties(plugin);
        // Use a single-thread executor for DB operations to avoid blocking main thread
        this.dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "TFM-Database");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Get the SQLProperties configuration.
     */
    @NotNull
    public SQLProperties getSqlProperties()
    {
        return sqlProperties;
    }

    /**
     * Get or create a JDBC connection for the configured database.
     */
    @NotNull
    public CompletableFuture<Connection> getConnection()
    {
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                DatabaseType dbType = sqlProperties.getDatabaseType();

                if (connection == null || connection.isClosed())
                {
                    // Load JDBC driver if specified
                    String driverClass = sqlProperties.getDriverClass();
                    if (driverClass != null && !driverClass.isEmpty())
                    {
                        try
                        {
                            Class.forName(driverClass);
                        }
                        catch (ClassNotFoundException e)
                        {
                            FLog.warning("JDBC driver not found: " + driverClass + 
                                ". Attempting to connect anyway...");
                        }
                    }

                    String url = sqlProperties.getJdbcUrl();
                    FLog.info("Connecting to database: " + dbType.getName() + " at " + maskPassword(url));
                    
                    connection = DriverManager.getConnection(url, sqlProperties.getConnectionProperties());
                    
                    // Apply SQLite-specific pragmas if applicable
                    if (dbType == DatabaseType.SQLITE)
                    {
                        sqlProperties.applySqlitePragmas(connection);
                    }
                    
                    FLog.info("Database connection established (" + dbType.getName() + ")");
                }
                return connection;
            }
            catch (SQLException e)
            {
                throw new RuntimeException("Failed to get database connection", e);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Failed to initialize database", e);
            }
        }, dbExecutor);
    }

    /**
     * Get the configured database type.
     */
    @NotNull
    public DatabaseType getDatabaseType()
    {
        return sqlProperties.getDatabaseType();
    }

    /**
     * Shutdown the connection handler, closing all connections.
     */
    public void shutdown()
    {
        FLog.info("Shutting down database connection handler...");
        dbExecutor.shutdown();
        
        // Close JDBC connection
        if (connection != null)
        {
            try
            {
                connection.close();
                FLog.info("Database connection closed.");
            }
            catch (SQLException e)
            {
                FLog.warning("Failed to close database connection: " + e.getMessage());
            }
        }
    }

    /**
     * Test the database connection.
     */
    public CompletableFuture<Boolean> testConnection()
    {
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                Connection conn = getConnection().join();
                return conn != null && !conn.isClosed() && conn.isValid(5);
            }
            catch (Exception e)
            {
                FLog.severe("Database connection test failed: " + e.getMessage());
                return false;
            }
        }, dbExecutor);
    }

    /**
     * Mask password in connection URL for logging.
     */
    private String maskPassword(String url)
    {
        // Mask any password patterns like :password@ or password=xxx
        return url.replaceAll(":[^:@/]+@", ":****@")
                  .replaceAll("password=[^&;]+", "password=****");
    }

    public CompletableFuture<Void> closeConnection() {
        return CompletableFuture.runAsync(() -> {
            if (connection != null)
            {
                try
                {
                    connection.close();
                    FLog.info("Database connection closed.");
                }
                catch (SQLException e)
                {
                    FLog.warning("Failed to close database connection: " + e.getMessage());
                }
                connection = null;
            }
        }, dbExecutor);
    }
}