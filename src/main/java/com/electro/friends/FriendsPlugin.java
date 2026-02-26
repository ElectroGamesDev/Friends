package com.electro.friends;

import com.electro.friends.api.FriendsAPI;
import com.electro.friends.command.FriendsCommands;
import com.electro.friends.database.BlockedPlayersDAO;
import com.electro.friends.database.Database;
import com.electro.friends.database.FriendRequestsDAO;
import com.electro.friends.database.FriendSettingsDAO;
import com.electro.friends.database.FriendsDAO;
import com.electro.friends.database.LastSeenDAO;
import com.electro.friends.database.MySQLDatabase;
import com.electro.friends.database.PlayerNamesDAO;
import com.electro.friends.database.SQLiteDatabase;
import com.electro.friends.listener.FriendsConnectionListener;
import com.electro.friends.manager.FriendsManager;
import com.electro.friends.ui.FriendsUI;
import com.electro.friends.util.ConfigManager;
import com.electro.friends.util.Lang;
import com.electro.friends.util.UsernameCache;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;


public class FriendsPlugin extends JavaPlugin {
    private static FriendsPlugin instance;

    // Database
    private Database database;
    private FriendsDAO friendsDAO;
    private FriendRequestsDAO requestsDAO;
    private FriendSettingsDAO settingsDAO;
    private BlockedPlayersDAO blockedPlayersDAO;
    private LastSeenDAO lastSeenDAO;
    private PlayerNamesDAO playerNamesDAO;

    // Managers
    private FriendsManager friendsManager;
    private ConfigManager configManager;

    // API
    private FriendsAPI api;

    // Listeners
    private FriendsConnectionListener connectionListener;

    // Utils
    private UsernameCache usernameCache;
    private Lang lang;

    // Config values
    private ConfigManager pluginConfig;
    private int maxFriends = 100;
    private int requestExpiryDays = 7;
    private int requestCooldownMinutes = 5;
    private int cleanupIntervalMinutes = 60;
    private int connectionPoolSize = 10;

    public FriendsUI friendsUI;

