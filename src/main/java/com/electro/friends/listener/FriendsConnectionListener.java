package com.electro.friends.listener;

import com.electro.friends.FriendsPlugin;
import com.electro.friends.database.LastSeenDAO;
import com.electro.friends.database.PlayerNamesDAO;
import com.electro.friends.manager.FriendsManager;
import com.electro.friends.model.FriendsProfile;
import com.electro.friends.util.Lang;
import com.electro.friends.util.UsernameCache;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.UUID;


public class FriendsConnectionListener {
    private final FriendsPlugin plugin;
    private final FriendsManager friendsManager;
    private final UsernameCache usernameCache;
    private final LastSeenDAO lastSeenDAO;
    private final PlayerNamesDAO playerNamesDAO;

    public FriendsConnectionListener(
            @Nonnull FriendsPlugin plugin,
            @Nonnull FriendsManager friendsManager,
            @Nonnull UsernameCache usernameCache,
            @Nonnull LastSeenDAO lastSeenDAO,
            @Nonnull PlayerNamesDAO playerNamesDAO) {
        this.plugin = plugin;
        this.friendsManager = friendsManager;
        this.usernameCache = usernameCache;
        this.lastSeenDAO = lastSeenDAO;
        this.playerNamesDAO = playerNamesDAO;
    }

    public void onPlayerConnect(@Nonnull PlayerConnectEvent event) {
        PlayerRef player = event.getPlayerRef();
        UUID uuid = player.getUuid();
        String username = player.getUsername();

        plugin.getLogger().atInfo().log("Loading friends profile for: " + username + " (" + uuid + ")");

        // Cache username and persist to DB so offline lookups work
        usernameCache.cache(uuid, username);
        playerNamesDAO.saveName(uuid, username);

        // Load profile asynchronously
        friendsManager.loadProfile(uuid).thenAccept(profile -> {
            plugin.getLogger().atInfo().log(
                    "Friends profile loaded for " + username +
                    " - Friends: " + profile.getFriendCount() +
                    ", Incoming Requests: " + profile.getIncomingRequests().size()
            );

            // Mark player as online
            friendsManager.markOnline(uuid);

            // Send join message to online friends
            notifyFriendsJoin(player, profile);

            // Show online friends count to the joining player
            showOnlineFriendsCount(player, profile);
        }).exceptionally(throwable -> {
            plugin.getLogger().atSevere().log("Failed to load friends profile for " + username, throwable);
            return null;
        });
    }

    public void onPlayerDisconnect(@Nonnull PlayerDisconnectEvent event) {
        PlayerRef player = event.getPlayerRef();
        UUID uuid = player.getUuid();
        String username = player.getUsername();

        plugin.getLogger().atInfo().log("Unloading friends profile for: " + username + " (" + uuid + ")");

        FriendsProfile profile = friendsManager.getProfile(uuid);
        if (profile != null) {
            // Notify friends that player left
            notifyFriendsLeave(player, profile);
        }

        // Update last seen timestamp
        lastSeenDAO.updateLastSeen(uuid, System.currentTimeMillis());

        // Mark player as offline
        friendsManager.markOffline(uuid);

        // Unload profile
        friendsManager.unloadProfile(uuid);

        plugin.getLogger().atInfo().log("Friends profile unloaded for " + username);
    }

    private void notifyFriendsJoin(@Nonnull PlayerRef player, @Nonnull FriendsProfile profile) {
        List<UUID> onlineFriends = friendsManager.getOnlineFriends(player.getUuid());

        for (UUID friendUuid : onlineFriends) {
            FriendsProfile friendProfile = friendsManager.getProfile(friendUuid);
            if (friendProfile == null || !friendProfile.getSettings().isNotificationsEnabled()) {
                continue;
            }

            // Get friend's PlayerRef
            PlayerRef friendRef = Universe.get().getPlayer(friendUuid);
            if (friendRef != null) {
                Lang lang = plugin.getLang();
                String[] parts = lang.get("notify.join").split("\\{player\\}", 2);
                Message message = Message.raw("")
                        .insert(Message.raw(lang.get("prefix") + " ").color(Color.YELLOW).bold(true))
                        .insert(Message.raw(parts[0]).color(Color.GRAY))
                        .insert(Message.raw(player.getUsername()).color(Color.GREEN))
                        .insert(Message.raw(parts.length > 1 ? parts[1] : "").color(Color.GRAY));

                friendRef.sendMessage(message);
            }
        }
    }

    private void notifyFriendsLeave(@Nonnull PlayerRef player, @Nonnull FriendsProfile profile) {
        List<UUID> onlineFriends = friendsManager.getOnlineFriends(player.getUuid());

        for (UUID friendUuid : onlineFriends) {
            FriendsProfile friendProfile = friendsManager.getProfile(friendUuid);
            if (friendProfile == null || !friendProfile.getSettings().isNotificationsEnabled()) {
                continue;
            }

            // Get friend's PlayerRef
            PlayerRef friendRef = Universe.get().getPlayer(friendUuid);
            if (friendRef != null) {
                Lang lang = plugin.getLang();
                String[] parts = lang.get("notify.leave").split("\\{player\\}", 2);
                Message message = Message.raw("")
                        .insert(Message.raw(lang.get("prefix") + " ").color(Color.YELLOW).bold(true))
                        .insert(Message.raw(parts[0]).color(Color.GRAY))
                        .insert(Message.raw(player.getUsername()).color(Color.RED))
                        .insert(Message.raw(parts.length > 1 ? parts[1] : "").color(Color.GRAY));

                friendRef.sendMessage(message);
            }
        }
    }

    private void showOnlineFriendsCount(@Nonnull PlayerRef player, @Nonnull FriendsProfile profile) {
        int onlineFriendsCount = friendsManager.getOnlineFriendsCount(player.getUuid());
        int totalFriends = profile.getFriendCount();

        // Only show if player has friends
        if (totalFriends == 0) {
            return;
        }

        Lang lang = plugin.getLang();
        String onlineCountText = lang.format("notify.friends-online",
                "online", String.valueOf(onlineFriendsCount),
                "total", String.valueOf(totalFriends));
        Message message = Message.raw("")
                .insert(Message.raw(lang.get("prefix") + " ").color(Color.YELLOW).bold(true))
                .insert(Message.raw(onlineCountText).color(Color.GRAY));

        player.sendMessage(message);
    }
}
