package me.totalfreedom.totalfreedommod.sql.adapter;

import me.totalfreedom.totalfreedommod.banning.PermBan;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for Permban data.
 * Each database type implements this with database-specific queries.
 */
public interface PermbanRepository
{
    // ============================================
    // CREATE Operations
    // ============================================

    /**
     * Insert a new permban into the database.
     * @param permban The permban object to insert
     * @return The generated permban ID, or -1 on failure
     */
    int insert(PermBan permban) throws SQLException;

    /**
     * Insert IPs for a permban.
     */
    void insertIps(int permbanId, List<String> ips) throws SQLException;

    /**
     * Add a single IP to a permban.
     */
    void addIp(int permbanId, String ip) throws SQLException;

    // ============================================
    // READ Operations
    // ============================================

    /**
     * Load all permbans from the database.
     */
    List<PermBan> loadAll() throws SQLException;

    /**
     * Find a permban by UUID.
     */
    PermBan findByUuid(UUID uuid) throws SQLException;

    /**
     * Find a permban by username (case-insensitive).
     */
    PermBan findByUsername(String username) throws SQLException;

    /**
     * Find a permban by IP address.
     */
    PermBan findByIp(String ip) throws SQLException;

    /**
     * Get the database ID for a permban by UUID.
     */
    int getPermbanId(UUID uuid) throws SQLException;

    /**
     * Get the database ID for a permban by username.
     */
    int getPermbanIdByUsername(String username) throws SQLException;

    /**
     * Get all IPs for a permban.
     */
    List<String> getIps(int permbanId) throws SQLException;

    /**
     * Check if a player is permbanned by UUID.
     */
    boolean isPermBanned(UUID uuid) throws SQLException;

    /**
     * Check if a player is permbanned by username.
     */
    boolean isPermBannedByUsername(String username) throws SQLException;

    /**
     * Check if an IP is permbanned.
     */
    boolean isPermBannedByIp(String ip) throws SQLException;

    // ============================================
    // UPDATE Operations
    // ============================================

    /**
     * Update an existing permban in the database.
     */
    boolean update(PermBan permban) throws SQLException;

    /**
     * Update a permban's reason.
     */
    boolean updateReason(UUID uuid, String reason) throws SQLException;

    /**
     * Sync all IPs for a permban (replace existing with new list).
     */
    void syncIps(int permbanId, List<String> ips) throws SQLException;

    // ============================================
    // DELETE Operations
    // ============================================

    /**
     * Delete a permban by UUID.
     */
    boolean delete(UUID uuid) throws SQLException;

    /**
     * Delete a permban by username.
     */
    boolean deleteByUsername(String username) throws SQLException;

    /**
     * Delete all permbans associated with an IP.
     */
    boolean deleteByIp(String ip) throws SQLException;

    /**
     * Remove a specific IP from a permban.
     */
    boolean removeIp(int permbanId, String ip) throws SQLException;
    
    /**
     * Delete all permbans from the database (synchronous).
     */
    void deleteAllSync() throws SQLException;

    // ============================================
    // Async Operations
    // ============================================

    CompletableFuture<List<PermBan>> loadAllAsync();

    CompletableFuture<Integer> insertAsync(PermBan permban);

    CompletableFuture<Boolean> updateAsync(PermBan permban);

    CompletableFuture<Boolean> deleteAsync(UUID uuid);
    
    /**
     * Save permban asynchronously (insert or update).
     */
    CompletableFuture<Integer> save(PermBan permban);
    
    /**
     * Find all permbans asynchronously.
     */
    CompletableFuture<List<PermBan>> findAll();
    
    /**
     * Delete all permbans asynchronously.
     */
    CompletableFuture<Void> deleteAll();
}