    public FriendsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        try {
            // Create plugin data folder
            Path dataFolder = Paths.get("mods", "FriendsData");
            Files.createDirectories(dataFolder);

            getLogger().atInfo().log("Initializing Friends plugin...");

            // Initialize config manager
            this.configManager = new ConfigManager(Paths.get("mods", "FriendsData"));

            // Initialize language file
            this.lang = new Lang(dataFolder);

            // Initialize plugin config (config.json)
            pluginConfig = new ConfigManager(dataFolder, "config.json");
            initializeDefaultConfig(pluginConfig);

            // Read config values
            maxFriends = pluginConfig.getInt("maxFriends", 100);
            requestExpiryDays = pluginConfig.getInt("requestExpiryDays", 7);
            requestCooldownMinutes = pluginConfig.getInt("requestCooldownMinutes", 5);
            cleanupIntervalMinutes = pluginConfig.getInt("cleanupIntervalMinutes", 60);
            connectionPoolSize = pluginConfig.getInt("connectionPoolSize", 10);

            // Initialize database based on config
            String dbType = pluginConfig.getString("database.type", "sqlite").toLowerCase();
            try {
                if ("mysql".equals(dbType)) {
                    String host = pluginConfig.getString("database.mysql.host", "localhost");
                    int port = pluginConfig.getInt("database.mysql.port", 3306);
                    String dbName = pluginConfig.getString("database.mysql.database", "friends");
                    String username = pluginConfig.getString("database.mysql.username", "root");
                    String password = pluginConfig.getString("database.mysql.password", "");

                    database = new MySQLDatabase(host, port, dbName, username, password, connectionPoolSize);
                    getLogger().atInfo().log("MySQL database initialized successfully");
                } else {
                    Path databasePath = dataFolder.resolve("friends.db");
                    database = new SQLiteDatabase(databasePath, connectionPoolSize);
                    getLogger().atInfo().log("SQLite database initialized successfully");
                }

                friendsDAO = new FriendsDAO(database);
                requestsDAO = new FriendRequestsDAO(database);
                settingsDAO = new FriendSettingsDAO(database);
                blockedPlayersDAO = new BlockedPlayersDAO(database);
                lastSeenDAO = new LastSeenDAO(database);
                playerNamesDAO = new PlayerNamesDAO(database);
            } catch (SQLException e) {
                getLogger().atSevere().log("Failed to initialize database", e);
                throw new RuntimeException("Database initialization failed", e);
            }

            // Initialize username cache
            usernameCache = new UsernameCache(playerNamesDAO);
            getLogger().atInfo().log("Username cache initialized");

            // Initialize managers
            friendsManager = new FriendsManager(friendsDAO, requestsDAO, settingsDAO, blockedPlayersDAO, lastSeenDAO,
                    maxFriends, requestExpiryDays, requestCooldownMinutes);
            getLogger().atInfo().log("Managers initialized");

            // Initialize API
            api = new FriendsAPI(friendsManager);
            getLogger().atInfo().log("API initialized");

            // Initialize listeners
            connectionListener = new FriendsConnectionListener(this, friendsManager, usernameCache, lastSeenDAO, playerNamesDAO);

            // Register event listeners
            registerEventListeners();
            getLogger().atInfo().log("Event listeners registered");

            // Other
            friendsUI = new FriendsUI(this, requestExpiryDays);

            // Register commands
            registerCommands();
            getLogger().atInfo().log("Commands registered");

            getLogger().atInfo().log("Friends plugin setup complete!");

        } catch (Exception e) {
            getLogger().atSevere().log("Failed to set up Friends plugin", e);
            throw new RuntimeException("Plugin setup failed", e);
        }
    }

    @Override
    protected void start() {
        getLogger().atInfo().log("Starting Friends plugin...");

        // Schedule cleanup task for expired friend requests
        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                int deleted = friendsManager.cleanupExpiredRequests();
                if (deleted > 0) {
                    getLogger().atInfo().log("Cleaned up " + deleted + " expired friend requests");
                }
            } catch (Exception e) {
                getLogger().atWarning().log("Error during friend request cleanup", e);
            }
        }, cleanupIntervalMinutes, cleanupIntervalMinutes, TimeUnit.MINUTES);

        getLogger().atInfo().log("Friends plugin started successfully!");
    }

    @Override
    protected void shutdown() {
        getLogger().atInfo().log("Shutting down Friends plugin...");

        try {
            // Close database
            if (database != null && !database.isClosed()) {
                getLogger().atInfo().log("Closing database...");
                database.close();
                getLogger().atInfo().log("Database closed");
            }

            getLogger().atInfo().log("Friends plugin shut down successfully!");

        } catch (Exception e) {
            getLogger().atSevere().log("Error during shutdown", e);
        }
    }

    public void reloadConfig() {
        pluginConfig.reload();
        lang.reload();
        maxFriends = pluginConfig.getInt("maxFriends", 100);
        requestExpiryDays = pluginConfig.getInt("requestExpiryDays", 7);
        requestCooldownMinutes = pluginConfig.getInt("requestCooldownMinutes", 5);
        cleanupIntervalMinutes = pluginConfig.getInt("cleanupIntervalMinutes", 60);

        friendsManager.updateConfig(maxFriends, requestExpiryDays, requestCooldownMinutes);
    }

    private void registerEventListeners() {
        getEventRegistry().register(PlayerConnectEvent.class, connectionListener::onPlayerConnect);
        getEventRegistry().register(PlayerDisconnectEvent.class, connectionListener::onPlayerDisconnect);
    }

    private void initializeDefaultConfig(@Nonnull ConfigManager pluginConfig) {
        if (pluginConfig.get("database.type") == null) {
            pluginConfig.beginBatch();
            pluginConfig.set("database.type", "sqlite");
            pluginConfig.set("database.mysql.host", "localhost");
            pluginConfig.set("database.mysql.port", 3306);
            pluginConfig.set("database.mysql.database", "friends");
            pluginConfig.set("database.mysql.username", "root");
            pluginConfig.set("database.mysql.password", "");
            pluginConfig.set("maxFriends", 100);
            pluginConfig.set("requestExpiryDays", 7);
            pluginConfig.set("requestCooldownMinutes", 5);
            pluginConfig.set("cleanupIntervalMinutes", 60);
            pluginConfig.set("connectionPoolSize", 10);
            pluginConfig.endBatch();
        }
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new FriendsCommands(this));
    }

    @Nonnull
    public static FriendsPlugin get() {
        return instance;
    }

    @Nonnull
    public FriendsAPI getAPI() {
        return api;
    }

    @Nonnull
    public FriendsManager getFriendsManager() {
        return friendsManager;
    }

    @Nonnull
    public UsernameCache getUsernameCache() {
        return usernameCache;
    }

    @Nonnull
    public Database getDatabase() {
        return database;
    }

    @Nonnull
    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    @Nonnull
    public ConfigManager getPluginConfig() {
        return pluginConfig;
    }

    public int getMaxFriends() {
        return maxFriends;
    }

    public int getRequestExpiryDays() {
        return requestExpiryDays;
    }

    @Nonnull
    public Lang getLang() {
        return lang;
    }
}
