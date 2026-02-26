package com.electro.friends.manager;

import com.electro.friends.database.BlockedPlayersDAO;
import com.electro.friends.database.FriendRequestsDAO;
import com.electro.friends.database.FriendSettingsDAO;
import com.electro.friends.database.FriendsDAO;
import com.electro.friends.database.LastSeenDAO;
import com.electro.friends.model.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FriendsManager {
    private final Map<UUID, FriendsProfile> cache;
    private final FriendsDAO friendsDAO;
    private final FriendRequestsDAO requestsDAO;
    private final FriendSettingsDAO settingsDAO;
    private final BlockedPlayersDAO blockedPlayersDAO;
    private final LastSeenDAO lastSeenDAO;

    // Track online players for notifications
    private final Set<UUID> onlinePlayers;

    // Request cooldown tracking: key = sender:receiver
    private final Map<String, Long> requestCooldowns;

    // Config values
    private int maxFriends;
    private long requestExpiryMs;
    private long requestCooldownMs;

    public FriendsManager(
            @Nonnull FriendsDAO friendsDAO,
            @Nonnull FriendRequestsDAO requestsDAO,
            @Nonnull FriendSettingsDAO settingsDAO,
            @Nonnull BlockedPlayersDAO blockedPlayersDAO,
            @Nonnull LastSeenDAO lastSeenDAO,
            int maxFriends,
            int requestExpiryDays,
            int requestCooldownMinutes) {
        this.cache = new ConcurrentHashMap<>();
        this.friendsDAO = friendsDAO;
        this.requestsDAO = requestsDAO;
        this.settingsDAO = settingsDAO;
        this.blockedPlayersDAO = blockedPlayersDAO;
        this.lastSeenDAO = lastSeenDAO;
        this.onlinePlayers = ConcurrentHashMap.newKeySet();
        this.requestCooldowns = new ConcurrentHashMap<>();
        this.maxFriends = maxFriends;
        this.requestExpiryMs = requestExpiryDays * 24L * 60 * 60 * 1000L;
        this.requestCooldownMs = requestCooldownMinutes * 60L * 1000L;
    }

    public void updateConfig(int maxFriends, int requestExpiryDays, int requestCooldownMinutes) {
        this.maxFriends = maxFriends;
        this.requestExpiryMs = requestExpiryDays * 24L * 60 * 60 * 1000L;
        this.requestCooldownMs = requestCooldownMinutes * 60L * 1000L;
    }

    public long getRequestExpiryMs() {
        return requestExpiryMs;
    }

    public int getMaxFriends() {
        return maxFriends;
    }

    @Nonnull
    public CompletableFuture<FriendsProfile> loadProfile(@Nonnull UUID playerUuid) {
        FriendsProfile cached = cache.get(playerUuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // Load from database async
        return CompletableFuture.supplyAsync(() -> {
            // Load settings
            FriendSettings settings = settingsDAO.loadSettings(playerUuid);

            // Create profile
            FriendsProfile profile = new FriendsProfile(playerUuid, settings);

            // Load friends
            List<FriendData> friends = friendsDAO.getFriends(playerUuid);
            for (FriendData friend : friends) {
                profile.addFriend(friend);
            }

            // Load incoming requests
            List<FriendRequest> incomingRequests = requestsDAO.getIncomingRequests(playerUuid);
            for (FriendRequest request : incomingRequests) {
                if (!request.isExpired(requestExpiryMs)) {
                    profile.addIncomingRequest(request);
                }
            }

            // Load outgoing requests
            List<FriendRequest> outgoingRequests = requestsDAO.getOutgoingRequests(playerUuid);
            for (FriendRequest request : outgoingRequests) {
                if (!request.isExpired(requestExpiryMs)) {
                    profile.addOutgoingRequest(request);
                }
            }

            // Load blocked players
            List<BlockedPlayer> blockedPlayers = blockedPlayersDAO.getBlockedPlayers(playerUuid);
            for (BlockedPlayer blocked : blockedPlayers) {
                profile.addBlocked(blocked.getBlockedUuid());
            }

            // Add to cache
            cache.put(playerUuid, profile);

            return profile;
        });
    }

    public void unloadProfile(@Nonnull UUID playerUuid) {
        cache.remove(playerUuid);
    }

    @Nullable
    public FriendsProfile getProfile(@Nonnull UUID playerUuid) {
        return cache.get(playerUuid);
    }

    public void markOnline(@Nonnull UUID playerUuid) {
        onlinePlayers.add(playerUuid);
    }

    public void markOffline(@Nonnull UUID playerUuid) {
        onlinePlayers.remove(playerUuid);
    }

    public boolean isOnline(@Nonnull UUID playerUuid) {
        return onlinePlayers.contains(playerUuid);
    }

    @Nonnull
    public Set<UUID> getOnlinePlayers() {
        return new HashSet<>(onlinePlayers);
    }

    @Nonnull
    public List<UUID> getOnlineFriends(@Nonnull UUID playerUuid) {
        FriendsProfile profile = cache.get(playerUuid);
        if (profile == null) {
            return Collections.emptyList();
        }

        return profile.getFriendsList().stream()
                .map(FriendData::getFriendUuid)
                .filter(onlinePlayers::contains)
                .collect(Collectors.toList());
    }

    public int getOnlineFriendsCount(@Nonnull UUID playerUuid) {
        return getOnlineFriends(playerUuid).size();
    }

    //  Friend Request Operations 

    @Nonnull
    public FriendRequestResult sendFriendRequest(@Nonnull UUID sender, @Nonnull UUID receiver) {
        if (sender.equals(receiver)) {
            return FriendRequestResult.CANNOT_ADD_SELF;
        }

        FriendsProfile senderProfile = cache.get(sender);
        if (senderProfile == null) {
            return FriendRequestResult.SENDER_PROFILE_NOT_LOADED;
        }

        // Check if already friends
        if (senderProfile.isFriend(receiver)) {
            return FriendRequestResult.ALREADY_FRIENDS;
        }

        // Check if sender blocked receiver
        if (senderProfile.isBlocked(receiver)) {
            return FriendRequestResult.PLAYER_BLOCKED;
        }

        // Check if receiver blocked sender
        FriendsProfile receiverProfile = cache.get(receiver);
        if (receiverProfile != null && receiverProfile.isBlocked(sender)) {
            return FriendRequestResult.BLOCKED_BY_PLAYER;
        }

        // Check max friends limit for sender
        if (senderProfile.getFriendCount() >= maxFriends) {
            return FriendRequestResult.SENDER_FRIENDS_LIST_FULL;
        }

        // Check cooldown
        String cooldownKey = sender + ":" + receiver;
        Long cooldownExpiry = requestCooldowns.get(cooldownKey);
        if (cooldownExpiry != null && System.currentTimeMillis() < cooldownExpiry) {
            return FriendRequestResult.REQUEST_ON_COOLDOWN;
        }

        // Check if there's already an outgoing request
        if (senderProfile.hasOutgoingRequest(receiver)) {
            return FriendRequestResult.REQUEST_ALREADY_SENT;
        }

        // Check if there's an incoming request from the receiver
        if (senderProfile.hasIncomingRequest(receiver)) {
            // Auto-accept the request
            acceptFriendRequest(sender, receiver);
            return FriendRequestResult.AUTO_ACCEPTED;
        }

        // Check receiver's settings (if loaded)
        if (receiverProfile != null && !receiverProfile.getSettings().isAllowRequests()) {
            return FriendRequestResult.RECEIVER_NOT_ACCEPTING_REQUESTS;
        }

        // Create the request
        long timestamp = System.currentTimeMillis();
        FriendRequest request = new FriendRequest(sender, receiver, timestamp);

        // Save to database
        requestsDAO.createRequest(sender, receiver, timestamp);

        // Update caches
        senderProfile.addOutgoingRequest(request);
        if (receiverProfile != null) {
            receiverProfile.addIncomingRequest(request);
        }

        return FriendRequestResult.SUCCESS;
    }

    @Nonnull
    public FriendRequestResult acceptFriendRequest(@Nonnull UUID receiver, @Nonnull UUID sender) {
        FriendsProfile receiverProfile = cache.get(receiver);
        if (receiverProfile == null) {
            return FriendRequestResult.RECEIVER_PROFILE_NOT_LOADED;
        }

        // Check if request exists
        if (!receiverProfile.hasIncomingRequest(sender)) {
            return FriendRequestResult.REQUEST_NOT_FOUND;
        }

        // Check max friends limit for both
        if (receiverProfile.getFriendCount() >= maxFriends) {
            return FriendRequestResult.RECEIVER_FRIENDS_LIST_FULL;
        }

        FriendsProfile senderProfile = cache.get(sender);
        if (senderProfile != null && senderProfile.getFriendCount() >= maxFriends) {
            return FriendRequestResult.SENDER_FRIENDS_LIST_FULL;
        }

        // Add friend relationship
        long timestamp = System.currentTimeMillis();
        friendsDAO.addFriend(receiver, sender, timestamp);

        // Update caches
        FriendData receiverFriend = new FriendData(sender, timestamp);
        FriendData senderFriend = new FriendData(receiver, timestamp);

        receiverProfile.addFriend(receiverFriend);
        receiverProfile.removeIncomingRequest(sender);

        if (senderProfile != null) {
            senderProfile.addFriend(senderFriend);
            senderProfile.removeOutgoingRequest(receiver);
        }

        // Delete request from database
        requestsDAO.deleteRequest(sender, receiver);

        return FriendRequestResult.SUCCESS;
    }

    @Nonnull
    public FriendRequestResult denyFriendRequest(@Nonnull UUID receiver, @Nonnull UUID sender) {
        FriendsProfile receiverProfile = cache.get(receiver);
        if (receiverProfile == null) {
            return FriendRequestResult.RECEIVER_PROFILE_NOT_LOADED;
        }

        // Check if request exists
        if (!receiverProfile.hasIncomingRequest(sender)) {
            return FriendRequestResult.REQUEST_NOT_FOUND;
        }

        // Delete request
        requestsDAO.deleteRequest(sender, receiver);

        // Update caches
        receiverProfile.removeIncomingRequest(sender);

        FriendsProfile senderProfile = cache.get(sender);
        if (senderProfile != null) {
            senderProfile.removeOutgoingRequest(receiver);
        }

        // Set cooldown for this sender->receiver pair
        requestCooldowns.put(sender + ":" + receiver, System.currentTimeMillis() + requestCooldownMs);

        return FriendRequestResult.SUCCESS;
    }

    @Nonnull
    public FriendRequestResult cancelFriendRequest(@Nonnull UUID sender, @Nonnull UUID receiver) {
        FriendsProfile senderProfile = cache.get(sender);
        if (senderProfile == null) {
            return FriendRequestResult.SENDER_PROFILE_NOT_LOADED;
        }

        // Check if request exists
        if (!senderProfile.hasOutgoingRequest(receiver)) {
            return FriendRequestResult.REQUEST_NOT_FOUND;
        }

        // Delete request
        requestsDAO.deleteRequest(sender, receiver);

        // Update caches
        senderProfile.removeOutgoingRequest(receiver);

        FriendsProfile receiverProfile = cache.get(receiver);
        if (receiverProfile != null) {
            receiverProfile.removeIncomingRequest(sender);
        }

        // Set cooldown
        requestCooldowns.put(sender + ":" + receiver, System.currentTimeMillis() + requestCooldownMs);

        return FriendRequestResult.SUCCESS;
    }

    @Nonnull
    public FriendRequestResult removeFriend(@Nonnull UUID player, @Nonnull UUID friend) {
        FriendsProfile playerProfile = cache.get(player);
        if (playerProfile == null) {
            return FriendRequestResult.SENDER_PROFILE_NOT_LOADED;
        }

        // Check if they are friends
        if (!playerProfile.isFriend(friend)) {
            return FriendRequestResult.NOT_FRIENDS;
        }

        // Remove from database
        friendsDAO.removeFriend(player, friend);

        // Update caches
        playerProfile.removeFriend(friend);

        FriendsProfile friendProfile = cache.get(friend);
        if (friendProfile != null) {
            friendProfile.removeFriend(player);
        }

        return FriendRequestResult.SUCCESS;
    }

    //  Block Operations 

    public void blockPlayer(@Nonnull UUID player, @Nonnull UUID target) {
        long timestamp = System.currentTimeMillis();

        // Save to database
        blockedPlayersDAO.blockPlayer(player, target, timestamp);

        // Update cache
        FriendsProfile profile = cache.get(player);
        if (profile != null) {
            profile.addBlocked(target);

            // Auto-unfriend if friends
            if (profile.isFriend(target)) {
                removeFriend(player, target);
            }

            // Remove any pending requests between these players
            if (profile.hasIncomingRequest(target)) {
                requestsDAO.deleteRequest(target, player);
                profile.removeIncomingRequest(target);
            }
            if (profile.hasOutgoingRequest(target)) {
                requestsDAO.deleteRequest(player, target);
                profile.removeOutgoingRequest(target);
            }
        }

        // Also clean up the target's cache
        FriendsProfile targetProfile = cache.get(target);
        if (targetProfile != null) {
            targetProfile.removeIncomingRequest(player);
            targetProfile.removeOutgoingRequest(player);
        }
    }

    public void unblockPlayer(@Nonnull UUID player, @Nonnull UUID target) {
        blockedPlayersDAO.unblockPlayer(player, target);

        FriendsProfile profile = cache.get(player);
        if (profile != null) {
            profile.removeBlocked(target);
        }
    }

    public boolean isBlocked(@Nonnull UUID player, @Nonnull UUID target) {
        FriendsProfile profile = cache.get(player);
        if (profile != null) {
            return profile.isBlocked(target);
        }
        return blockedPlayersDAO.isBlocked(player, target);
    }

    @Nonnull
    public Set<UUID> getBlockedPlayers(@Nonnull UUID player) {
        FriendsProfile profile = cache.get(player);
        if (profile != null) {
            return profile.getBlockedPlayers();
        }
        return blockedPlayersDAO.getBlockedPlayers(player).stream()
                .map(BlockedPlayer::getBlockedUuid)
                .collect(Collectors.toSet());
    }

    //  Settings Operations
    public void toggleNotifications(@Nonnull UUID playerUuid) {
        FriendsProfile profile = cache.get(playerUuid);
        if (profile == null) {
            return;
        }

        FriendSettings settings = profile.getSettings();
        boolean newValue = !settings.isNotificationsEnabled();
        settings.setNotificationsEnabled(newValue);

        // Save to database
        settingsDAO.setNotificationsEnabled(playerUuid, newValue);
    }

    public void toggleAllowRequests(@Nonnull UUID playerUuid) {
        FriendsProfile profile = cache.get(playerUuid);
        if (profile == null) {
            return;
        }

        FriendSettings settings = profile.getSettings();
        boolean newValue = !settings.isAllowRequests();
        settings.setAllowRequests(newValue);

        // Save to database
        settingsDAO.setAllowRequests(playerUuid, newValue);
    }

    //  Last Seen 

    public long getLastSeen(@Nonnull UUID playerUuid) {
        return lastSeenDAO.getLastSeen(playerUuid);
    }

    //  Mutual Friends 

    @Nonnull
    public List<UUID> getMutualFriends(@Nonnull UUID player1, @Nonnull UUID player2) {
        Set<UUID> friends1;
        Set<UUID> friends2;

        FriendsProfile profile1 = cache.get(player1);
        if (profile1 != null) {
            friends1 = profile1.getFriends().keySet();
        } else {
            friends1 = friendsDAO.getFriends(player1).stream()
                    .map(FriendData::getFriendUuid)
                    .collect(Collectors.toSet());
        }

        FriendsProfile profile2 = cache.get(player2);
        if (profile2 != null) {
            friends2 = profile2.getFriends().keySet();
        } else {
            friends2 = friendsDAO.getFriends(player2).stream()
                    .map(FriendData::getFriendUuid)
                    .collect(Collectors.toSet());
        }

        // Intersect
        Set<UUID> mutual = new HashSet<>(friends1);
        mutual.retainAll(friends2);
        return new ArrayList<>(mutual);
    }

    //  Force Operations
    public void forceAddFriend(@Nonnull UUID player1, @Nonnull UUID player2) {
        long timestamp = System.currentTimeMillis();
        friendsDAO.addFriend(player1, player2, timestamp);

        FriendData friend1 = new FriendData(player2, timestamp);
        FriendData friend2 = new FriendData(player1, timestamp);

        FriendsProfile profile1 = cache.get(player1);
        if (profile1 != null) {
            profile1.addFriend(friend1);
        }

        FriendsProfile profile2 = cache.get(player2);
        if (profile2 != null) {
            profile2.addFriend(friend2);
        }
    }

    public void forceRemoveFriend(@Nonnull UUID player1, @Nonnull UUID player2) {
        friendsDAO.removeFriend(player1, player2);

        FriendsProfile profile1 = cache.get(player1);
        if (profile1 != null) {
            profile1.removeFriend(player2);
        }

        FriendsProfile profile2 = cache.get(player2);
        if (profile2 != null) {
            profile2.removeFriend(player1);
        }
    }

    public void clearAllRequests(@Nonnull UUID playerUuid) {
        requestsDAO.deleteAllRequestsForPlayer(playerUuid);

        FriendsProfile profile = cache.get(playerUuid);
        if (profile != null) {
            // Clear all incoming/outgoing from other profiles' caches
            for (FriendRequest req : profile.getIncomingRequests()) {
                FriendsProfile senderProfile = cache.get(req.getSenderUuid());
                if (senderProfile != null) {
                    senderProfile.removeOutgoingRequest(playerUuid);
                }
            }
            for (FriendRequest req : profile.getOutgoingRequests()) {
                FriendsProfile receiverProfile = cache.get(req.getReceiverUuid());
                if (receiverProfile != null) {
                    receiverProfile.removeIncomingRequest(playerUuid);
                }
            }

            // Clear this player's requests by reloading
            // We can't clear synchronized lists directly, so remove each
            List<UUID> incomingSenders = profile.getIncomingRequests().stream()
                    .map(FriendRequest::getSenderUuid).toList();
            for (UUID sender : incomingSenders) {
                profile.removeIncomingRequest(sender);
            }
            List<UUID> outgoingReceivers = profile.getOutgoingRequests().stream()
                    .map(FriendRequest::getReceiverUuid).toList();
            for (UUID receiver : outgoingReceivers) {
                profile.removeOutgoingRequest(receiver);
            }
        }
    }

    // 

    public int cleanupExpiredRequests() {
        return requestsDAO.deleteOldRequests(requestExpiryMs);
    }

    //  Result Enum

    public enum FriendRequestResult {
        SUCCESS,
        AUTO_ACCEPTED,
        ALREADY_FRIENDS,
        NOT_FRIENDS,
        REQUEST_ALREADY_SENT,
        REQUEST_NOT_FOUND,
        CANNOT_ADD_SELF,
        SENDER_PROFILE_NOT_LOADED,
        RECEIVER_PROFILE_NOT_LOADED,
        RECEIVER_NOT_ACCEPTING_REQUESTS,
        PLAYER_BLOCKED,
        BLOCKED_BY_PLAYER,
        SENDER_FRIENDS_LIST_FULL,
        RECEIVER_FRIENDS_LIST_FULL,
        REQUEST_ON_COOLDOWN
    }
}
