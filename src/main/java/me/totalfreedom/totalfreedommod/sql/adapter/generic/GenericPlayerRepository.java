package me.totalfreedom.totalfreedommod.sql.adapter.generic;

import me.totalfreedom.totalfreedommod.player.CommandSpyMode;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.sql.StatementHandler;
import me.totalfreedom.totalfreedommod.sql.adapter.DatabaseAdapter;
import me.totalfreedom.totalfreedommod.sql.adapter.PlayerRepository;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import reactor.core.publisher.Mono;

/**
 * All dialect differences are resolved through the {@link DatabaseAdapter} passed in.
 */
public class GenericPlayerRepository implements PlayerRepository
{
    private final StatementHandler statementHandler;
    private final DatabaseAdapter adapter;

    private final String tblPlayers;
    private final String tblPlayerIps;
    private final String colUsername;
    private final String colFirstJoin;
    private final String colLastJoin;
    private final String colPotionSpy;
    private final String colCommandSpyMode;
    private final String colMuted;
    private final String colFrozen;
    private final String colCommandsBlocked;
    private final String colStrikes;
    private final String colSavedTag;
    private final String colNickname;
    private final String colId;
    private final String colPlayerUsername;
    private final String colIp;
    private final String selectColumns;

    public GenericPlayerRepository(StatementHandler statementHandler, DatabaseAdapter adapter)
    {
        this.statementHandler = statementHandler;
        this.adapter = adapter;

        this.tblPlayers = adapter.quoteIdentifier("players");
        this.tblPlayerIps = adapter.quoteIdentifier("player_ips");
        this.colUsername = adapter.quoteIdentifier("username");
        this.colFirstJoin = adapter.quoteIdentifier("first_join_unix");
        this.colLastJoin = adapter.quoteIdentifier("last_join_unix");
        this.colPotionSpy = adapter.quoteIdentifier("potion_spy");
        this.colCommandSpyMode = adapter.quoteIdentifier("command_spy_mode");
        this.colMuted = adapter.quoteIdentifier("muted");
        this.colFrozen = adapter.quoteIdentifier("frozen");
        this.colCommandsBlocked = adapter.quoteIdentifier("commands_blocked");
        this.colStrikes = adapter.quoteIdentifier("strikes");
        this.colSavedTag = adapter.quoteIdentifier("saved_tag");
        this.colNickname = adapter.quoteIdentifier("nickname");
        this.colId = adapter.quoteIdentifier("id");
        this.colPlayerUsername = adapter.quoteIdentifier("username");
        this.colIp = adapter.quoteIdentifier("ip");
        this.selectColumns = String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s, %s",
                colUsername, colFirstJoin, colLastJoin, colPotionSpy, colCommandSpyMode, colMuted, colFrozen,
                colCommandsBlocked, colStrikes, colSavedTag)
                + ", " + colNickname;
    }

    @Override
    public void insert(PlayerData data) throws SQLException
    {
        String sql = String.format("INSERT INTO %s (%s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", tblPlayers, selectColumns);

        statementHandler.executeUpdate(sql,
                data.getUsername(),
                data.getFirstJoinUnix(),
                data.getLastJoinUnix(),
                data.isPotionSpy(),
                data.getCommandSpyMode().getName(),
                data.isMuted(),
                data.isFrozen(),
                data.isCommandsBlocked(),
                data.getStrikes(),
                data.getSavedTag(),
                serializeNickname(data));

        insertIps(data.getUsername(), data.getIps());
    }

    @Override
    public void insertIps(String username, List<String> ips) throws SQLException
    {
        if (ips == null || ips.isEmpty()) return;

        String sql = String.format("%s INTO %s (%s, %s) VALUES (?, ?)%s",
                adapter.insertIgnoreSyntax(), tblPlayerIps, colPlayerUsername, colIp, adapter.insertIgnoreSuffix());
        for (String ip : ips)
        {
            statementHandler.executeUpdate(sql, username, ip);
        }
    }

    @Override
    public void addIp(String username, String ip) throws SQLException
    {
        String sql = String.format("%s INTO %s (%s, %s) VALUES (?, ?)%s",
                adapter.insertIgnoreSyntax(), tblPlayerIps, colPlayerUsername, colIp, adapter.insertIgnoreSuffix());
        statementHandler.executeUpdate(sql, username, ip);
    }

    @Override
    public Map<String, PlayerData> loadAll() throws SQLException
    {
        Map<String, PlayerData> players = new HashMap<>();

        String sql = String.format("SELECT %s FROM %s", selectColumns, tblPlayers);
        try (ResultSet rs = statementHandler.executeQuery(sql))
        {
            while (rs.next())
            {
                PlayerData data = loadPlayerFromRow(rs);
                players.put(data.getUsername(), data);
            }
        }

        String ipSql = String.format("SELECT %s, %s FROM %s ORDER BY %s ASC", colPlayerUsername, colIp, tblPlayerIps, colId);
        try (ResultSet rs = statementHandler.executeQuery(ipSql))
        {
            while (rs.next())
            {
                PlayerData data = players.get(rs.getString("username"));
                if (data != null)
                {
                    data.addIp(rs.getString("ip"));
                }
            }
        }

        return players;
    }

    @Override
    public PlayerData findByUsername(String username) throws SQLException
    {
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?", selectColumns, tblPlayers, colUsername);
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
             ResultSet rs = stmt.executeQuery())
        {
            if (rs.next())
            {
                PlayerData data = loadPlayerFromRow(rs);
                getIps(data.getUsername()).forEach(data::addIp);
                return data;
            }
        }
        return null;
    }

    @Override
    public boolean exists(String username) throws SQLException
    {
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s = ?", tblPlayers, colUsername);
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
             ResultSet rs = stmt.executeQuery())
        {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    @Override
    public List<String> getIps(String username) throws SQLException
    {
        List<String> ips = new ArrayList<>();
        String sql = String.format("SELECT %s FROM %s WHERE %s = ? ORDER BY %s ASC", colIp, tblPlayerIps, colPlayerUsername, colId);
        try (PreparedStatement stmt = statementHandler.prepareStatement(sql, username);
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
    public boolean update(PlayerData data) throws SQLException
    {
        String sql = String.format("UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = ?, %s = ?, %s = ?, %s = ?, %s = ?, %s = ? WHERE %s = ?",
                tblPlayers, colFirstJoin, colLastJoin, colPotionSpy, colCommandSpyMode, colMuted, colFrozen,
                colCommandsBlocked, colStrikes, colSavedTag, colUsername);

        int rows = statementHandler.executeUpdate(sql,
                data.getFirstJoinUnix(),
                data.getLastJoinUnix(),
                data.isPotionSpy(),
                data.getCommandSpyMode().getName(),
                data.isMuted(),
                data.isFrozen(),
                data.isCommandsBlocked(),
                data.getStrikes(),
                data.getSavedTag(),
                data.getUsername());

        statementHandler.executeUpdate(
                String.format("UPDATE %s SET %s = ? WHERE %s = ?", tblPlayers, colNickname, colUsername),
                serializeNickname(data), data.getUsername());

        return rows > 0;
    }

    @Override
    public void syncIps(String username, List<String> ips) throws SQLException
    {
        statementHandler.executeUpdate(String.format("DELETE FROM %s WHERE %s = ?", tblPlayerIps, colPlayerUsername), username);
        insertIps(username, ips);
    }

    @Override
    public void saveOrUpdate(PlayerData data) throws SQLException
    {
        if (exists(data.getUsername()))
        {
            update(data);
            syncIps(data.getUsername(), data.getIps());
        }
        else
        {
            insert(data);
        }
    }

    @Override
    public boolean delete(String username) throws SQLException
    {
        String sql = String.format("DELETE FROM %s WHERE %s = ?", tblPlayers, colUsername);
        return statementHandler.executeUpdate(sql, username) > 0;
    }

    @Override
    public void deleteAllSync() throws SQLException
    {
        statementHandler.executeUpdate(String.format("DELETE FROM %s", tblPlayerIps));
        statementHandler.executeUpdate(String.format("DELETE FROM %s", tblPlayers));
    }

    @Override
    public Mono<Map<String, PlayerData>> loadAllAsync()
    {
        return statementHandler.supplyMono(this::loadAll);
    }

    @Override
    public Mono<Void> save(PlayerData data)
    {
        return statementHandler.runMono(() -> saveOrUpdate(data));
    }

    @Override
    public Mono<Boolean> deleteAsync(String username)
    {
        return statementHandler.supplyMono(() -> delete(username));
    }

    @Override
    public Mono<Void> deleteAll()
    {
        return statementHandler.runMono(this::deleteAllSync);
    }

    private PlayerData loadPlayerFromRow(ResultSet rs) throws SQLException
    {
        PlayerData data = new PlayerData(rs.getString("username"));
        data.setFirstJoinUnix(rs.getLong("first_join_unix"));
        data.setLastJoinUnix(rs.getLong("last_join_unix"));
        data.setPotionSpy(rs.getBoolean("potion_spy"));
        data.setCommandSpyMode(CommandSpyMode.fromString(rs.getString("command_spy_mode")));
        data.setMuted(rs.getBoolean("muted"));
        data.setFrozen(rs.getBoolean("frozen"));
        data.setCommandsBlocked(rs.getBoolean("commands_blocked"));
        data.setStrikes(rs.getInt("strikes"));
        data.setSavedTag(rs.getString("saved_tag"));

        String rawNickname = rs.getString("nickname");
        if (rawNickname != null && !rawNickname.isEmpty())
        {
            data.setNicknameRaw(AdventureUtil.legacyToComponent(rawNickname));
        }

        return data;
    }

    private static String serializeNickname(PlayerData data)
    {
        return data.getNickname() != null ? AdventureUtil.componentToLegacy(data.getNickname()) : null;
    }
}
