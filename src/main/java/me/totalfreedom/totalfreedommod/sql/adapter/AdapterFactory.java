package me.totalfreedom.totalfreedommod.sql.adapter;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.sql.ConnectionHandler;
import me.totalfreedom.totalfreedommod.sql.SQLProperties;
import me.totalfreedom.totalfreedommod.sql.StatementHandler;
import me.totalfreedom.totalfreedommod.sql.adapter.h2.H2Adapter;
import me.totalfreedom.totalfreedommod.sql.adapter.mysql.MySQLAdapter;
import me.totalfreedom.totalfreedommod.sql.adapter.postgresql.PostgreSQLAdapter;
import me.totalfreedom.totalfreedommod.sql.adapter.sqlite.SQLiteAdapter;
import me.totalfreedom.totalfreedommod.util.FLog;

/**
 * Factory class for creating database adapters based on the configured database type.
 * This factory handles the instantiation of the correct adapter implementation
 * based on the DatabaseType enum from SQLProperties.
 * 
 * Supported SQL databases:
 * - SQLite (default, embedded)
 * - MySQL (standalone server)
 * - MariaDB (MySQL-compatible, uses MySQL adapter)
 * - PostgreSQL (standalone server)
 * - H2 (embedded, MySQL-compatible)
 * 
 * NoSQL databases (MongoDB, Redis) are not supported by this factory
 * and require separate handling through ConnectionHandler.getNoSqlClient().
 */
public final class AdapterFactory
{
    private AdapterFactory()
    {
        // Prevent instantiation
    }

    /**
     * Creates a DatabaseAdapter based on the configured database type.
     *
     * @param plugin The TotalFreedomMod plugin instance
     * @param properties The SQL properties containing database configuration
     * @param connectionHandler The connection handler for database connections
     * @param statementHandler The statement handler for executing queries
     * @return The appropriate DatabaseAdapter for the configured database type
     * @throws UnsupportedOperationException if the database type is not supported
     */
    public static DatabaseAdapter createAdapter(
            TotalFreedomMod plugin,
            SQLProperties properties,
            ConnectionHandler connectionHandler,
            StatementHandler statementHandler)
    {
        SQLProperties.DatabaseType type = properties.getDatabaseType();

        FLog.info("Creating database adapter for type: " + type.name());

        return switch (type)
        {
            case SQLITE -> new SQLiteAdapter(plugin, connectionHandler, statementHandler);
            case MYSQL -> new MySQLAdapter(plugin, connectionHandler, statementHandler);
            case MARIADB -> {
                FLog.info("MariaDB selected, using MySQL adapter (compatible)");
                yield new MySQLAdapter(plugin, connectionHandler, statementHandler);
            }
            case POSTGRESQL -> new PostgreSQLAdapter(plugin, connectionHandler, statementHandler);
            case H2 -> new H2Adapter(plugin, connectionHandler, statementHandler);
            case MONGODB, REDIS -> throw new UnsupportedOperationException(
                    "NoSQL database '" + type.name() + "' is not supported by the SQL adapter system. " +
                    "Use ConnectionHandler.getNoSqlClient() for NoSQL operations.");
        };
    }

    /**
     * Checks if the given database type is a SQL database.
     *
     * @param type The database type to check
     * @return true if the database type is SQL, false if NoSQL
     */
    public static boolean isSqlDatabase(SQLProperties.DatabaseType type)
    {
        return switch (type)
        {
            case SQLITE, MYSQL, MARIADB, POSTGRESQL, H2 -> true;
            case MONGODB, REDIS -> false;
        };
    }

    /**
     * Gets the recommended adapter for a given database type.
     * Useful for determining compatibility.
     *
     * @param type The database type
     * @return The class of the adapter that will be used
     */
    public static Class<? extends DatabaseAdapter> getAdapterClass(SQLProperties.DatabaseType type)
    {
        return switch (type)
        {
            case SQLITE -> SQLiteAdapter.class;
            case MYSQL, MARIADB -> MySQLAdapter.class;
            case POSTGRESQL -> PostgreSQLAdapter.class;
            case H2 -> H2Adapter.class;
            case MONGODB, REDIS -> null;
        };
    }
}
