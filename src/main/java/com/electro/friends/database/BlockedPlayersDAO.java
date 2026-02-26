package com.electro.friends.database;

import com.electro.friends.model.BlockedPlayer;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class BlockedPlayersDAO {
    private final Database database;

    public BlockedPlayersDAO(@Nonnull Database database) {
        this.database = database;
    }

    public void blockPlayer(@Nonnull UUID playerUuid, @Nonnull UUID blockedUuid, long timestamp) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = database.isMySQL()
                    ? "INSERT IGNORE INTO blocked_players (player_uuid, blocked_uuid, blocked_at) VALUES (?, ?, ?)"
                    : "INSERT OR IGNORE INTO blocked_players (player_uuid, blocked_uuid, blocked_at) VALUES (?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, blockedUuid.toString());
                stmt.setLong(3, timestamp);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error blocking player: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }
    }

    public void unblockPlayer(@Nonnull UUID playerUuid, @Nonnull UUID blockedUuid) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "DELETE FROM blocked_players WHERE player_uuid = ? AND blocked_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, blockedUuid.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error unblocking player: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }
    }

    @Nonnull
    public List<BlockedPlayer> getBlockedPlayers(@Nonnull UUID playerUuid) {
        List<BlockedPlayer> blocked = new ArrayList<>();
        Connection conn = null;

        try {
            conn = database.getConnection();

            String sql = "SELECT blocked_uuid, blocked_at FROM blocked_players WHERE player_uuid = ? ORDER BY blocked_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        UUID blockedUuid = UUID.fromString(rs.getString("blocked_uuid"));
                        long blockedAt = rs.getLong("blocked_at");
                        blocked.add(new BlockedPlayer(blockedUuid, blockedAt));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading blocked players: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return blocked;
    }

    public boolean isBlocked(@Nonnull UUID playerUuid, @Nonnull UUID blockedUuid) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "SELECT 1 FROM blocked_players WHERE player_uuid = ? AND blocked_uuid = ? LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, blockedUuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking block status: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return false;
    }

    public int getBlockedCount(@Nonnull UUID playerUuid) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "SELECT COUNT(*) FROM blocked_players WHERE player_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting blocked count: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return 0;
    }
}
