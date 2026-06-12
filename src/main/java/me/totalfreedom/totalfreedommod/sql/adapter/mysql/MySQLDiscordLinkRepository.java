package me.totalfreedom.totalfreedommod.sql.adapter.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.sql.StatementHandler;
import me.totalfreedom.totalfreedommod.sql.adapter.DiscordLinkRepository;

public class MySQLDiscordLinkRepository implements DiscordLinkRepository
{
    private final TotalFreedomMod plugin;
    private final StatementHandler statementHandler;

    public MySQLDiscordLinkRepository(TotalFreedomMod plugin, StatementHandler statementHandler)
    {
        this.plugin = plugin;
        this.statementHandler = statementHandler;
    }

    @Override
    public void insert(UUID adminUuid, String discordUserId) throws SQLException
    {
        statementHandler.executeUpdate(
                "INSERT INTO `discord_links` (`admin_uuid`, `discord_user_id`, `linked_at`) VALUES (?, ?, NOW())",
                adminUuid.toString(), discordUserId);
    }

    @Override
    public String findDiscordIdByAdminUuid(UUID adminUuid) throws SQLException
    {
        try (ResultSet rs = statementHandler.executeQuery(
                "SELECT `discord_user_id` FROM `discord_links` WHERE `admin_uuid` = ?", adminUuid.toString()))
        {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    @Override
    public UUID findAdminUuidByDiscordId(String discordUserId) throws SQLException
    {
        try (ResultSet rs = statementHandler.executeQuery(
                "SELECT `admin_uuid` FROM `discord_links` WHERE `discord_user_id` = ?", discordUserId))
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
        return statementHandler.executeUpdate(
                "DELETE FROM `discord_links` WHERE `admin_uuid` = ?", adminUuid.toString()) > 0;
    }

    @Override
    public boolean deleteByDiscordUserId(String discordUserId) throws SQLException
    {
        return statementHandler.executeUpdate(
                "DELETE FROM `discord_links` WHERE `discord_user_id` = ?", discordUserId) > 0;
    }
}
