package me.totalfreedom.totalfreedommod.sql.adapter.mysql;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.banning.Ban;
import me.totalfreedom.totalfreedommod.sql.StatementHandler;
import me.totalfreedom.totalfreedommod.sql.adapter.BanRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * MySQL/MariaDB implementation of BanRepository.
 * Uses MySQL-specific SQL syntax.
 */
public class MySQLBanRepository implements BanRepository
{
    private final TotalFreedomMod plugin;
    private final StatementHandler statementHandler;

    public MySQLBanRepository(TotalFreedomMod plugin, StatementHandler statementHandler)
    {
        this.plugin = plugin;
        this.statementHandler = statementHandler;
    }

    // ============================================
    // CREATE Operations
    // ============================================

    @Override
    public int insert(Ban ban) throws SQLException
    {
        String sql = """
            INSERT INTO `bans` (`uuid`, `username`, `banned_by`, `banned_by_uuid`, `reason`, `expire_at`)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = statementHandler.prepareStatement(sql,
                ban.getUuid() != null ? ban.getUuid().toString() : null,
                ban.getUsername(),
                ban.getBannedBy(),
                ban.getBannedByUuid() != null ? ban.getBannedByUuid().toString() : null,
                ban.getReason(),
                ban.getExpireAt() != null ? FUtil.dateToString(ban.getExpireAt()) : null))
        {
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys())
            {
                if (rs.next())
                {
                    int banId = rs.getInt(1);
                    insertIps(banId, ban.getIps());
                    return banId;
                }
            }
        }
        return -1;
    }

    @Override
    public void insertIps(int banId, List<String> ips) throws SQLException
    {
        if (ips == null || ips.isEmpty()) return;

        String sql = "INSERT IGNORE INTO `ban_ips` (`ban_id`, `ip`) VALUES (?, ?)";
        for (String ip : ips)
        {
            statementHandler.executeUpdate(sql, banId, ip);
        }
    }

    @Override
    public void addIp(int banId, String ip) throws SQLException
    {
        String sql = "INSERT IGNORE INTO `ban_ips` (`ban_id`, `ip`) VALUES (?, ?)";
        statementHandler.executeUpdate(sql, banId, ip);
    }

    // ============================================
    // READ Operations
    // ============================================

    @Override
    public List<Ban> loadAll() throws SQLException
    {
        List<Ban> bans = new ArrayList<>();
        Map<Integer, Ban> banById = new HashMap<>();

        String sql = "SELECT `id`, `uuid`, `username`, `banned_by`, `banned_by_uuid`, `reason`, `expire_at` FROM `bans`";
        try (ResultSet rs = statementHandler.executeQuery(sql))
        {
            while (rs.next())
            {
                int id = rs.getInt("id");
                String uuidStr = rs.getString("uuid");
                String username = rs.getString("username");
                String bannedBy = rs.getString("banned_by");
                String bannedByUuidStr = rs.getString("banned_by_uuid");
                String reason = rs.getString("reason");
                String expireAtStr = rs.getString("expire_at");

                Ban ban = new Ban();
                ban.setUuid(uuidStr != null ? UUID.fromString(uuidStr) : null);
                ban.setUsername(username);
                ban.setBannedBy(bannedBy);
                ban.setBannedByUuid(bannedByUuidStr != null ? UUID.fromString(bannedByUuidStr) : null);
                ban.setReason(reason);
                ban.setExpireAt(expireAtStr != null ? FUtil.stringToDate(expireAtStr) : null);

                bans.add(ban);
                banById.put(id, ban);
            }
        }

        // Load IPs
        String ipSql = "SELECT `ban_id`, `ip` FROM `ban_ips`";
        try (ResultSet rs = statementHandler.executeQuery(ipSql))
        {
            while (rs.next())
            {
                int banId = rs.getInt("ban_id");
                String ip = rs.getString("ip");
                Ban ban = banById.get(banId);
                if (ban != null)
                {
                    ban.addIp(ip);
                }
            }
        }

        return bans;
    }

    @Override
    public Ban findByUuid(UUID uuid) throws SQLException
    {
        String sql = "SELECT `id`, `uuid`, `username`, `banned_by`, `banned_by_uuid`, `reason`, `expire_at` FROM `bans` WHERE `uuid` = ?";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, uuid.toString());
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next())
            {
                return loadBanFromResultSet(rs);
            }
        }
        return null;
    }

    @Override
    public Ban findByUsername(String username) throws SQLException
    {
        String sql = "SELECT `id`, `uuid`, `username`, `banned_by`, `banned_by_uuid`, `reason`, `expire_at` FROM `bans` WHERE LOWER(`username`) = LOWER(?)";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next())
            {
                return loadBanFromResultSet(rs);
            }
        }
        return null;
    }

    @Override
    public Ban findByIp(String ip) throws SQLException
    {
        String sql = """
            SELECT b.`id`, b.`uuid`, b.`username`, b.`banned_by`, b.`banned_by_uuid`, b.`reason`, b.`expire_at`
            FROM `bans` b
            INNER JOIN `ban_ips` bi ON b.`id` = bi.`ban_id`
            WHERE bi.`ip` = ?
            """;
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, ip);
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next())
            {
                return loadBanFromResultSet(rs);
            }
        }
        return null;
    }

    @Override
    public List<Ban> findActiveBans() throws SQLException
    {
        // MySQL: NOW() for current time
        String sql = """
            SELECT `id`, `uuid`, `username`, `banned_by`, `banned_by_uuid`, `reason`, `expire_at`
            FROM `bans`
            WHERE `expire_at` IS NULL OR `expire_at` > NOW()
            """;
        List<Ban> bans = new ArrayList<>();
        try (ResultSet rs = statementHandler.executeQuery(sql))
        {
            while (rs.next())
            {
                bans.add(loadBanFromResultSet(rs));
            }
        }
        return bans;
    }

    @Override
    public List<Ban> findExpiredBans() throws SQLException
    {
        String sql = """
            SELECT `id`, `uuid`, `username`, `banned_by`, `banned_by_uuid`, `reason`, `expire_at`
            FROM `bans`
            WHERE `expire_at` IS NOT NULL AND `expire_at` <= NOW()
            """;
        List<Ban> bans = new ArrayList<>();
        try (ResultSet rs = statementHandler.executeQuery(sql))
        {
            while (rs.next())
            {
                bans.add(loadBanFromResultSet(rs));
            }
        }
        return bans;
    }

    @Override
    public int getBanId(UUID uuid) throws SQLException
    {
        String sql = "SELECT `id` FROM `bans` WHERE `uuid` = ?";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, uuid.toString());
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next()) return rs.getInt("id");
        }
        return -1;
    }

    @Override
    public int getBanIdByUsername(String username) throws SQLException
    {
        String sql = "SELECT `id` FROM `bans` WHERE LOWER(`username`) = LOWER(?)";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next()) return rs.getInt("id");
        }
        return -1;
    }

    @Override
    public List<String> getIps(int banId) throws SQLException
    {
        List<String> ips = new ArrayList<>();
        String sql = "SELECT `ip` FROM `ban_ips` WHERE `ban_id` = ?";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, banId);
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
    public boolean isBanned(UUID uuid) throws SQLException
    {
        String sql = """
            SELECT COUNT(*) FROM `bans` 
            WHERE `uuid` = ? AND (`expire_at` IS NULL OR `expire_at` > NOW())
            """;
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, uuid.toString());
             ResultSet rs = stmt.executeQuery())
        {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    @Override
    public boolean isBannedByUsername(String username) throws SQLException
    {
        String sql = """
            SELECT COUNT(*) FROM `bans` 
            WHERE LOWER(`username`) = LOWER(?) AND (`expire_at` IS NULL OR `expire_at` > NOW())
            """;
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
             ResultSet rs = stmt.executeQuery())
        {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    @Override
    public boolean isBannedByIp(String ip) throws SQLException
    {
        String sql = """
            SELECT COUNT(*) FROM `bans` b
            INNER JOIN `ban_ips` bi ON b.`id` = bi.`ban_id`
            WHERE bi.`ip` = ? AND (b.`expire_at` IS NULL OR b.`expire_at` > NOW())
            """;
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, ip);
             ResultSet rs = stmt.executeQuery())
        {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // ============================================
    // UPDATE Operations
    // ============================================

    @Override
    public boolean update(Ban ban) throws SQLException
    {
        String sql = """
            UPDATE `bans`
            SET `username` = ?, `banned_by` = ?, `banned_by_uuid` = ?, `reason` = ?, `expire_at` = ?
            WHERE `uuid` = ?
            """;

        int rows = statementHandler.executeUpdate(sql,
                ban.getUsername(),
                ban.getBannedBy(),
                ban.getBannedByUuid() != null ? ban.getBannedByUuid().toString() : null,
                ban.getReason(),
                ban.getExpireAt() != null ? FUtil.dateToString(ban.getExpireAt()) : null,
                ban.getUuid().toString());

        return rows > 0;
    }

    @Override
    public boolean updateReason(UUID uuid, String reason) throws SQLException
    {
        String sql = "UPDATE `bans` SET `reason` = ? WHERE `uuid` = ?";
        return statementHandler.executeUpdate(sql, reason, uuid.toString()) > 0;
    }

    @Override
    public boolean updateExpiry(UUID uuid, Date expireAt) throws SQLException
    {
        String sql = "UPDATE `bans` SET `expire_at` = ? WHERE `uuid` = ?";
        return statementHandler.executeUpdate(sql, expireAt != null ? FUtil.dateToString(expireAt) : null, uuid.toString()) > 0;
    }

    @Override
    public void syncIps(int banId, List<String> ips) throws SQLException
    {
        statementHandler.executeUpdate("DELETE FROM `ban_ips` WHERE `ban_id` = ?", banId);
        insertIps(banId, ips);
    }

    // ============================================
    // DELETE Operations
    // ============================================

    @Override
    public boolean delete(UUID uuid) throws SQLException
    {
        String sql = "DELETE FROM `bans` WHERE `uuid` = ?";
        return statementHandler.executeUpdate(sql, uuid.toString()) > 0;
    }

    @Override
    public boolean deleteByUsername(String username) throws SQLException
    {
        String sql = "DELETE FROM `bans` WHERE LOWER(`username`) = LOWER(?)";
        return statementHandler.executeUpdate(sql, username) > 0;
    }

    @Override
    public boolean deleteByIp(String ip) throws SQLException
    {
        String selectSql = "SELECT `ban_id` FROM `ban_ips` WHERE `ip` = ?";
        List<Integer> banIds = new ArrayList<>();
        try (PreparedStatement stmt = statementHandler.prepareStatement(selectSql, ip);
             ResultSet rs = stmt.executeQuery())
        {
            while (rs.next())
            {
                banIds.add(rs.getInt("ban_id"));
            }
        }

        for (int banId : banIds)
        {
            statementHandler.executeUpdate("DELETE FROM `bans` WHERE `id` = ?", banId);
        }

        return !banIds.isEmpty();
    }

    @Override
    public boolean removeIp(int banId, String ip) throws SQLException
    {
        String sql = "DELETE FROM `ban_ips` WHERE `ban_id` = ? AND `ip` = ?";
        return statementHandler.executeUpdate(sql, banId, ip) > 0;
    }

    @Override
    public int deleteExpiredBans() throws SQLException
    {
        String sql = "DELETE FROM `bans` WHERE `expire_at` IS NOT NULL AND `expire_at` <= NOW()";
        return statementHandler.executeUpdate(sql);
    }

    @Override
    public void deleteAllSync() throws SQLException
    {
        statementHandler.executeUpdate("DELETE FROM `ban_ips`");
        statementHandler.executeUpdate("DELETE FROM `bans`");
    }

    // ============================================
    // Async Operations
    // ============================================

    @Override
    public CompletableFuture<List<Ban>> loadAllAsync()
    {
        return CompletableFuture.supplyAsync(() -> {
            try { return loadAll(); }
            catch (SQLException e) { FLog.severe("Failed to load bans: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<Integer> insertAsync(Ban ban)
    {
        return CompletableFuture.supplyAsync(() -> {
            try { return insert(ban); }
            catch (SQLException e) { FLog.severe("Failed to insert ban: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<Boolean> updateAsync(Ban ban)
    {
        return CompletableFuture.supplyAsync(() -> {
            try { return update(ban); }
            catch (SQLException e) { FLog.severe("Failed to update ban: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteAsync(UUID uuid)
    {
        return CompletableFuture.supplyAsync(() -> {
            try { return delete(uuid); }
            catch (SQLException e) { FLog.severe("Failed to delete ban: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<Integer> save(Ban ban)
    {
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                // Check if ban exists by UUID
                if (ban.getUuid() != null && getBanId(ban.getUuid()) > 0)
                {
                    update(ban);
                    return getBanId(ban.getUuid());
                }
                return insert(ban);
            }
            catch (SQLException e) { FLog.severe("Failed to save ban: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<List<Ban>> findAll()
    {
        return loadAllAsync();
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
            catch (SQLException e) { FLog.severe("Failed to delete all bans: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    // ============================================
    // Helper Methods
    // ============================================

    private Ban loadBanFromResultSet(ResultSet rs) throws SQLException
    {
        int id = rs.getInt("id");
        String uuidStr = rs.getString("uuid");
        String username = rs.getString("username");
        String bannedBy = rs.getString("banned_by");
        String bannedByUuidStr = rs.getString("banned_by_uuid");
        String reason = rs.getString("reason");
        String expireAtStr = rs.getString("expire_at");

        Ban ban = new Ban();
        ban.setUuid(uuidStr != null ? UUID.fromString(uuidStr) : null);
        ban.setUsername(username);
        ban.setBannedBy(bannedBy);
        ban.setBannedByUuid(bannedByUuidStr != null ? UUID.fromString(bannedByUuidStr) : null);
        ban.setReason(reason);
        ban.setExpireAt(expireAtStr != null ? FUtil.stringToDate(expireAtStr) : null);

        List<String> ips = getIps(id);
        ban.setIps(ips);

        return ban;
    }
}
