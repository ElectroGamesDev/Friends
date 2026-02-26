package com.electro.friends.util;

import com.electro.friends.database.PlayerNamesDAO;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


public class UsernameCache {
    private final Map<UUID, String> cache;
    private final PlayerNamesDAO playerNamesDAO;

    public UsernameCache(@Nonnull PlayerNamesDAO playerNamesDAO) {
        this.cache = new ConcurrentHashMap<>();
        this.playerNamesDAO = playerNamesDAO;
    }


    public void cache(@Nonnull UUID uuid, @Nonnull String username) {
        cache.put(uuid, username);
    }

    @Nonnull
    public String getUsernameCached(@Nonnull UUID uuid) {
        // Online player always wins
        PlayerRef player = Universe.get().getPlayer(uuid);
        if (player != null) {
            String username = player.getUsername();
            cache.put(uuid, username);
            return username;
        }

        // Check internal cache
        String cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }

        // Synchronous DB fallback (only for admin lookups where async isn't available)
        String fromDb = playerNamesDAO.getName(uuid);
        if (fromDb != null) {
            cache.put(uuid, fromDb);
            return fromDb;
        }

        return "Unknown";
    }

    @Nonnull
    public CompletableFuture<String> getUsernameAsync(@Nonnull UUID uuid) {
        // Online player always wins
        PlayerRef player = Universe.get().getPlayer(uuid);
        if (player != null) {
            String username = player.getUsername();
            cache.put(uuid, username);
            return CompletableFuture.completedFuture(username);
        }

        // Check internal cache
        String cached = cache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // Async DB fallback
        return CompletableFuture.supplyAsync(() -> {
            String fromDb = playerNamesDAO.getName(uuid);
            if (fromDb != null) {
                cache.put(uuid, fromDb);
                return fromDb;
            }
            return "Unknown";
        });
    }

    @Nonnull
    public String getUsernameOrDefaultCached(
            @Nonnull UUID uuid,
            @Nonnull String defaultValue
    ) {
        // Online player wins
        PlayerRef player = Universe.get().getPlayer(uuid);
        if (player != null) {
            String username = player.getUsername();
            cache.put(uuid, username);
            return username;
        }

        // Check internal cache
        String cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }

        // Synchronous DB fallback
        String fromDb = playerNamesDAO.getName(uuid);
        if (fromDb != null) {
            cache.put(uuid, fromDb);
            return fromDb;
        }

        return defaultValue;
    }

    @Nonnull
    public CompletableFuture<String> getUsernameOrDefaultAsync(
            @Nonnull UUID uuid,
            @Nonnull String defaultValue
    ) {
        // Fast-path: online player
        PlayerRef player = Universe.get().getPlayer(uuid);
        if (player != null) {
            String username = player.getUsername();
            cache.put(uuid, username);
            return CompletableFuture.completedFuture(username);
        }

        // Check internal cache
        String cached = cache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // Async DB fallback
        return CompletableFuture.supplyAsync(() -> {
            String fromDb = playerNamesDAO.getName(uuid);
            if (fromDb != null) {
                cache.put(uuid, fromDb);
                return fromDb;
            }
            return defaultValue;
        });
    }

    public void clear() {
        cache.clear();
    }
}
