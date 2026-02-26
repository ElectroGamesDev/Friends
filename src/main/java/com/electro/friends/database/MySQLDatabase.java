package com.electro.friends.database;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class MySQLDatabase implements Database {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final BlockingQueue<Connection> connectionPool;
    private final int poolSize;
    private volatile boolean closed;

    public MySQLDatabase(@Nonnull String host, int port, @Nonnull String database,
                         @Nonnull String username, @Nonnull String password, int poolSize) throws SQLException {
        this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true";
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
        this.connectionPool = new ArrayBlockingQueue<>(poolSize);
        this.closed = false;

        // Load MySQL JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found", e);
        }

        // Initialize connection pool
        for (int i = 0; i < poolSize; i++) {
            connectionPool.offer(createConnection());
        }

        // Initialize schema
        initializeSchema();
    }

    @Nonnull
    private Connection createConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
        conn.setAutoCommit(true);
        return conn;
    }

    private boolean isConnectionValid(@Nonnull Connection conn) {
        try {
            if (conn.isClosed()) return false;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
            }
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    @Nonnull
    public Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("Database is closed");
        }

        Connection conn = connectionPool.poll();
        if (conn == null || !isConnectionValid(conn)) {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
            conn = createConnection();
        }
        return conn;
    }

    @Override
    public void releaseConnection(@Nonnull Connection connection) {
        if (!closed && connection != null) {
            try {
                if (!connection.isClosed()) {
                    connectionPool.offer(connection);
                }
            } catch (SQLException e) {
                System.err.println("Error checking connection status: " + e.getMessage());
            }
        }
    }

    private void initializeSchema() throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();

            try (Statement stmt = conn.createStatement()) {
                // Friends table - stores friend relationships
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS friends (
                        player_uuid VARCHAR(36) NOT NULL,
                        friend_uuid VARCHAR(36) NOT NULL,
                        since BIGINT NOT NULL,
                        PRIMARY KEY (player_uuid, friend_uuid),
                        INDEX idx_player_uuid (player_uuid),
                        INDEX idx_friend_uuid (friend_uuid)
                    )
                """);

                // Friend requests table - stores pending requests
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS friend_requests (
                        sender_uuid VARCHAR(36) NOT NULL,
                        receiver_uuid VARCHAR(36) NOT NULL,
                        sent_at BIGINT NOT NULL,
                        PRIMARY KEY (sender_uuid, receiver_uuid),
                        INDEX idx_sender_uuid (sender_uuid),
                        INDEX idx_receiver_uuid (receiver_uuid)
                    )
                """);

                // Friend settings table - stores player-specific settings
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS friend_settings (
                        player_uuid VARCHAR(36) PRIMARY KEY,
                        notifications_enabled TINYINT(1) DEFAULT 1,
                        allow_requests TINYINT(1) DEFAULT 1
                    )
                """);

                // Blocked players table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS blocked_players (
                        player_uuid VARCHAR(36) NOT NULL,
                        blocked_uuid VARCHAR(36) NOT NULL,
                        blocked_at BIGINT NOT NULL,
                        PRIMARY KEY (player_uuid, blocked_uuid)
                    )
                """);

                // Player last seen table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_last_seen (
                        player_uuid VARCHAR(36) PRIMARY KEY,
                        last_seen BIGINT NOT NULL
                    )
                """);

                // Player names table - persistent username cache
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_names (
                        player_uuid VARCHAR(36) PRIMARY KEY,
                        username VARCHAR(64) NOT NULL
                    )
                """);

                System.out.println("[Friends] MySQL database schema initialized successfully");
            }
        } finally {
            if (conn != null) {
                releaseConnection(conn);
            }
        }
    }

    @Override
    public void close() {
        closed = true;

        while (!connectionPool.isEmpty()) {
            try {
                Connection conn = connectionPool.poll();
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isMySQL() {
        return true;
    }
}
