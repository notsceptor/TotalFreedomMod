package me.totalfreedom.totalfreedommod.sql.adapter.postgresql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.banning.StrikeRecord;
import me.totalfreedom.totalfreedommod.sql.StatementHandler;
import me.totalfreedom.totalfreedommod.sql.adapter.StrikeRepository;
import me.totalfreedom.totalfreedommod.util.FLog;

public class PostgreSQLStrikeRepository implements StrikeRepository
{
    private final TotalFreedomMod plugin;
    private final StatementHandler statementHandler;

    public PostgreSQLStrikeRepository(TotalFreedomMod plugin, StatementHandler statementHandler)
    {
        this.plugin = plugin;
        this.statementHandler = statementHandler;
    }

    @Override
    public Map<String, StrikeRecord> loadAll() throws SQLException
    {
        Map<String, StrikeRecord> out = new HashMap<>();
        String sql = "SELECT ip, strike_count, last_strike_unix, last_username FROM strikes";
        try (ResultSet rs = statementHandler.executeQuery(sql))
        {
            while (rs.next())
            {
                String ip = rs.getString("ip");
                int count = rs.getInt("strike_count");
                long last = rs.getLong("last_strike_unix");
                String username = rs.getString("last_username");
                out.put(ip, new StrikeRecord(ip, count, last, username));
            }
        }
        return out;
    }

    @Override
    public void upsert(StrikeRecord r) throws SQLException
    {
        String sql = """
            INSERT INTO strikes (ip, strike_count, last_strike_unix, last_username)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (ip) DO UPDATE SET
                strike_count = EXCLUDED.strike_count,
                last_strike_unix = EXCLUDED.last_strike_unix,
                last_username = EXCLUDED.last_username
            """;
        statementHandler.executeUpdate(sql,
                r.getIp(), r.getCount(), r.getLastStrikeUnix(), r.getLastUsername());
    }

    @Override
    public boolean deleteByIp(String ip) throws SQLException
    {
        return statementHandler.executeUpdate("DELETE FROM strikes WHERE ip = ?", ip) > 0;
    }

    @Override
    public void deleteAllSync() throws SQLException
    {
        statementHandler.executeUpdate("DELETE FROM strikes");
    }

    @Override
    public CompletableFuture<Map<String, StrikeRecord>> loadAllAsync()
    {
        return CompletableFuture.supplyAsync(() -> {
            try { return loadAll(); }
            catch (SQLException e) { FLog.severe("Failed to load strikes: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<Void> upsertAsync(StrikeRecord r)
    {
        return CompletableFuture.runAsync(() -> {
            try { upsert(r); }
            catch (SQLException e) { FLog.severe("Failed to upsert strike: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteByIpAsync(String ip)
    {
        return CompletableFuture.supplyAsync(() -> {
            try { return deleteByIp(ip); }
            catch (SQLException e) { FLog.severe("Failed to delete strike: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }

    @Override
    public CompletableFuture<Void> deleteAll()
    {
        return CompletableFuture.runAsync(() -> {
            try { deleteAllSync(); }
            catch (SQLException e) { FLog.severe("Failed to clear strikes: " + e.getMessage()); throw new RuntimeException(e); }
        });
    }
}
