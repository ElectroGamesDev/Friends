package com.electro.friends.database;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class LastSeenDAO {
    private final Database database;

    public LastSeenDAO(@Nonnull Database database) {
        this.database = database;
    }

    public void updateLastSeen(@Nonnull UUID playerUuid, long timestamp) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = database.isMySQL()
                    ? "INSERT INTO player_last_seen (player_uuid, last_seen) VALUES (?, ?) ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen)"
                    : "INSERT INTO player_last_seen (player_uuid, last_seen) VALUES (?, ?) ON CONFLICT(player_uuid) DO UPDATE SET last_seen = excluded.last_seen";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setLong(2, timestamp);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error updating last seen: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }
    }

    public long getLastSeen(@Nonnull UUID playerUuid) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "SELECT last_seen FROM player_last_seen WHERE player_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("last_seen");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting last seen: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return 0;
    }

    @Nonnull
    public Map<UUID, Long> getLastSeenBulk(@Nonnull List<UUID> playerUuids) {
        Map<UUID, Long> results = new HashMap<>();
        if (playerUuids.isEmpty()) {
            return results;
        }

        Connection conn = null;
        try {
            conn = database.getConnection();

            StringBuilder sqlBuilder = new StringBuilder("SELECT player_uuid, last_seen FROM player_last_seen WHERE player_uuid IN (");
            for (int i = 0; i < playerUuids.size(); i++) {
                sqlBuilder.append(i > 0 ? ",?" : "?");
            }
            sqlBuilder.append(")");

            try (PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
                for (int i = 0; i < playerUuids.size(); i++) {
                    stmt.setString(i + 1, playerUuids.get(i).toString());
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                        long lastSeen = rs.getLong("last_seen");
                        results.put(uuid, lastSeen);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting bulk last seen: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return results;
    }
}
