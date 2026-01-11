package me.totalfreedom.totalfreedommod.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.sql.SQLProperties.DatabaseType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.totalfreedom.totalfreedommod.util.FLog;

/**
 * Handles database connections for all supported database types.
 * Supports: SQLite, MySQL, MariaDB, PostgreSQL, H2, MongoDB, Redis
 */
public class ConnectionHandler
{
    private final TotalFreedomMod plugin;
    private final SQLProperties sqlProperties;
    private Connection connection = null;
    private final ExecutorService dbExecutor;
    
    // For NoSQL databases, we'll store the client reference
    private Object noSqlClient = null;

    public ConnectionHandler(@NotNull final TotalFreedomMod plugin)
    {
        this.plugin = plugin;
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
     * Get or create a JDBC connection for SQL databases.
     * For NoSQL databases (MongoDB, Redis), this will throw an exception.
     */
    @NotNull
    public CompletableFuture<Connection> getConnection()
    {
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                DatabaseType dbType = sqlProperties.getDatabaseType();
                
                // Check if this is a NoSQL database
                if (dbType.isNoSQL())
                {
                    throw new SQLException("Database type " + dbType.getName() + 
                        " is a NoSQL database and does not use JDBC connections. " +
                        "Use getNoSqlClient() instead.");
                }

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
     * Get NoSQL client for MongoDB or Redis.
     * Returns null for SQL databases.
     * 
     * Note: Implementation depends on the NoSQL driver being used.
     * This method provides a placeholder for NoSQL client initialization.
     */
    @Nullable
    public CompletableFuture<Object> getNoSqlClient()
    {
        return CompletableFuture.supplyAsync(() -> {
            DatabaseType dbType = sqlProperties.getDatabaseType();
            
            if (!dbType.isNoSQL())
            {
                FLog.warning("getNoSqlClient() called for SQL database type: " + dbType.getName());
                return null;
            }

            if (noSqlClient != null)
            {
                return noSqlClient;
            }

            String url = sqlProperties.getJdbcUrl();
            FLog.info("Connecting to NoSQL database: " + dbType.getName());

            switch (dbType)
            {
                case MONGODB:
                    // MongoDB client initialization
                    // Requires: org.mongodb:mongodb-driver-sync dependency
                    // noSqlClient = MongoClients.create(url);
                    FLog.warning("MongoDB support requires mongodb-driver-sync dependency. " +
                        "Add implementation 'org.mongodb:mongodb-driver-sync:4.11.1' to build.gradle");
                    throw new UnsupportedOperationException(
                        "MongoDB client not implemented. Add MongoDB driver dependency.");

                case REDIS:
                    // Redis client initialization
                    // Requires: redis.clients:jedis dependency
                    // noSqlClient = new JedisPool(host, port);
                    FLog.warning("Redis support requires jedis dependency. " +
                        "Add implementation 'redis.clients:jedis:5.1.0' to build.gradle");
                    throw new UnsupportedOperationException(
                        "Redis client not implemented. Add Jedis driver dependency.");

                default:
                    throw new IllegalStateException("Unknown NoSQL type: " + dbType);
            }
        }, dbExecutor);
    }

    /**
     * Check if the current database configuration uses JDBC.
     */
    public boolean isJdbcDatabase()
    {
        return sqlProperties.isJdbcDatabase();
    }

    /**
     * Check if the current database configuration is NoSQL.
     */
    public boolean isNoSQL()
    {
        return sqlProperties.isNoSQL();
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

        // Close NoSQL client
        if (noSqlClient != null)
        {
            try
            {
                // MongoDB: ((MongoClient) noSqlClient).close();
                // Redis: ((JedisPool) noSqlClient).close();
                FLog.info("NoSQL client closed.");
            }
            catch (Exception e)
            {
                FLog.warning("Failed to close NoSQL client: " + e.getMessage());
            }
            noSqlClient = null;
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
                if (isNoSQL())
                {
                    // For NoSQL, just check if we can get a client
                    getNoSqlClient().join();
                    return true;
                }
                else
                {
                    Connection conn = getConnection().join();
                    return conn != null && !conn.isClosed() && conn.isValid(5);
                }
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