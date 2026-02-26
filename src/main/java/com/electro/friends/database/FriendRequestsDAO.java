package com.electro.friends.database;

import com.electro.friends.model.FriendRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class FriendRequestsDAO {
    private final Database database;

    public FriendRequestsDAO(@Nonnull Database database) {
        this.database = database;
    }

    
    public void createRequest(@Nonnull UUID sender, @Nonnull UUID receiver, long timestamp) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = database.isMySQL()
                    ? "INSERT IGNORE INTO friend_requests (sender_uuid, receiver_uuid, sent_at) VALUES (?, ?, ?)"
                    : "INSERT OR IGNORE INTO friend_requests (sender_uuid, receiver_uuid, sent_at) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sender.toString());
                stmt.setString(2, receiver.toString());
                stmt.setLong(3, timestamp);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error creating friend request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }
    }

    
    public void deleteRequest(@Nonnull UUID sender, @Nonnull UUID receiver) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "DELETE FROM friend_requests WHERE sender_uuid = ? AND receiver_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sender.toString());
                stmt.setString(2, receiver.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error deleting friend request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }
    }

    
    @Nullable
    public FriendRequest getRequest(@Nonnull UUID sender, @Nonnull UUID receiver) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "SELECT * FROM friend_requests WHERE sender_uuid = ? AND receiver_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sender.toString());
                stmt.setString(2, receiver.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new FriendRequest(
                                UUID.fromString(rs.getString("sender_uuid")),
                                UUID.fromString(rs.getString("receiver_uuid")),
                                rs.getLong("sent_at")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting friend request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return null;
    }

    
    @Nonnull
    public List<FriendRequest> getIncomingRequests(@Nonnull UUID playerUuid) {
        List<FriendRequest> requests = new ArrayList<>();
        Connection conn = null;

        try {
            conn = database.getConnection();

            String sql = "SELECT * FROM friend_requests WHERE receiver_uuid = ? ORDER BY sent_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        requests.add(new FriendRequest(
                                UUID.fromString(rs.getString("sender_uuid")),
                                UUID.fromString(rs.getString("receiver_uuid")),
                                rs.getLong("sent_at")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading incoming friend requests: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return requests;
    }

    
    @Nonnull
    public List<FriendRequest> getOutgoingRequests(@Nonnull UUID playerUuid) {
        List<FriendRequest> requests = new ArrayList<>();
        Connection conn = null;

        try {
            conn = database.getConnection();

            String sql = "SELECT * FROM friend_requests WHERE sender_uuid = ? ORDER BY sent_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        requests.add(new FriendRequest(
                                UUID.fromString(rs.getString("sender_uuid")),
                                UUID.fromString(rs.getString("receiver_uuid")),
                                rs.getLong("sent_at")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading outgoing friend requests: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return requests;
    }

    
    public boolean requestExists(@Nonnull UUID sender, @Nonnull UUID receiver) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "SELECT 1 FROM friend_requests WHERE sender_uuid = ? AND receiver_uuid = ? LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, sender.toString());
                stmt.setString(2, receiver.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking friend request existence: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return false;
    }

    
    public void deleteAllRequestsForPlayer(@Nonnull UUID playerUuid) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            String sql = "DELETE FROM friend_requests WHERE sender_uuid = ? OR receiver_uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, playerUuid.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error deleting all requests for player: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }
    }

    
    public int deleteOldRequests(long olderThanMillis) {
        Connection conn = null;
        try {
            conn = database.getConnection();

            long cutoffTime = System.currentTimeMillis() - olderThanMillis;
            String sql = "DELETE FROM friend_requests WHERE sent_at < ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, cutoffTime);
                return stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error deleting old friend requests: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                database.releaseConnection(conn);
            }
        }

        return 0;
    }
}
