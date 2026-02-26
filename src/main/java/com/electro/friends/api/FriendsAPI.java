package com.electro.friends.api;

import com.electro.friends.manager.FriendsManager;
import com.electro.friends.model.FriendData;
import com.electro.friends.model.FriendRequest;
import com.electro.friends.model.FriendSettings;
import com.electro.friends.model.FriendsProfile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class FriendsAPI {
    private final FriendsManager friendsManager;

    public FriendsAPI(@Nonnull FriendsManager friendsManager) {
        this.friendsManager = friendsManager;
    }

    @Nullable
    public FriendsProfile getProfile(@Nonnull UUID playerUuid) {
        return friendsManager.getProfile(playerUuid);
    }

    public boolean areFriends(@Nonnull UUID player1, @Nonnull UUID player2) {
        FriendsProfile profile = friendsManager.getProfile(player1);
        return profile != null && profile.isFriend(player2);
    }

    @Nonnull
    public List<FriendData> getFriends(@Nonnull UUID playerUuid) {
        FriendsProfile profile = friendsManager.getProfile(playerUuid);
        if (profile == null) {
            return List.of();
        }
        return profile.getFriendsList();
    }

    public int getFriendCount(@Nonnull UUID playerUuid) {
        FriendsProfile profile = friendsManager.getProfile(playerUuid);
        return profile != null ? profile.getFriendCount() : 0;
    }

    @Nonnull
    public List<UUID> getOnlineFriends(@Nonnull UUID playerUuid) {
        return friendsManager.getOnlineFriends(playerUuid);
    }

    public int getOnlineFriendsCount(@Nonnull UUID playerUuid) {
        return friendsManager.getOnlineFriendsCount(playerUuid);
    }

    public boolean isOnline(@Nonnull UUID playerUuid) {
        return friendsManager.isOnline(playerUuid);
    }

    @Nonnull
    public Set<UUID> getOnlinePlayers() {
        return friendsManager.getOnlinePlayers();
    }

    @Nonnull
    public List<FriendRequest> getIncomingRequests(@Nonnull UUID playerUuid) {
        FriendsProfile profile = friendsManager.getProfile(playerUuid);
        if (profile == null) {
            return List.of();
        }
        return profile.getIncomingRequests();
    }

    @Nonnull
    public List<FriendRequest> getOutgoingRequests(@Nonnull UUID playerUuid) {
        FriendsProfile profile = friendsManager.getProfile(playerUuid);
        if (profile == null) {
            return List.of();
        }
        return profile.getOutgoingRequests();
    }

    @Nullable
    public FriendSettings getSettings(@Nonnull UUID playerUuid) {
        FriendsProfile profile = friendsManager.getProfile(playerUuid);
        return profile != null ? profile.getSettings() : null;
    }

    @Nonnull
    public FriendsManager.FriendRequestResult sendFriendRequest(@Nonnull UUID sender, @Nonnull UUID receiver) {
        return friendsManager.sendFriendRequest(sender, receiver);
    }

    @Nonnull
    public FriendsManager.FriendRequestResult acceptFriendRequest(@Nonnull UUID receiver, @Nonnull UUID sender) {
        return friendsManager.acceptFriendRequest(receiver, sender);
    }

    @Nonnull
    public FriendsManager.FriendRequestResult denyFriendRequest(@Nonnull UUID receiver, @Nonnull UUID sender) {
        return friendsManager.denyFriendRequest(receiver, sender);
    }

    @Nonnull
    public FriendsManager.FriendRequestResult cancelFriendRequest(@Nonnull UUID sender, @Nonnull UUID receiver) {
        return friendsManager.cancelFriendRequest(sender, receiver);
    }

    @Nonnull
    public FriendsManager.FriendRequestResult removeFriend(@Nonnull UUID player, @Nonnull UUID friend) {
        return friendsManager.removeFriend(player, friend);
    }

    public void toggleNotifications(@Nonnull UUID playerUuid) {
        friendsManager.toggleNotifications(playerUuid);
    }

    public void toggleAllowRequests(@Nonnull UUID playerUuid) {
        friendsManager.toggleAllowRequests(playerUuid);
    }

    // Block list
    public void blockPlayer(@Nonnull UUID player, @Nonnull UUID target) {
        friendsManager.blockPlayer(player, target);
    }

    public void unblockPlayer(@Nonnull UUID player, @Nonnull UUID target) {
        friendsManager.unblockPlayer(player, target);
    }

    public boolean isBlocked(@Nonnull UUID player, @Nonnull UUID target) {
        return friendsManager.isBlocked(player, target);
    }

    @Nonnull
    public Set<UUID> getBlockedPlayers(@Nonnull UUID player) {
        return friendsManager.getBlockedPlayers(player);
    }

    // Last seen
    public long getLastSeen(@Nonnull UUID playerUuid) {
        return friendsManager.getLastSeen(playerUuid);
    }

    // Mutual friends
    @Nonnull
    public List<UUID> getMutualFriends(@Nonnull UUID player1, @Nonnull UUID player2) {
        return friendsManager.getMutualFriends(player1, player2);
    }
}
