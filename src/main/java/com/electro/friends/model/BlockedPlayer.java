package com.electro.friends.model;

import javax.annotation.Nonnull;
import java.util.UUID;


public class BlockedPlayer {
    private final UUID blockedUuid;
    private final long blockedAt;

    public BlockedPlayer(@Nonnull UUID blockedUuid, long blockedAt) {
        this.blockedUuid = blockedUuid;
        this.blockedAt = blockedAt;
    }

    @Nonnull
    public UUID getBlockedUuid() {
        return blockedUuid;
    }

    public long getBlockedAt() {
        return blockedAt;
    }

    @Override
    public String toString() {
        return "BlockedPlayer{" +
                "blockedUuid=" + blockedUuid +
                ", blockedAt=" + blockedAt +
                '}';
    }
}
