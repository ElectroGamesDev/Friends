package com.electro.friends.model;

import javax.annotation.Nonnull;
import java.util.UUID;


public class FriendSettings {
    private final UUID playerUuid;
    private boolean notificationsEnabled;
    private boolean allowRequests;

    public FriendSettings(@Nonnull UUID playerUuid, boolean notificationsEnabled, boolean allowRequests) {
        this.playerUuid = playerUuid;
        this.notificationsEnabled = notificationsEnabled;
        this.allowRequests = allowRequests;
    }

    @Nonnull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public boolean isAllowRequests() {
        return allowRequests;
    }

    public void setAllowRequests(boolean allowRequests) {
        this.allowRequests = allowRequests;
    }

    @Override
    public String toString() {
        return "FriendSettings{" +
                "playerUuid=" + playerUuid +
                ", notificationsEnabled=" + notificationsEnabled +
                ", allowRequests=" + allowRequests +
                '}';
    }
}
