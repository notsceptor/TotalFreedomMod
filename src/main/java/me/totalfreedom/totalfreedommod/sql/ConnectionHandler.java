package me.totalfreedom.totalfreedommod.sql;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.sql.SQLProperties.DatabaseType;
import me.totalfreedom.totalfreedommod.util.FLog;

/**
 * Contains the HikariCP connection pool for the configured database.
 * Supports: SQLite, MySQL, PostgreSQL
 */
public class ConnectionHandler
{
    private static final int DEFAULT_POOL_SIZE = 10;
    private static final int SQLITE_POOL_SIZE = 1;

    private final SQLProperties sqlProperties;
    private volatile HikariDataSource dataSource;
    private volatile AccessController accessController;

    public ConnectionHandler(@NotNull final TotalFreedomMod plugin)
    {
        this.sqlProperties = new SQLProperties(plugin);
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
     * Build the HikariCP connection pool for the configured database. 
     * Blocks until Hikari's own initial-connection test succeeds or fails.
     *
     * @apiNote SQLite is a single-writer embedded engine, so it is pinned to a single pooled connection regardless of configuration.
     */
    public void connect() throws SQLException
    {
        DatabaseType dbType = sqlProperties.getDatabaseType();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(sqlProperties.getJdbcUrl());
        config.setPoolName("TFM-" + dbType.getName());

        String driverClass = sqlProperties.getDriverClass();
        if (driverClass != null && !driverClass.isEmpty())
        {
            config.setDriverClassName(driverClass);
        }

        if (sqlProperties.hasCredentials())
        {
            config.setUsername(sqlProperties.getUsername());
            config.setPassword(sqlProperties.getPassword());
        }

        if (dbType == DatabaseType.SQLITE)
        {
            config.setMaximumPoolSize(SQLITE_POOL_SIZE);
            config.setConnectionTestQuery("SELECT 1");
            // SQLite pragmas are set per-connection so we need to make sure it doesn't accidentally get recycled
            config.setMaxLifetime(0);
        }
        else
        {
            config.setMaximumPoolSize(DEFAULT_POOL_SIZE);
            config.setMinimumIdle(1);
        }

        try
        {
            FLog.info(String.format(
                                    "Connecting to database: %s at %s", 
                                    dbType.getName(), 
                                    maskPassword(config.getJdbcUrl())
                                ));
            this.dataSource = new HikariDataSource(config);
            this.accessController = new AccessController(dataSource.getMaximumPoolSize());

            if (dbType == DatabaseType.SQLITE)
            {
                try (Connection connection = dataSource.getConnection())
                {
                    sqlProperties.applySqlitePragmas(connection);
                }
                catch (SQLException e)
                {
                    throw e; // this should be handled downstream, not here.
                }
                catch (Exception e)
                {
                    throw new SQLException("Failed to apply SQLite pragmas", e); // this should also be handled downstream.
                }
            }

            FLog.info(String.format(
                                    "Database connection pool established (%s, poolSize %d)", 
                                    dbType.getName(), 
                                    dataSource.getMaximumPoolSize()
                                ));
        }
        catch (SQLException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new SQLException("Failed to initialize database connection pool", e);
        }
    }

    /**
     * Borrow a pooled connection. Callers are responsible for closing it, which
     * returns it to the pool rather than actually closing the physical connection.
     */
    @NotNull
    public Connection borrowConnection() throws SQLException
    {
        if (dataSource == null)
        {
            throw new IllegalStateException("Connection pool not initialized. Call connect() first.");
        }
        return dataSource.getConnection();
    }

    /**
     * Semaphore holder which limits concurrent async queries to the pool's maximum connection count.
     */
    @NotNull
    public AccessController getAccessController()
    {
        if (accessController == null)
        {
            throw new IllegalStateException("Connection pool not initialized. Call connect() first.");
        }
        return accessController;
    }

    @NotNull
    public DatabaseType getDatabaseType()
    {
        return sqlProperties.getDatabaseType();
    }

    public boolean isConnected()
    {
        return dataSource != null && !dataSource.isClosed();
    }

    public boolean testConnection()
    {
        try (Connection connection = borrowConnection())
        {
            return connection.isValid(5);
        }
        catch (Exception e)
        {
            FLog.severe(String.format(
                                    "Database connection test failed: %s", 
                                    ExceptionUtils.getRootCauseMessage(e)
                                ));
            return false;
        }
    }

    public void shutdown()
    {
        FLog.info("Shutting down database connection handler...");
        if (dataSource != null && !dataSource.isClosed())
        {
            dataSource.close();
            FLog.info("Database connection pool closed.");
        }
    }

    private String maskPassword(String url)
    {
        return url.replaceAll(":[^:@/]+@", ":****@")
                  .replaceAll("password=[^&;]+", "password=****");
    }
}
