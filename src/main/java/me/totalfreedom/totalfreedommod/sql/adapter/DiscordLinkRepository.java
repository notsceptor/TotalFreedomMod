package me.totalfreedom.totalfreedommod.sql.adapter;

import java.sql.SQLException;
import java.util.UUID;

public interface DiscordLinkRepository
{
    /**
     * Insert a new link. Fails if either side is already linked. Callers
     * should remove any existing link first via {@link #deleteByAdminUuid} or
     * {@link #deleteByDiscordUserId}.
     */
    void insert(UUID adminUuid, String discordUserId) throws SQLException;

    /**
     * @return Discord user id linked to {@code adminUuid}, or {@code null} if unlinked.
     */
    String findDiscordIdByAdminUuid(UUID adminUuid) throws SQLException;

    /**
     * @return admin UUID linked to {@code discordUserId}, or {@code null} if unlinked.
     */
    UUID findAdminUuidByDiscordId(String discordUserId) throws SQLException;

    /**
     * @return true if a row was deleted.
     */
    boolean deleteByAdminUuid(UUID adminUuid) throws SQLException;

    /**
     * @return true if a row was deleted.
     */
    boolean deleteByDiscordUserId(String discordUserId) throws SQLException;
}
