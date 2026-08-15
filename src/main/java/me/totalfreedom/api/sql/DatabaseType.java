package me.totalfreedom.api.sql;

/**
 * Enum of supported database types with their default ports and JDBC info.
 */
public enum DatabaseType
{
    SQLITE("sqlite", 0, "org.sqlite.JDBC", false),
    MYSQL("mysql", 3306, "com.mysql.cj.jdbc.Driver", true),
    POSTGRESQL("postgresql", 5432, "org.postgresql.Driver", true);

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

    public boolean isEmbedded()
    {
        return this == SQLITE;
    }
}
