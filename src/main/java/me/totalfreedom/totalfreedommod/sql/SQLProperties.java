package me.totalfreedom.totalfreedommod.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.io.File;

import org.jetbrains.annotations.NotNull;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;

/**
 * Handles SQL database configuration properties.
 * Supports: sqlite, mysql, mariadb, postgresql, h2, mongodb, redis
 */
public final class SQLProperties 
{
    /**
     * Enum of supported database types with their default ports and JDBC info.
     */
    public enum DatabaseType
    {
        SQLITE("sqlite", 0, "org.sqlite.JDBC", false),
        MYSQL("mysql", 3306, "com.mysql.cj.jdbc.Driver", true),
        MARIADB("mariadb", 3306, "org.mariadb.jdbc.Driver", true),
        POSTGRESQL("postgresql", 5432, "org.postgresql.Driver", true),
        H2("h2", 9092, "org.h2.Driver", false),
        MONGODB("mongodb", 27017, "mongodb.jdbc.MongoDriver", true),
        REDIS("redis", 6379, null, true); // Redis doesn't use JDBC

        private final String name;
        private final int defaultPort;
        private final String driverClass;
        private final boolean requiresAuth;

        DatabaseType(String name, int defaultPort, String driverClass, boolean requiresAuth)
        {
            this.name = name;
            this.defaultPort = defaultPort;
            this.driverClass = driverClass;
            this.requiresAuth = requiresAuth;
        }

        public String getName() { return name; }
        public int getDefaultPort() { return defaultPort; }
        public String getDriverClass() { return driverClass; }
        public boolean requiresAuth() { return requiresAuth; }

        public static DatabaseType fromString(String type)
        {
            for (DatabaseType dt : values())
            {
                if (dt.name.equalsIgnoreCase(type))
                {
                    return dt;
                }
            }
            return SQLITE; // Default fallback
        }

        public boolean isNoSQL()
        {
            return this == MONGODB || this == REDIS;
        }

        public boolean isEmbedded()
        {
            return this == SQLITE || this == H2;
        }
    }

    private final DatabaseType databaseType;
    private final String host;
    private final int port;
    private final String databaseName;
    private final String username;
    private final String password;
    private final Map<String, String> additionalOptions;
    private final File propertiesFile;
    private final Properties sqlProperties;

