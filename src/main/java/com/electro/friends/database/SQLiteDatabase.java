package com.electro.friends.database;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class SQLiteDatabase implements Database {
    private final String jdbcUrl;
    private final BlockingQueue<Connection> connectionPool;
    private final int poolSize;
    private volatile boolean closed;

    public SQLiteDatabase(@Nonnull Path databasePath, int poolSize) throws SQLException {
        this.jdbcUrl = "jdbc:sqlite:" + databasePath.toString();
        this.poolSize = poolSize;
        this.connectionPool = new ArrayBlockingQueue<>(poolSize);
        this.closed = false;

        // Load SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
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
        Connection conn = DriverManager.getConnection(jdbcUrl);
        conn.setAutoCommit(true);

        // Enable foreign keys and WAL mode for better performance
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");
        }

        return conn;
    }

    @Nonnull
    public Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("Database is closed");
        }

        Connection conn = connectionPool.poll();
        if (conn == null || conn.isClosed()) {
            conn = createConnection();
        }
        return conn;
    }

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
                        player_uuid TEXT NOT NULL,
                        friend_uuid TEXT NOT NULL,
                        since BIGINT NOT NULL,
                        PRIMARY KEY (player_uuid, friend_uuid)
                    )
                """);

                stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_uuid ON friends(player_uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_friend_uuid ON friends(friend_uuid)");

                // Friend requests table - stores pending requests
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS friend_requests (
                        sender_uuid TEXT NOT NULL,
                        receiver_uuid TEXT NOT NULL,
                        sent_at BIGINT NOT NULL,
                        PRIMARY KEY (sender_uuid, receiver_uuid)
                    )
                """);

                stmt.execute("CREATE INDEX IF NOT EXISTS idx_sender_uuid ON friend_requests(sender_uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_receiver_uuid ON friend_requests(receiver_uuid)");

                // Friend settings table - stores player-specific settings
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS friend_settings (
                        player_uuid TEXT PRIMARY KEY,
                        notifications_enabled INTEGER DEFAULT 1,
                        allow_requests INTEGER DEFAULT 1
                    )
                """);

                // Blocked players table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS blocked_players (
                        player_uuid TEXT NOT NULL,
                        blocked_uuid TEXT NOT NULL,
                        blocked_at BIGINT NOT NULL,
                        PRIMARY KEY (player_uuid, blocked_uuid)
                    )
                """);

                // Player last seen table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_last_seen (
                        player_uuid TEXT PRIMARY KEY,
                        last_seen BIGINT NOT NULL
                    )
                """);

                // Player names table - persistent username cache
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_names (
                        player_uuid TEXT PRIMARY KEY,
                        username TEXT NOT NULL
                    )
                """);

                System.out.println("[Friends] Database schema initialized successfully");
            }
        } finally {
            if (conn != null) {
                releaseConnection(conn);
            }
        }
    }

    public void close() {
        closed = true;

        // Close all connections in the pool
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

    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isMySQL() {
        return false;
    }
}
