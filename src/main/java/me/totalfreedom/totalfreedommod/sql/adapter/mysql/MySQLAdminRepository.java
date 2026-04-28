package me.totalfreedom.totalfreedommod.sql.adapter.mysql;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.sql.StatementHandler;
import me.totalfreedom.totalfreedommod.sql.adapter.AdminRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * MySQL/MariaDB implementation of AdminRepository.
 * Uses MySQL-specific SQL syntax.
 */
public class MySQLAdminRepository implements AdminRepository
{
    private final TotalFreedomMod plugin;
    private final StatementHandler statementHandler;

    public MySQLAdminRepository(TotalFreedomMod plugin, StatementHandler statementHandler)
    {
        this.plugin = plugin;
        this.statementHandler = statementHandler;
    }

    // ============================================
    // CREATE Operations
    // ============================================

    @Override
    public int insert(UUID uuid, Admin admin) throws SQLException
    {
        String sql = """
            INSERT INTO `admins` (`uuid`, `username`, `rank`, `active`, `last_login`, `login_message`)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = statementHandler.prepareStatement(sql,
                uuid.toString(),
                admin.getName(),
                admin.getRank().toString(),
                admin.isActive() ? 1 : 0,
                FUtil.dateToString(admin.getLastLogin()),
                admin.getLoginMessage()))
        {
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys())
            {
                if (rs.next())
                {
                    int adminId = rs.getInt(1);
                    insertIps(adminId, admin.getIps());
                    return adminId;
                }
            }
        }
        return -1;
    }

    @Override
    public void insertIps(int adminId, List<String> ips) throws SQLException
    {
        if (ips == null || ips.isEmpty()) return;

        // MySQL: INSERT IGNORE
        String sql = "INSERT IGNORE INTO `admin_ips` (`admin_id`, `ip`) VALUES (?, ?)";
        for (String ip : ips)
        {
            statementHandler.executeUpdate(sql, adminId, ip);
        }
    }

    @Override
    public void addIp(int adminId, String ip) throws SQLException
    {
        String sql = "INSERT IGNORE INTO `admin_ips` (`admin_id`, `ip`) VALUES (?, ?)";
        statementHandler.executeUpdate(sql, adminId, ip);
    }

    // ============================================
    // READ Operations
    // ============================================

    @Override
    public Map<String, Admin> loadAll() throws SQLException
    {
        Map<String, Admin> admins = new HashMap<>();
        Map<Integer, Admin> adminById = new HashMap<>();

        String sql = "SELECT `id`, `uuid`, `username`, `rank`, `active`, `last_login`, `login_message` FROM `admins`";
        try (ResultSet rs = statementHandler.executeQuery(sql))
        {
            while (rs.next())
            {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String rankStr = rs.getString("rank");
                boolean active = rs.getInt("active") == 1;
                String lastLoginStr = rs.getString("last_login");
                String loginMessage = rs.getString("login_message");

                String configKey = username.toLowerCase();
                Admin admin = new Admin(configKey);
                admin.setName(username);
                admin.setRank(Rank.findRank(rankStr));
                admin.setActive(active);
                admin.setLastLogin(FUtil.stringToDate(lastLoginStr));
                admin.setLoginMessage(loginMessage);

                UUID dbUuid = FUtil.parseUuid(rs.getString("uuid"));
                if (dbUuid != null)
                {
                    admin.setUuid(dbUuid);
                }

                admins.put(configKey, admin);
                adminById.put(id, admin);
            }
        }

        // Load IPs
        String ipSql = "SELECT `admin_id`, `ip` FROM `admin_ips`";
        try (ResultSet rs = statementHandler.executeQuery(ipSql))
        {
            while (rs.next())
            {
                int adminId = rs.getInt("admin_id");
                String ip = rs.getString("ip");
                Admin admin = adminById.get(adminId);
                if (admin != null)
                {
                    admin.addIp(ip);
                }
            }
        }

        return admins;
    }

    @Override
    public Admin findByUuid(UUID uuid) throws SQLException
    {
        String sql = "SELECT `id`, `uuid`, `username`, `rank`, `active`, `last_login`, `login_message` FROM `admins` WHERE `uuid` = ?";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, uuid.toString());
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next())
            {
                return loadAdminFromResultSet(rs);
            }
        }
        return null;
    }

    @Override
    public Admin findByUsername(String username) throws SQLException
    {
        // MySQL: case-insensitive with collation, but using LOWER() for consistency
        String sql = "SELECT `id`, `uuid`, `username`, `rank`, `active`, `last_login`, `login_message` FROM `admins` WHERE LOWER(`username`) = LOWER(?)";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next())
            {
                return loadAdminFromResultSet(rs);
            }
        }
        return null;
    }

    @Override
    public Admin findByIp(String ip) throws SQLException
    {
        String sql = """
            SELECT a.`id`, a.`uuid`, a.`username`, a.`rank`, a.`active`, a.`last_login`, a.`login_message`
            FROM `admins` a
            INNER JOIN `admin_ips` ai ON a.`id` = ai.`admin_id`
            WHERE ai.`ip` = ?
            """;
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, ip);
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next())
            {
                return loadAdminFromResultSet(rs);
            }
        }
        return null;
    }

    @Override
    public int getAdminId(String username) throws SQLException
    {
        String sql = "SELECT `id` FROM `admins` WHERE LOWER(`username`) = LOWER(?)";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next()) return rs.getInt("id");
        }
        return -1;
    }

    @Override
    public int getAdminIdByUuid(UUID uuid) throws SQLException
    {
        String sql = "SELECT `id` FROM `admins` WHERE `uuid` = ?";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, uuid.toString());
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next()) return rs.getInt("id");
        }
        return -1;
    }

    @Override
    public List<String> getIps(int adminId) throws SQLException
    {
        List<String> ips = new ArrayList<>();
        String sql = "SELECT `ip` FROM `admin_ips` WHERE `admin_id` = ?";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, adminId);
             ResultSet rs = stmt.executeQuery())
        {
            while (rs.next())
            {
                ips.add(rs.getString("ip"));
            }
        }
        return ips;
    }

    @Override
    public boolean exists(String username) throws SQLException
    {
        String sql = "SELECT COUNT(*) FROM `admins` WHERE LOWER(`username`) = LOWER(?)";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
             ResultSet rs = stmt.executeQuery())
        {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    @Override
    public boolean existsByUuid(UUID uuid) throws SQLException
    {
        String sql = "SELECT COUNT(*) FROM `admins` WHERE `uuid` = ?";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, uuid.toString());
             ResultSet rs = stmt.executeQuery())
        {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    @Override
    public UUID getUuidByUsername(String username) throws SQLException
    {
        String sql = "SELECT `uuid` FROM `admins` WHERE LOWER(`username`) = LOWER(?)";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next())
            {
                return UUID.fromString(rs.getString("uuid"));
            }
        }
        return null;
    }

    // ============================================
    // UPDATE Operations
    // ============================================

    @Override
    public boolean update(UUID uuid, Admin admin) throws SQLException
    {
        String sql = """
            UPDATE `admins`
            SET `username` = ?, `rank` = ?, `active` = ?, `last_login` = ?, `login_message` = ?
            WHERE `uuid` = ?
            """;

        int rows = statementHandler.executeUpdate(sql,
                admin.getName(),
                admin.getRank().toString(),
                admin.isActive() ? 1 : 0,
                FUtil.dateToString(admin.getLastLogin()),
                admin.getLoginMessage(),
                uuid.toString());

        return rows > 0;
    }

    @Override
    public boolean updateRank(String username, String rank) throws SQLException
    {
        String sql = "UPDATE `admins` SET `rank` = ? WHERE LOWER(`username`) = LOWER(?)";
        return statementHandler.executeUpdate(sql, rank, username) > 0;
    }

    @Override
    public boolean updateActive(String username, boolean active) throws SQLException
    {
        String sql = "UPDATE `admins` SET `active` = ? WHERE LOWER(`username`) = LOWER(?)";
        return statementHandler.executeUpdate(sql, active ? 1 : 0, username) > 0;
    }

    @Override
    public boolean updateLastLogin(String username, Date lastLogin) throws SQLException
    {
        String sql = "UPDATE `admins` SET `last_login` = ? WHERE LOWER(`username`) = LOWER(?)";
        return statementHandler.executeUpdate(sql, FUtil.dateToString(lastLogin), username) > 0;
    }

    @Override
    public boolean updateUsername(UUID uuid, String newUsername) throws SQLException
    {
        String sql = "UPDATE `admins` SET `username` = ? WHERE `uuid` = ?";
        return statementHandler.executeUpdate(sql, newUsername, uuid.toString()) > 0;
    }

    @Override
    public void syncIps(int adminId, List<String> ips) throws SQLException
    {
        statementHandler.executeUpdate("DELETE FROM `admin_ips` WHERE `admin_id` = ?", adminId);
        insertIps(adminId, ips);
    }

    @Override
    public int saveOrUpdate(UUID uuid, Admin admin) throws SQLException
    {
        if (existsByUuid(uuid))
        {
            update(uuid, admin);
            int adminId = getAdminIdByUuid(uuid);
            syncIps(adminId, admin.getIps());
            return adminId;
        }
        else
        {
            return insert(uuid, admin);
        }
    }

    // ============================================
    // DELETE Operations
    // ============================================

    @Override
    public boolean delete(UUID uuid) throws SQLException
    {
        String sql = "DELETE FROM `admins` WHERE `uuid` = ?";
        return statementHandler.executeUpdate(sql, uuid.toString()) > 0;
    }

    @Override
    public boolean deleteByUsername(String username) throws SQLException
    {
        String sql = "DELETE FROM `admins` WHERE LOWER(`username`) = LOWER(?)";
        return statementHandler.executeUpdate(sql, username) > 0;
    }

    @Override
    public boolean removeIp(int adminId, String ip) throws SQLException
    {
        String sql = "DELETE FROM `admin_ips` WHERE `admin_id` = ? AND `ip` = ?";
        return statementHandler.executeUpdate(sql, adminId, ip) > 0;
    }

    @Override
    public boolean clearIps(int adminId) throws SQLException
    {
        String sql = "DELETE FROM `admin_ips` WHERE `admin_id` = ?";
        return statementHandler.executeUpdate(sql, adminId) > 0;
    }

    @Override
    public void deleteAllSync() throws SQLException
    {
        statementHandler.executeUpdate("DELETE FROM `admin_ips`");
        statementHandler.executeUpdate("DELETE FROM `admins`");
    }

    // ============================================
    // Async Operations
    // ============================================

    @Override
    public CompletableFuture<Map<String, Admin>> loadAllAsync()
    {
        return CompletableFuture.supplyAsync(() -> {
            try { return loadAll(); }
            catch (SQLException e) { FLog.severe("Failed to load admins: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<Integer> insertAsync(UUID uuid, Admin admin)
    {
        return CompletableFuture.supplyAsync(() -> {
            try { return insert(uuid, admin); }
            catch (SQLException e) { FLog.severe("Failed to insert admin: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<Boolean> updateAsync(UUID uuid, Admin admin)
    {
        return CompletableFuture.supplyAsync(() -> {
            try { return update(uuid, admin); }
            catch (SQLException e) { FLog.severe("Failed to update admin: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteAsync(UUID uuid)
    {
        return CompletableFuture.supplyAsync(() -> {
            try { return delete(uuid); }
            catch (SQLException e) { FLog.severe("Failed to delete admin: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<Integer> save(UUID uuid, Admin admin)
    {
        return CompletableFuture.supplyAsync(() -> {
            try { return saveOrUpdate(uuid, admin); }
            catch (SQLException e) { FLog.severe("Failed to save admin: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<List<Admin>> findAll()
    {
        return CompletableFuture.supplyAsync(() -> {
            try { return new ArrayList<>(loadAll().values()); }
            catch (SQLException e) { FLog.severe("Failed to find all admins: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteByUuid(UUID uuid)
    {
        return deleteAsync(uuid);
    }

    @Override
    public CompletableFuture<Void> deleteAll()
    {
        return CompletableFuture.runAsync(() -> {
            try { deleteAllSync(); }
            catch (SQLException e) { FLog.severe("Failed to delete all admins: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    // ============================================
    // Helper Methods
    // ============================================

    private Admin loadAdminFromResultSet(ResultSet rs) throws SQLException
    {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String rankStr = rs.getString("rank");
        boolean active = rs.getInt("active") == 1;
        String lastLoginStr = rs.getString("last_login");
        String loginMessage = rs.getString("login_message");

        String configKey = username.toLowerCase();
        Admin admin = new Admin(configKey);
        admin.setName(username);
        admin.setRank(Rank.findRank(rankStr));
        admin.setActive(active);
        admin.setLastLogin(FUtil.stringToDate(lastLoginStr));
        admin.setLoginMessage(loginMessage);

        UUID dbUuid = FUtil.parseUuid(rs.getString("uuid"));
        if (dbUuid != null)
        {
            admin.setUuid(dbUuid);
        }

        List<String> ips = getIps(id);
        admin.addIps(ips);

        return admin;
    }
}
