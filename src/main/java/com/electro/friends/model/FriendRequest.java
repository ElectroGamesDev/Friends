package com.electro.friends.model;

import javax.annotation.Nonnull;
import java.util.UUID;


public class FriendRequest {
    private final UUID senderUuid;
    private final UUID receiverUuid;
    private final long sentAt;

    public FriendRequest(@Nonnull UUID senderUuid, @Nonnull UUID receiverUuid, long sentAt) {
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
        this.sentAt = sentAt;
    }

    @Nonnull
    public UUID getSenderUuid() {
        return senderUuid;
    }

    @Nonnull
    public UUID getReceiverUuid() {
        return receiverUuid;
    }

    public long getSentAt() {
        return sentAt;
    }

    public boolean isExpired(long expiryMs) {
        return (System.currentTimeMillis() - sentAt) > expiryMs;
    }

    @Override
    public String toString() {
        return "FriendRequest{" +
                "senderUuid=" + senderUuid +
                ", receiverUuid=" + receiverUuid +
                ", sentAt=" + sentAt +
                '}';
    }
}
