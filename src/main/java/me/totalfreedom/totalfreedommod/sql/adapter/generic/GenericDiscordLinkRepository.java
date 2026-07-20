package me.totalfreedom.totalfreedommod.sql.adapter.generic;

import me.totalfreedom.totalfreedommod.sql.StatementHandler;
import me.totalfreedom.totalfreedommod.sql.adapter.DatabaseAdapter;
import me.totalfreedom.totalfreedommod.sql.adapter.DiscordLinkRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * All dialect differences are resolved through the {@link DatabaseAdapter} passed in.
 */
public class GenericDiscordLinkRepository implements DiscordLinkRepository
{
    private final StatementHandler statementHandler;

    private final String insertSql;
    private final String selectDiscordIdSql;
    private final String selectAdminUuidSql;
    private final String deleteByAdminUuidSql;
    private final String deleteByDiscordUserIdSql;

    public GenericDiscordLinkRepository(StatementHandler statementHandler, DatabaseAdapter adapter)
    {
        this.statementHandler = statementHandler;

        String tblDiscordLinks = adapter.quoteIdentifier("discord_links");
        String colAdminUuid = adapter.quoteIdentifier("admin_uuid");
        String colDiscordUserId = adapter.quoteIdentifier("discord_user_id");
        String colLinkedAt = adapter.quoteIdentifier("linked_at");

        this.insertSql = String.format("INSERT INTO %s (%s, %s, %s) VALUES (?, ?, %s)",
                tblDiscordLinks, colAdminUuid, colDiscordUserId, colLinkedAt, adapter.currentTimestamp());
        this.selectDiscordIdSql = String.format("SELECT %s FROM %s WHERE %s = ?", colDiscordUserId, tblDiscordLinks, colAdminUuid);
        this.selectAdminUuidSql = String.format("SELECT %s FROM %s WHERE %s = ?", colAdminUuid, tblDiscordLinks, colDiscordUserId);
        this.deleteByAdminUuidSql = String.format("DELETE FROM %s WHERE %s = ?", tblDiscordLinks, colAdminUuid);
        this.deleteByDiscordUserIdSql = String.format("DELETE FROM %s WHERE %s = ?", tblDiscordLinks, colDiscordUserId);
    }

    @Override
    public void insert(UUID adminUuid, String discordUserId) throws SQLException
    {
        statementHandler.executeUpdate(insertSql, adminUuid.toString(), discordUserId);
    }

    @Override
    public String findDiscordIdByAdminUuid(UUID adminUuid) throws SQLException
    {
        try (ResultSet rs = statementHandler.executeQuery(selectDiscordIdSql, adminUuid.toString()))
        {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    @Override
    public UUID findAdminUuidByDiscordId(String discordUserId) throws SQLException
    {
        try (ResultSet rs = statementHandler.executeQuery(selectAdminUuidSql, discordUserId))
        {
            if (!rs.next())
            {
                return null;
            }
            String raw = rs.getString(1);
            return raw == null ? null : UUID.fromString(raw);
        }
    }

    @Override
    public boolean deleteByAdminUuid(UUID adminUuid) throws SQLException
    {
        return statementHandler.executeUpdate(deleteByAdminUuidSql, adminUuid.toString()) > 0;
    }

    @Override
    public boolean deleteByDiscordUserId(String discordUserId) throws SQLException
    {
        return statementHandler.executeUpdate(deleteByDiscordUserIdSql, discordUserId) > 0;
    }
}
