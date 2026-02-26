package com.electro.friends.model;

import javax.annotation.Nonnull;
import java.util.UUID;


public class FriendData {
    private final UUID friendUuid;
    private final long since;

    public FriendData(@Nonnull UUID friendUuid, long since) {
        this.friendUuid = friendUuid;
        this.since = since;
    }

    @Nonnull
    public UUID getFriendUuid() {
        return friendUuid;
    }

    public long getSince() {
        return since;
    }

    @Override
    public String toString() {
        return "FriendData{" +
                "friendUuid=" + friendUuid +
                ", since=" + since +
                '}';
    }
}
