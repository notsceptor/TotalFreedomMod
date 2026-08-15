package me.totalfreedom.api.banning;

import java.util.Collection;
import java.util.Set;

import me.totalfreedom.totalfreedommod.banning.Ban;

public interface IBanManager
{
    /**
     * Populate the ban list from SQL where it is available and the JSON snapshot otherwise.
     * Never blocks: the SQL read runs off-thread and is applied back on the main thread.
     */
    void load();

    void awaitPendingWrites(long timeoutMs);

    Set<Ban> getAllBans();

    Collection<Ban> getIpBans();

    Collection<Ban> getUsernameBans();

    /**
     * Blocking write of every ban record. Startup/shutdown only. Single-entry changes belong on
     * a queued save; whole-list changes reachable from a command or event handler belong on
     * {@link #saveAllAsync()}.
     */
    void saveAll();

    void saveAllAsync();

    Ban getByIp(String ip);

    Ban getByUsername(String username);

    Ban unbanIp(String ip);

    Ban unbanUsername(String username);

    boolean isIpBanned(String ip);

    boolean isUsernameBanned(String username);

    boolean addBan(Ban ban);

    boolean removeBan(Ban ban);

    int purge();
}
