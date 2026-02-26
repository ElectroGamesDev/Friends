package com.electro.friends.util;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class Lang {
    private final ConfigManager config;

    public Lang(@Nonnull Path dataFolder) {
        this.config = new ConfigManager(dataFolder, "lang.json");
        initializeDefaults();
    }

    @Nonnull
    public String get(@Nonnull String key) {
        String value = config.getString(key);
        return value != null ? value : key;
    }

    @Nonnull
    public String format(@Nonnull String key, @Nonnull String... replacements) {
        String text = get(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return text;
    }

    public void reload() {
        config.reload();
        initializeDefaults();
    }

    private void initializeDefaults() {
        if (config.get("prefix") instanceof String) {
            return;
        }

        config.beginBatch();

        // Generic
        config.set("prefix", "[Friends]");

        // Connection notifications
        config.set("notify.join", "{player} joined the server!");
        config.set("notify.leave", "{player} left the server.");
        config.set("notify.friends-online", "You have {online} of {total} friends online.");

        // Error messages
        config.set("error.profile-not-loaded", "Your friends profile is not loaded.");
        config.set("error.loading-gui", "Error loading friends GUI!");
        config.set("error.loading-profile", "Error loading friends profile!");
        config.set("error.loading-friend-list", "Error loading friends list.");
        config.set("error.loading-requests", "Error loading friend requests.");
        config.set("error.loading-removal", "Error processing friend removal.");
        config.set("error.loading-deny", "Error processing request denial.");
        config.set("error.loading-cancel", "Error processing request cancellation.");
        config.set("error.loading-unblock", "Error processing unblock.");
        config.set("error.loading-request", "Error processing friend request.");
        config.set("error.unknown-setting", "Unknown setting: {setting}");

        // Friend request - send
        config.set("request.sent", "Friend request sent to ");
        config.set("request.sent-suffix", "!");
        config.set("request.received", " sent you a friend request! Use ");
        config.set("request.received-cmd", "/friends accept {player}");
        config.set("request.received-suffix", " to accept.");
        config.set("request.now-friends", "You are now friends with ");
        config.set("request.now-friends-suffix", "!");
        config.set("request.accepted-by.suffix", " accepted your friend request!");
        config.set("request.already-friends", "You are already friends with {player}!");
        config.set("request.already-sent", "You already sent a friend request to {player}!");
        config.set("request.cannot-self", "You cannot send a friend request to yourself!");
        config.set("request.not-accepting", "{player} is not accepting friend requests.");
        config.set("request.you-blocked", "You have blocked {player}. Unblock them first to send a request.");
        config.set("request.sender-full", "Your friends list is full!");
        config.set("request.receiver-full", "{player}'s friends list is full.");
        config.set("request.cooldown", "Please wait before sending another request to {player}.");
        config.set("request.failed", "Failed to send friend request: {result}");
        config.set("request.no-request-from", "No friend request from: {player}");
        config.set("request.accept-failed", "Failed to accept friend request: {result}");
        config.set("request.no-outgoing", "No outgoing friend request to: {player}");
        config.set("request.deny-failed", "Failed to deny friend request: {result}");
        config.set("request.denied", "Denied friend request from ");
        config.set("request.denied-suffix", ".");
        config.set("request.cancel-failed", "Failed to cancel friend request: {result}");
        config.set("request.cancelled", "Cancelled friend request to ");
        config.set("request.cancelled-suffix", ".");

        // Unfriend
        config.set("unfriend.not-friends", "You are not friends with: {player}");
        config.set("unfriend.failed", "Failed to remove friend: {result}");
        config.set("unfriend.removed", "Removed ");
        config.set("unfriend.removed-suffix", " from your friends list.");

        // Block / Unblock
        config.set("block.cannot-self", "You cannot block yourself!");
        config.set("block.already-blocked", "You have already blocked {player}!");
        config.set("block.not-found", "Player not found: {player}");
        config.set("block.blocked", "Blocked ");
        config.set("block.blocked-suffix", ". They can no longer send you friend requests.");
        config.set("unblock.no-blocked", "You have no blocked players.");
        config.set("unblock.not-in-list", "Player not found in your block list: {player}");
        config.set("unblock.unblocked", "Unblocked ");
        config.set("unblock.unblocked-suffix", ".");

        // Settings command
        config.set("settings.notifications", "Friend notifications: ");
        config.set("settings.requests-toggle", "Accept friend requests: ");
        config.set("settings.enabled", "ENABLED");
        config.set("settings.disabled", "DISABLED");
        config.set("settings.notifications-title", "Notifications: ");
        config.set("settings.accept-title", "Accept Requests: ");
        config.set("settings.toggle-notifs-cmd", "/friends settings notifications");
        config.set("settings.toggle-requests-cmd", "/friends settings requests");

        // Usage messages
        config.set("usage.add", "Usage: /friends add <player>");
        config.set("usage.accept", "Usage: /friends accept <player>");
        config.set("usage.deny", "Usage: /friends deny <player>");
        config.set("usage.cancel", "Usage: /friends cancel <player>");
        config.set("usage.unfriend", "Usage: /friends unfriend <player>");
        config.set("usage.block", "Usage: /friends block <player>");
        config.set("usage.unblock", "Usage: /friends unblock <player>");

        // Friends list command
        config.set("list.no-friends", "You have no friends yet. Use ");
        config.set("list.no-friends-cmd", "/friends friend <player>");
        config.set("list.no-friends-suffix", " to add friends!");
        config.set("list.online-header", "\nOnline ({count}):\n");
        config.set("list.offline-header", "\nOffline ({count}):\n");
        config.set("list.friends-since", " (friends since {time})\n");
        config.set("list.error", "Error loading friends list.");

        // Friend requests command
        config.set("requests.none", "You have no pending friend requests.");
        config.set("requests.incoming-header", "\nIncoming Requests ({count}):\n");
        config.set("requests.outgoing-header", "\nOutgoing Requests ({count}):\n");
        config.set("requests.sent-time", " (sent {time})\n");
        config.set("requests.error", "Error loading friend requests.");

        // UI (add friend section)
        config.set("ui.please-enter-username", "Please enter a username!");
        config.set("ui.player-not-found", "Player not found: {player}");
        config.set("ui.add-friend-placeholder", "Enter username");
        config.set("ui.add-friend-btn", "Add Friend");
        config.set("ui.no-friends", "You haven't added any friends yet!\n\nEnter a username above to send a request.");

        // UI (friend list entries)
        config.set("ui.status.online", "Online");
        config.set("ui.status.last-seen", "Last seen {time}");
        config.set("ui.status.offline", "Offline");
        config.set("ui.friends-since", "Friends since {time}");
        config.set("ui.remove-btn", "Remove");
        config.set("ui.removed-from-friends", "Removed {player} from your friends list");

        // UI (requests tab)
        config.set("ui.no-requests", "No pending friend requests");
        config.set("ui.expires-in", "Expires in {time}");
        config.set("ui.expired", "Expired");
        config.set("ui.accept-btn", "Accept");
        config.set("ui.deny-btn", "Deny");
        config.set("ui.now-friends", "You are now friends with {player}");
        config.set("ui.denied-request", "Denied friend request from {player}");

        // UI (pending/sent tab)
        config.set("ui.sent-to", "Sent to {player}");
        config.set("ui.no-pending", "No outgoing requests");
        config.set("ui.cancel-btn", "Cancel");
        config.set("ui.cancelled-request", "Cancelled friend request to {player}");

        // UI (settings tab)
        config.set("ui.setting.notifications", "Friend Notifications");
        config.set("ui.setting.requests", "Allow Friend Requests");
        config.set("ui.notifications-toggled", "Notifications {state}");
        config.set("ui.requests-toggled", "Friend requests {state}");
        config.set("ui.state.enabled", "enabled");
        config.set("ui.state.disabled", "disabled");

        // UI (header / tabs)
        config.set("ui.stat.friends", "Friends");
        config.set("ui.stat.online", "Online");
        config.set("ui.stat.requests", "Requests");
        config.set("ui.tab.friends", "Friends");
        config.set("ui.tab.requests", "Requests ({count})");
        config.set("ui.tab.pending", "Sent ({count})");
        config.set("ui.tab.settings", "Settings");
        config.set("ui.title", "Friends");

        config.endBatch();
    }
}
