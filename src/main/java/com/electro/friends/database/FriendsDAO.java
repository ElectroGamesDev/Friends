package com.electro.friends.database;

import com.electro.friends.model.FriendData;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class FriendsDAO {
    private final Database database;

    public FriendsDAO(@Nonnull Database database) {
        this.database = database;
    }

    
    public void addFriend(@Nonnull UUID player1, @Nonnull UUID player2, long timestamp) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = database.isMySQL()
                    ? "INSERT IGNORE INTO friends (player_uuid, friend_uuid, since) VALUES (?, ?, ?)"
                    : "INSERT OR IGNORE INTO friends (player_uuid, friend_uuid, since) VALUES (?, ?, ?)";

            // Add both directions
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Player1 -> Player2
                stmt.setString(1, player1.toString());
                stmt.setString(2, player2.toString());
                stmt.setLong(3, timestamp);
                stmt.executeUpdate();

                // Player2 -> Player1
                stmt.setString(1, player2.toString());
                stmt.setString(2, player1.toString());
                stmt.setLong(3, timestamp);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error adding friend relationship: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }
    }

    
    public void removeFriend(@Nonnull UUID player1, @Nonnull UUID player2) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "DELETE FROM friends WHERE player_uuid = ? AND friend_uuid = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Remove Player1 -> Player2
                stmt.setString(1, player1.toString());
                stmt.setString(2, player2.toString());
                stmt.executeUpdate();

                // Remove Player2 -> Player1
                stmt.setString(1, player2.toString());
                stmt.setString(2, player1.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error removing friend relationship: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }
    }

    
    @Nonnull
    public List<FriendData> getFriends(@Nonnull UUID playerUuid) {
        List<FriendData> friends = new ArrayList<>();
        Connection conn = null;

        try {
            conn = database.getConnection();

            String sql = "SELECT friend_uuid, since FROM friends WHERE player_uuid = ? ORDER BY since DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        UUID friendUuid = UUID.fromString(rs.getString("friend_uuid"));
                        long since = rs.getLong("since");
                        friends.add(new FriendData(friendUuid, since));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading friends: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return friends;
    }

    
    public boolean areFriends(@Nonnull UUID player1, @Nonnull UUID player2) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "SELECT 1 FROM friends WHERE player_uuid = ? AND friend_uuid = ? LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player1.toString());
                stmt.setString(2, player2.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking friendship: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return false;
    }

    
    public int getFriendCount(@Nonnull UUID playerUuid) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "SELECT COUNT(*) FROM friends WHERE player_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting friend count: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return 0;
    }

    
    public long getFriendshipTimestamp(@Nonnull UUID player1, @Nonnull UUID player2) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "SELECT since FROM friends WHERE player_uuid = ? AND friend_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player1.toString());
                stmt.setString(2, player2.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("since");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting friendship timestamp: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return 0;
    }
}
