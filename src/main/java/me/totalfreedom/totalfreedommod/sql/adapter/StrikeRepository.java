package me.totalfreedom.totalfreedommod.sql.adapter;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import me.totalfreedom.totalfreedommod.banning.StrikeRecord;

public interface StrikeRepository
{
    Map<String, StrikeRecord> loadAll() throws SQLException;

    void upsert(StrikeRecord record) throws SQLException;

    boolean deleteByIp(String ip) throws SQLException;

    void deleteAllSync() throws SQLException;

    CompletableFuture<Map<String, StrikeRecord>> loadAllAsync();

    CompletableFuture<Void> upsertAsync(StrikeRecord record);

    CompletableFuture<Boolean> deleteByIpAsync(String ip);

    CompletableFuture<Void> deleteAll();
}