    public SQLProperties(final TotalFreedomMod plugin)
    {
        try {
            this.propertiesFile = new File(plugin.getDataFolder(), "sql.properties");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.sqlProperties = new Properties();
        try {
            if (propertiesFile.exists()) {
                sqlProperties.load(new java.io.FileReader(propertiesFile));
            } else {
                propertiesFile.getParentFile().mkdirs();
                propertiesFile.createNewFile();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load SQL properties", e);
        }

        // Parse database type
        String typeStr = sqlProperties.getProperty("database.type", "sqlite").toLowerCase();
        this.databaseType = DatabaseType.fromString(typeStr);

        // Parse connection settings with type-aware defaults
        this.host = sqlProperties.getProperty("database.host", "localhost");
        this.port = Integer.parseInt(sqlProperties.getProperty("database.port", 
            String.valueOf(databaseType.getDefaultPort())));
        this.databaseName = sqlProperties.getProperty("database.name", 
            getDefaultDatabaseName(databaseType));

        // Authentication
        this.username = sqlProperties.getProperty("database.username", "");
        this.password = sqlProperties.getProperty("database.password", "");

        // Additional options
        this.additionalOptions = new HashMap<>();
        sqlProperties.forEach((key, value) -> {
            String keyStr = key.toString();
            if (keyStr.startsWith("database.option.")) {
                String optionKey = keyStr.substring("database.option.".length());
                additionalOptions.put(optionKey, value.toString());
            }
        });

        try {
            sqlProperties.store(new java.io.FileWriter(propertiesFile), "SQL Configuration Properties");
        } catch (Exception e) {
            throw new RuntimeException("Failed to save SQL properties", e);
        }
    }

    private String getDefaultDatabaseName(DatabaseType type)
    {
        return switch (type)
        {
            case SQLITE -> "totalfreedom.db";
            case H2 -> "./totalfreedom";
            case REDIS -> "0";
            default -> "totalfreedom";
        };
    }

    @NotNull
    public DatabaseType getDatabaseType()
    {
        return databaseType;
    }

    @NotNull
    public String getDatabaseTypeString()
    {
        return databaseType.getName();
    }

    @NotNull
    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    @NotNull
    public String getDatabaseName()
    {
        return databaseName;
    }

    @NotNull
    public String getUsername()
    {
        return username;
    }

    @NotNull
    public String getPassword()
    {
        return password;
    }

    public boolean hasCredentials()
    {
        return username != null && !username.isEmpty();
    }

    @NotNull
    public Map<String, String> getAdditionalOptions()
    {
        return additionalOptions;
    }

    @NotNull
    public Properties getSqlProperties()
    {
        return sqlProperties;
    }

    /**
     * Get connection properties for DriverManager.
     */
    @NotNull
    public Properties getConnectionProperties()
    {
        Properties props = new Properties();
        if (hasCredentials())
        {
            props.setProperty("user", username);
            props.setProperty("password", password);
        }
        // Add additional options as properties
        additionalOptions.forEach(props::setProperty);
        return props;
    }

    /**
     * Apply SQLite-specific PRAGMA statements to a connection.
     */
    public void applySqlitePragmas(@NotNull Connection connection) throws Exception
    {
        if (databaseType != DatabaseType.SQLITE)
        {
            return;
        }
        for (Map.Entry<String, String> entry : additionalOptions.entrySet())
        {
            String query = "PRAGMA " + entry.getKey() + " = ?";
            try (PreparedStatement statement = connection.prepareStatement(query))
            {
                statement.setString(1, entry.getValue());
                statement.execute();
            }
        }
    }

    /**
     * Build the JDBC URL for the configured database type.
     */
    @NotNull
    public String getJdbcUrl()
    {
        StringBuilder url = new StringBuilder("jdbc:");

        switch (databaseType)
        {
            case SQLITE:
                // jdbc:sqlite:path/to/database.db
                url.append("sqlite:").append(databaseName);
                break;

            case MYSQL:
                // jdbc:mysql://host:port/database
                url.append("mysql://").append(host).append(":").append(port).append("/").append(databaseName);
                appendOptions(url);
                break;

            case MARIADB:
                // jdbc:mariadb://host:port/database
                url.append("mariadb://").append(host).append(":").append(port).append("/").append(databaseName);
                appendOptions(url);
                break;

            case POSTGRESQL:
                // jdbc:postgresql://host:port/database
                url.append("postgresql://").append(host).append(":").append(port).append("/").append(databaseName);
                appendOptions(url);
                break;

            case H2:
                // jdbc:h2:./database (file) or jdbc:h2:mem:database (memory) or jdbc:h2:tcp://host:port/database (server)
                if (databaseName.startsWith("mem:") || databaseName.startsWith("./") || databaseName.startsWith("/"))
                {
                    url.append("h2:").append(databaseName);
                }
                else if (host != null && !host.isEmpty() && !host.equals("localhost") && port > 0)
                {
                    url.append("h2:tcp://").append(host).append(":").append(port).append("/").append(databaseName);
                }
                else
                {
                    url.append("h2:").append(databaseName);
                }
                appendOptions(url, ";");
                break;

            case MONGODB:
                // mongodb://host:port/database
                url.setLength(0); // Clear "jdbc:"
                url.append("mongodb://");
                if (hasCredentials())
                {
                    url.append(username).append(":").append(password).append("@");
                }
                url.append(host).append(":").append(port).append("/").append(databaseName);
                appendOptions(url);
                break;

            case REDIS:
                // redis://host:port/database
                url.setLength(0); // Clear "jdbc:"
                url.append("redis://");
                if (hasCredentials())
                {
                    url.append(":").append(password).append("@");
                }
                url.append(host).append(":").append(port).append("/").append(databaseName);
                break;

            default:
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }

        return url.toString();
    }

    /**
     * Append options as URL parameters using ? and &
     */
    private void appendOptions(StringBuilder url)
    {
        appendOptions(url, "?", "&");
    }

    /**
     * Append options using custom separators (e.g., H2 uses ; instead of & and ?)
     */
    private void appendOptions(StringBuilder url, String separator)
    {
        appendOptions(url, separator, separator);
    }

    private void appendOptions(StringBuilder url, String firstSeparator, String separator)
    {
        if (additionalOptions.isEmpty())
        {
            return;
        }

        boolean first = true;
        for (Map.Entry<String, String> entry : additionalOptions.entrySet())
        {
            url.append(first ? firstSeparator : separator);
            url.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
    }

    /**
     * Check if the configured database type uses standard JDBC.
     */
    public boolean isJdbcDatabase()
    {
        return databaseType != DatabaseType.REDIS;
    }

    /**
     * Check if the configured database type is a NoSQL database.
     */
    public boolean isNoSQL()
    {
        return databaseType.isNoSQL();
    }

    /**
     * Check if the configured database type is embedded (no server needed).
     */
    public boolean isEmbedded()
    {
        return databaseType.isEmbedded();
    }

    /**
     * Get the JDBC driver class name for the configured database type.
     */
    public String getDriverClass()
    {
        return databaseType.getDriverClass();
    }
}