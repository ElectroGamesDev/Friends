package com.electro.friends.database;

import com.electro.friends.model.FriendSettings;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;


public class FriendSettingsDAO {
    private final Database database;

    public FriendSettingsDAO(@Nonnull Database database) {
        this.database = database;
    }

    
    @Nonnull
    public FriendSettings loadSettings(@Nonnull UUID playerUuid) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "SELECT * FROM friend_settings WHERE player_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new FriendSettings(
                                playerUuid,
                                rs.getInt("notifications_enabled") == 1,
                                rs.getInt("allow_requests") == 1
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading friend settings: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        // Return default settings if none exist
        return new FriendSettings(playerUuid, true, true);
    }

    
    public void saveSettings(@Nonnull FriendSettings settings) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = database.isMySQL()
                    ? """
                        INSERT INTO friend_settings (player_uuid, notifications_enabled, allow_requests)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            notifications_enabled = VALUES(notifications_enabled),
                            allow_requests = VALUES(allow_requests)
                      """
                    : """
                        INSERT INTO friend_settings (player_uuid, notifications_enabled, allow_requests)
                        VALUES (?, ?, ?)
                        ON CONFLICT(player_uuid) DO UPDATE SET
                            notifications_enabled = excluded.notifications_enabled,
                            allow_requests = excluded.allow_requests
                      """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, settings.getPlayerUuid().toString());
                stmt.setInt(2, settings.isNotificationsEnabled() ? 1 : 0);
                stmt.setInt(3, settings.isAllowRequests() ? 1 : 0);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error saving friend settings: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }
    }

    
    public void setNotificationsEnabled(@Nonnull UUID playerUuid, boolean enabled) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = database.isMySQL()
                    ? """
                        INSERT INTO friend_settings (player_uuid, notifications_enabled, allow_requests)
                        VALUES (?, ?, 1)
                        ON DUPLICATE KEY UPDATE
                            notifications_enabled = VALUES(notifications_enabled)
                      """
                    : """
                        INSERT INTO friend_settings (player_uuid, notifications_enabled, allow_requests)
                        VALUES (?, ?, 1)
                        ON CONFLICT(player_uuid) DO UPDATE SET
                            notifications_enabled = excluded.notifications_enabled
                      """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setInt(2, enabled ? 1 : 0);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error updating notification settings: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }
    }

    
    public void setAllowRequests(@Nonnull UUID playerUuid, boolean allow) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = database.isMySQL()
                    ? """
                        INSERT INTO friend_settings (player_uuid, notifications_enabled, allow_requests)
                        VALUES (?, 1, ?)
                        ON DUPLICATE KEY UPDATE
                            allow_requests = VALUES(allow_requests)
                      """
                    : """
                        INSERT INTO friend_settings (player_uuid, notifications_enabled, allow_requests)
                        VALUES (?, 1, ?)
                        ON CONFLICT(player_uuid) DO UPDATE SET
                            allow_requests = excluded.allow_requests
                      """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setInt(2, allow ? 1 : 0);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error updating allow requests setting: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }
    }
}
