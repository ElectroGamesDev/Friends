package com.electro.friends.database;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;


public class PlayerNamesDAO {
    private final Database database;

    public PlayerNamesDAO(@Nonnull Database database) {
        this.database = database;
    }

    public void saveName(@Nonnull UUID playerUuid, @Nonnull String username) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = database.isMySQL()
                    ? "INSERT INTO player_names (player_uuid, username) VALUES (?, ?) ON DUPLICATE KEY UPDATE username = VALUES(username)"
                    : "INSERT INTO player_names (player_uuid, username) VALUES (?, ?) ON CONFLICT(player_uuid) DO UPDATE SET username = excluded.username";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, username);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error saving player name: " + e.getMessage());
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }
    }

    @Nullable
    public String getName(@Nonnull UUID playerUuid) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "SELECT username FROM player_names WHERE player_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("username");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting player name: " + e.getMessage());
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return null;
    }
}
