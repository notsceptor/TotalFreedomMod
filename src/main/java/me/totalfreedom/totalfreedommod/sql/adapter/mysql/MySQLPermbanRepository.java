package me.totalfreedom.totalfreedommod.sql.adapter.mysql;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.banning.PermBan;
import me.totalfreedom.totalfreedommod.sql.StatementHandler;
import me.totalfreedom.totalfreedommod.sql.adapter.PermbanRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import reactor.core.publisher.Mono;

/**
 * MySQL/MariaDB implementation of PermbanRepository.
 * Uses MySQL-specific SQL syntax.
 */
public class MySQLPermbanRepository implements PermbanRepository
{
    private final TotalFreedomMod plugin;
    private final StatementHandler statementHandler;

    public MySQLPermbanRepository(TotalFreedomMod plugin, StatementHandler statementHandler)
    {
        this.plugin = plugin;
        this.statementHandler = statementHandler;
    }

    // ============================================
    // CREATE Operations
    // ============================================

    @Override
    public int insert(PermBan permban) throws SQLException
    {
        String sql = """
            INSERT INTO `permbans` (`uuid`, `username`, `reason`)
            VALUES (?, ?, ?)
            """;

        try (PreparedStatement stmt = statementHandler.prepareStatement(sql,
                permban.getUuid() != null ? permban.getUuid().toString() : null,
                permban.getUsername(),
                permban.getReason()))
        {
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys())
            {
                if (rs.next())
                {
                    int permbanId = rs.getInt(1);
                    insertIps(permbanId, permban.getIps());
                    return permbanId;
                }
            }
        }
        return -1;
    }

    @Override
    public void insertIps(int permbanId, List<String> ips) throws SQLException
    {
        if (ips == null || ips.isEmpty()) return;

        String sql = "INSERT IGNORE INTO `permban_ips` (`permban_id`, `ip`) VALUES (?, ?)";
        for (String ip : ips)
        {
            statementHandler.executeUpdate(sql, permbanId, ip);
        }
    }

    @Override
    public void addIp(int permbanId, String ip) throws SQLException
    {
        String sql = "INSERT IGNORE INTO `permban_ips` (`permban_id`, `ip`) VALUES (?, ?)";
        statementHandler.executeUpdate(sql, permbanId, ip);
    }

    // ============================================
    // READ Operations
    // ============================================

    @Override
    public List<PermBan> loadAll() throws SQLException
    {
        List<PermBan> permbans = new ArrayList<>();
        Map<Integer, PermBan> permbanById = new HashMap<>();

        String sql = "SELECT `id`, `uuid`, `username`, `reason` FROM `permbans`";
        try (ResultSet rs = statementHandler.executeQuery(sql))
        {
            while (rs.next())
            {
                int id = rs.getInt("id");
                String uuidStr = rs.getString("uuid");
                String username = rs.getString("username");
                String reason = rs.getString("reason");

                PermBan permban = new PermBan();
                permban.setUuid(uuidStr != null ? UUID.fromString(uuidStr) : null);
                permban.setUsername(username);
                permban.setReason(reason);

                permbans.add(permban);
                permbanById.put(id, permban);
            }
        }

        // Load IPs
        String ipSql = "SELECT `permban_id`, `ip` FROM `permban_ips`";
        try (ResultSet rs = statementHandler.executeQuery(ipSql))
        {
            while (rs.next())
            {
                int permbanId = rs.getInt("permban_id");
                String ip = rs.getString("ip");
                PermBan permban = permbanById.get(permbanId);
                if (permban != null)
                {
                    permban.addIp(ip);
                }
            }
        }

        return permbans;
    }

    @Override
    public PermBan findByUuid(UUID uuid) throws SQLException
    {
        String sql = "SELECT `id`, `uuid`, `username`, `reason` FROM `permbans` WHERE `uuid` = ?";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, uuid.toString());
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next())
            {
                return loadPermbanFromResultSet(rs);
            }
        }
        return null;
    }

    @Override
    public PermBan findByUsername(String username) throws SQLException
    {
        String sql = "SELECT `id`, `uuid`, `username`, `reason` FROM `permbans` WHERE LOWER(`username`) = LOWER(?)";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next())
            {
                return loadPermbanFromResultSet(rs);
            }
        }
        return null;
    }

    @Override
    public PermBan findByIp(String ip) throws SQLException
    {
        String sql = """
            SELECT p.`id`, p.`uuid`, p.`username`, p.`reason`
            FROM `permbans` p
            INNER JOIN `permban_ips` pi ON p.`id` = pi.`permban_id`
            WHERE pi.`ip` = ?
            """;
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, ip);
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next())
            {
                return loadPermbanFromResultSet(rs);
            }
        }
        return null;
    }

    @Override
    public int getPermbanId(UUID uuid) throws SQLException
    {
        String sql = "SELECT `id` FROM `permbans` WHERE `uuid` = ?";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, uuid.toString());
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next()) return rs.getInt("id");
        }
        return -1;
    }

    @Override
    public int getPermbanIdByUsername(String username) throws SQLException
    {
        String sql = "SELECT `id` FROM `permbans` WHERE LOWER(`username`) = LOWER(?)";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next()) return rs.getInt("id");
        }
        return -1;
    }

    @Override
    public List<String> getIps(int permbanId) throws SQLException
    {
        List<String> ips = new ArrayList<>();
        String sql = "SELECT `ip` FROM `permban_ips` WHERE `permban_id` = ?";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, permbanId);
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
    public boolean isPermBanned(UUID uuid) throws SQLException
    {
        String sql = "SELECT COUNT(*) FROM `permbans` WHERE `uuid` = ?";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, uuid.toString());
             ResultSet rs = stmt.executeQuery())
        {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    @Override
    public boolean isPermBannedByUsername(String username) throws SQLException
    {
        String sql = "SELECT COUNT(*) FROM `permbans` WHERE LOWER(`username`) = LOWER(?)";
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
             ResultSet rs = stmt.executeQuery())
        {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    @Override
    public boolean isPermBannedByIp(String ip) throws SQLException
    {
        String sql = """
            SELECT COUNT(*) FROM `permbans` p
            INNER JOIN `permban_ips` pi ON p.`id` = pi.`permban_id`
            WHERE pi.`ip` = ?
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
    public boolean update(PermBan permban) throws SQLException
    {
        String sql = """
            UPDATE `permbans`
            SET `username` = ?, `reason` = ?
            WHERE `uuid` = ?
            """;

        int rows = statementHandler.executeUpdate(sql,
                permban.getUsername(),
                permban.getReason(),
                permban.getUuid().toString());

        return rows > 0;
    }

    @Override
    public boolean updateReason(UUID uuid, String reason) throws SQLException
    {
        String sql = "UPDATE `permbans` SET `reason` = ? WHERE `uuid` = ?";
        return statementHandler.executeUpdate(sql, reason, uuid.toString()) > 0;
    }

    @Override
    public void syncIps(int permbanId, List<String> ips) throws SQLException
    {
        statementHandler.executeUpdate("DELETE FROM `permban_ips` WHERE `permban_id` = ?", permbanId);
        insertIps(permbanId, ips);
    }

    // ============================================
    // DELETE Operations
    // ============================================

    @Override
    public boolean delete(UUID uuid) throws SQLException
    {
        String sql = "DELETE FROM `permbans` WHERE `uuid` = ?";
        return statementHandler.executeUpdate(sql, uuid.toString()) > 0;
    }

    @Override
    public boolean deleteByUsername(String username) throws SQLException
    {
        String sql = "DELETE FROM `permbans` WHERE LOWER(`username`) = LOWER(?)";
        return statementHandler.executeUpdate(sql, username) > 0;
    }

    @Override
    public boolean deleteByIp(String ip) throws SQLException
    {
        String selectSql = "SELECT `permban_id` FROM `permban_ips` WHERE `ip` = ?";
        List<Integer> permbanIds = new ArrayList<>();
        try (PreparedStatement stmt = statementHandler.prepareStatement(selectSql, ip);
             ResultSet rs = stmt.executeQuery())
        {
            while (rs.next())
            {
                permbanIds.add(rs.getInt("permban_id"));
            }
        }

        for (int permbanId : permbanIds)
        {
            statementHandler.executeUpdate("DELETE FROM `permbans` WHERE `id` = ?", permbanId);
        }

        return !permbanIds.isEmpty();
    }

    @Override
    public boolean removeIp(int permbanId, String ip) throws SQLException
    {
        String sql = "DELETE FROM `permban_ips` WHERE `permban_id` = ? AND `ip` = ?";
        return statementHandler.executeUpdate(sql, permbanId, ip) > 0;
    }

    @Override
    public void deleteAllSync() throws SQLException
    {
        statementHandler.executeUpdate("DELETE FROM `permban_ips`");
        statementHandler.executeUpdate("DELETE FROM `permbans`");
    }

    // ============================================
    // Async Operations
    // ============================================

    @Override
    public Mono<List<PermBan>> loadAllAsync()
    {
        return statementHandler.supplyMono(this::loadAll);
    }

    @Override
    public Mono<Integer> insertAsync(PermBan permban)
    {
        return statementHandler.supplyMono(() -> insert(permban));
    }

    @Override
    public Mono<Boolean> updateAsync(PermBan permban)
    {
        return statementHandler.supplyMono(() -> update(permban));
    }

    @Override
    public Mono<Boolean> deleteAsync(UUID uuid)
    {
        return statementHandler.supplyMono(() -> delete(uuid));
    }

    @Override
    public Mono<Integer> save(PermBan permban)
    {
        return statementHandler.supplyMono(() -> {
            if (permban.getUuid() != null && getPermbanId(permban.getUuid()) > 0)
            {
                update(permban);
                return getPermbanId(permban.getUuid());
            }
            return insert(permban);
        });
    }

    @Override
    public Mono<List<PermBan>> findAll()
    {
        return loadAllAsync();
    }

    @Override
    public Mono<Void> deleteAll()
    {
        return statementHandler.runMono(this::deleteAllSync);
    }

    // ============================================
    // Helper Methods
    // ============================================

    private PermBan loadPermbanFromResultSet(ResultSet rs) throws SQLException
    {
        int id = rs.getInt("id");
        String uuidStr = rs.getString("uuid");
        String username = rs.getString("username");
        String reason = rs.getString("reason");

        PermBan permban = new PermBan();
        permban.setUuid(uuidStr != null ? UUID.fromString(uuidStr) : null);
        permban.setUsername(username);
        permban.setReason(reason);

        List<String> ips = getIps(id);
        permban.setIps(ips);

        return permban;
    }
}
