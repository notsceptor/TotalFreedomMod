package me.totalfreedom.totalfreedommod.sql;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataManager {

        private final TotalFreedomMod plugin;
        private final ConnectionHandler connectionHandler;
        private final StatementHandler statementHandler;

        public DataManager(TotalFreedomMod plugin) {
            this.plugin = plugin;
            this.connectionHandler = new ConnectionHandler(plugin);
            this.statementHandler = new StatementHandler(connectionHandler);
        }

        public void initialize() {
            try {
                connectionHandler.getConnection().join();
                runMigrations();
            }
            catch (SQLException ex)
            {
                plugin.getLogger().severe("Failed to initialize database: " + ex.getMessage());
            }
            catch (Exception ex)
            {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                plugin.getLogger().severe("Failed to initialize database: " + cause.getMessage());
            }
        }
        
        private void runMigrations() throws SQLException {
            createMigrationTable();
            
            if (!hasMigrationRun("001_create_admins_table")) {
                migrateAdmins();
                recordMigration("001_create_admins_table");
            }
            
            if (!hasMigrationRun("002_create_bans_table")) {
                migrateBans();
                recordMigration("002_create_bans_table");
            }
            
            if (!hasMigrationRun("003_create_permbans_table")) {
                migratePermbans();
                recordMigration("003_create_permbans_table");
            }
        }
        
        private void createMigrationTable() throws SQLException {
            String sql = """
                CREATE TABLE IF NOT EXISTS migrations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    migration_name TEXT NOT NULL UNIQUE,
                    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            statementHandler.executeUpdate(sql);
        }
        
        private boolean hasMigrationRun(String migrationName) throws SQLException {
            String sql = "SELECT COUNT(*) FROM migrations WHERE migration_name = ?";
            try (PreparedStatement stmt = statementHandler.prepareStatement(sql, migrationName);
                 ResultSet rs = stmt.executeQuery())
            {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
        
        private void recordMigration(String migrationName) throws SQLException {
            String sql = "INSERT INTO migrations (migration_name) VALUES (?)";
            statementHandler.executeUpdate(sql, migrationName);
        }
        
        private void migrateAdmins() throws SQLException {
            // Admins table with UUID as primary identifier
            // uuid, username, active, rank, ips[], last_login, login_message
            String adminsSql = """
                CREATE TABLE IF NOT EXISTS admins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL UNIQUE,
                    username TEXT NOT NULL,
                    rank TEXT NOT NULL DEFAULT 'SUPER_ADMIN',
                    active BOOLEAN DEFAULT TRUE,
                    last_login TEXT,
                    login_message TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            statementHandler.executeUpdate(adminsSql);

            // Separate table for admin IPs (one-to-many relationship)
            String adminIpsSql = """
                CREATE TABLE IF NOT EXISTS admin_ips (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    admin_id INTEGER NOT NULL,
                    ip TEXT NOT NULL,
                    FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE CASCADE,
                    UNIQUE(admin_id, ip)
                )
                """;
            statementHandler.executeUpdate(adminIpsSql);
            plugin.getLogger().info("Created admins and admin_ips tables");
        }
        
        private void migrateBans() throws SQLException {
            // Bans table with UUID as primary identifier
            // uuid, username (cached), ips[], by, reason, expiry_unix
            String bansSql = """
                CREATE TABLE IF NOT EXISTS bans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT,
                    username TEXT,
                    by TEXT,
                    reason TEXT,
                    expiry_unix INTEGER DEFAULT -1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            statementHandler.executeUpdate(bansSql);

            // Separate table for ban IPs (one-to-many relationship)
            String banIpsSql = """
                CREATE TABLE IF NOT EXISTS ban_ips (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ban_id INTEGER NOT NULL,
                    ip TEXT NOT NULL,
                    FOREIGN KEY (ban_id) REFERENCES bans(id) ON DELETE CASCADE,
                    UNIQUE(ban_id, ip)
                )
                """;
            statementHandler.executeUpdate(banIpsSql);
            plugin.getLogger().info("Created bans and ban_ips tables");
        }
        
        private void migratePermbans() throws SQLException {
            // Permbans table with UUID as primary identifier
            // uuid, username (cached), ips[]
            String sql = """
                CREATE TABLE IF NOT EXISTS permbans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL UNIQUE,
                    username TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            statementHandler.executeUpdate(sql);

            // Separate table for permban IPs (one-to-many relationship)
            String permbanIpsSql = """
                CREATE TABLE IF NOT EXISTS permban_ips (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    permban_id INTEGER NOT NULL,
                    ip TEXT NOT NULL,
                    FOREIGN KEY (permban_id) REFERENCES permbans(id) ON DELETE CASCADE,
                    UNIQUE(permban_id, ip)
                )
                """;
            statementHandler.executeUpdate(permbanIpsSql);
            plugin.getLogger().info("Created permbans and permban_ips tables");
        }
        
        public void close() {
            statementHandler.close();
        }
}
