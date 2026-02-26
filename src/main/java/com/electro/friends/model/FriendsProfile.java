package com.electro.friends.model;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class FriendsProfile {
    private final UUID playerUuid;
    private final Map<UUID, FriendData> friends;
    private final List<FriendRequest> incomingRequests;
    private final List<FriendRequest> outgoingRequests;
    private final Set<UUID> blockedPlayers;
    private FriendSettings settings;

    public FriendsProfile(@Nonnull UUID playerUuid, @Nonnull FriendSettings settings) {
        this.playerUuid = playerUuid;
        this.friends = new ConcurrentHashMap<>();
        this.incomingRequests = Collections.synchronizedList(new ArrayList<>());
        this.outgoingRequests = Collections.synchronizedList(new ArrayList<>());
        this.blockedPlayers = ConcurrentHashMap.newKeySet();
        this.settings = settings;
    }

    @Nonnull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    @Nonnull
    public Map<UUID, FriendData> getFriends() {
        return friends;
    }

    @Nonnull
    public List<FriendData> getFriendsList() {
        return new ArrayList<>(friends.values());
    }

    public void addFriend(@Nonnull FriendData friend) {
        friends.put(friend.getFriendUuid(), friend);
    }

    public void removeFriend(@Nonnull UUID friendUuid) {
        friends.remove(friendUuid);
    }

    public boolean isFriend(@Nonnull UUID uuid) {
        return friends.containsKey(uuid);
    }

    public int getFriendCount() {
        return friends.size();
    }

    @Nonnull
    public List<FriendRequest> getIncomingRequests() {
        return new ArrayList<>(incomingRequests);
    }

    public void addIncomingRequest(@Nonnull FriendRequest request) {
        incomingRequests.add(request);
    }

    public void removeIncomingRequest(@Nonnull UUID senderUuid) {
        incomingRequests.removeIf(req -> req.getSenderUuid().equals(senderUuid));
    }

    @Nonnull
    public List<FriendRequest> getOutgoingRequests() {
        return new ArrayList<>(outgoingRequests);
    }

    public void addOutgoingRequest(@Nonnull FriendRequest request) {
        outgoingRequests.add(request);
    }

    public void removeOutgoingRequest(@Nonnull UUID receiverUuid) {
        outgoingRequests.removeIf(req -> req.getReceiverUuid().equals(receiverUuid));
    }

    public boolean hasIncomingRequest(@Nonnull UUID senderUuid) {
        return incomingRequests.stream().anyMatch(req -> req.getSenderUuid().equals(senderUuid));
    }

    public boolean hasOutgoingRequest(@Nonnull UUID receiverUuid) {
        return outgoingRequests.stream().anyMatch(req -> req.getReceiverUuid().equals(receiverUuid));
    }

    // Blocked players methods
    public void addBlocked(@Nonnull UUID uuid) {
        blockedPlayers.add(uuid);
    }

    public void removeBlocked(@Nonnull UUID uuid) {
        blockedPlayers.remove(uuid);
    }

    public boolean isBlocked(@Nonnull UUID uuid) {
        return blockedPlayers.contains(uuid);
    }

    @Nonnull
    public Set<UUID> getBlockedPlayers() {
        return new HashSet<>(blockedPlayers);
    }

    @Nonnull
    public FriendSettings getSettings() {
        return settings;
    }

    public void setSettings(@Nonnull FriendSettings settings) {
        this.settings = settings;
    }

    @Override
    public String toString() {
        return "FriendsProfile{" +
                "playerUuid=" + playerUuid +
                ", friendCount=" + friends.size() +
                ", incomingRequests=" + incomingRequests.size() +
                ", outgoingRequests=" + outgoingRequests.size() +
                ", blockedPlayers=" + blockedPlayers.size() +
                '}';
    }
}
