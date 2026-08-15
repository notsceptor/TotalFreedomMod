package me.totalfreedom.api.admin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.admin.Admin;

public interface IAdminList
{
    /**
     * Populate the list from SQL where it is available and the JSON snapshot otherwise. Never
     * blocks: the SQL read runs off-thread and its result is applied back on the main thread,
     * so this is safe from a command handler as well as from startup.
     */
    void load();

    /**
     * Blocking write of every admin record. Do <b>not</b> call this from a command or event
     * handler: under SQL it is a serial round-trip per admin on the calling thread. Single-entry
     * changes belong on {@link #saveAdminAsync(Admin)}.
     */
    void save();

    /**
     * Wait for queued {@link #saveAdminAsync(Admin)} writes to land, up to {@code timeoutMs}.
     */
    void awaitPendingWrites(long timeoutMs);

    /**
     * Persist <i>every</i> admin record off the main thread. Only call this when the whole list
     * is genuinely dirty. If only a single entry changed, call {@link #saveAdminAsync(Admin)}
     * instead, which queues just that row.
     */
    void saveAsync();

    /**
     * Queue a single admin row for an off-thread write, followed by a refresh of the JSON
     * snapshot. Safe to call from commands and event handlers.
     */
    void saveAdminAsync(Admin admin);

    boolean isAdminSync(CommandSender sender);

    boolean isAdmin(CommandSender sender);

    /**
     * Whether {@code sender} counts as a senior admin.
     */
    boolean isSeniorAdmin(CommandSender sender);

    /**
     * The same test applied to a stored profile rather than to a live sender, for the cleanup
     * pass that runs over admins who are not online to be asked.
     */
    boolean grantsSeniorStatus(Admin admin);

    Admin getAdmin(CommandSender sender);

    Admin getEntryByName(String name);

    Admin getEntryByIp(String ip);

    Admin getEntryByIpFuzzy(String needleIp);

    void updateLastLogin(Player player);

    boolean isAdminImpostor(Player player);

    boolean isIdentityMatched(Player player);

    boolean addAdmin(Admin admin);

    boolean removeAdmin(Admin admin);

    /**
     * Refresh the IP lookup table for a single admin. Use this instead of {@link #updateTables()}
     * when only one entry's IP list has changed.
     */
    void refreshIps(Admin admin);

    void updateTables();

    Map<String, Admin> getAllAdmins();

    Set<Admin> getActiveAdmins();

    Admin getAdminByUuid(UUID uuid);

    Set<String> getAdminNames();

    Set<String> getAdminIps();

    Set<Player> getOnlineAdmins();

    void deactivateOldEntries(boolean verbose);
}
