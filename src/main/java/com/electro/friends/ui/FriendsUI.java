package com.electro.friends.ui;

import au.ellie.hyui.builders.PageBuilder;
import com.electro.friends.FriendsPlugin;
import com.electro.friends.manager.FriendsManager;
import com.electro.friends.model.FriendData;
import com.electro.friends.model.FriendRequest;
import com.electro.friends.model.FriendSettings;
import com.electro.friends.model.FriendsProfile;
import com.electro.friends.util.Lang;
import com.electro.friends.util.MessageUtil;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.hypixel.hytale.server.core.NameMatching.EXACT_IGNORE_CASE;

public class FriendsUI {
    final private FriendsPlugin plugin;
    private final long requestExpiryMs;

    private record FriendDisplay(FriendData friend, String username, boolean isOnline, long lastSeen) {}
    private record RequestDisplay(FriendRequest request, String username) {}

    public enum Tab {
        FRIENDS, REQUESTS, PENDING, SETTINGS
    }

    public FriendsUI(FriendsPlugin plugin, int requestExpiryDays) {
        this.plugin = plugin;
        this.requestExpiryMs = requestExpiryDays * 24L * 60 * 60 * 1000L;
    }

    public void openFriendsGUI(PlayerRef playerRef, Store<EntityStore> store, UUID playerUuid, FriendsUI.Tab currentTab) {
        FriendsProfile profile = plugin.getAPI().getProfile(playerUuid);
        if (profile == null) {
            playerRef.sendMessage(Message.raw(plugin.getLang().get("error.loading-profile")).color(Color.RED));
            return;
        }

        List<FriendData> friends = profile.getFriendsList();
        List<FriendRequest> incoming = profile.getIncomingRequests();
        List<FriendRequest> outgoing = profile.getOutgoingRequests();
        int onlineCount = plugin.getAPI().getOnlineFriendsCount(playerUuid);

        // Build GUI asynchronously after fetching all usernames
        buildGUIHtmlAsync(friends, incoming, outgoing, onlineCount, profile.getSettings(), currentTab)
                .thenAccept(html -> {
                    // Schedule the UI opening on the world thread
                    World world = Universe.get().getWorld(playerRef.getWorldUuid());
                    world.execute(() -> {
                        PageBuilder page = PageBuilder.pageForPlayer(playerRef)
                                .withLifetime(CustomPageLifetime.CanDismiss)
                                .fromHtml(html);

                        setupEventListeners(page, playerRef, store, playerUuid, friends, incoming, outgoing, currentTab);

                        page.open(store);
                    });
                })
                .exceptionally(ex -> {
                    playerRef.sendMessage(Message.raw(plugin.getLang().get("error.loading-gui")).color(Color.RED));
                    ex.printStackTrace();
                    return null;
                });
    }

    private CompletableFuture<String> buildGUIHtmlAsync(List<FriendData> friends, List<FriendRequest> incoming,
                                                        List<FriendRequest> outgoing, int onlineCount,
                                                        FriendSettings settings, FriendsUI.Tab currentTab) {
        CompletableFuture<String> contentFuture;

        switch (currentTab) {
            case FRIENDS -> contentFuture = buildFriendsContentAsync(friends);
            case REQUESTS -> contentFuture = buildRequestsContentAsync(incoming);
            case PENDING -> contentFuture = buildPendingContentAsync(outgoing);
            case SETTINGS -> contentFuture = CompletableFuture.completedFuture(buildSettingsContent(settings));
            default -> contentFuture = CompletableFuture.completedFuture("");
        }

        return contentFuture.thenApply(content -> {
            Lang lang = plugin.getLang();
            StringBuilder sb = new StringBuilder();

            sb.append("""
        <style>
            .friends-container {
                anchor-width: 800;
                anchor-height: 520;
            }

            .header-section {
                layout: top;
                flex-weight: 0;
            }

            .stats-row {
                layout: center;
                flex-weight: 0;
            }

            .stat-item {
                layout: top;
                flex-weight: 0;
                text-align: center;
                anchor-width: 120;
            }

            .stat-spacer {
                flex-weight: 0;
                anchor-width: 40;
            }

            .stat-label {
                color: #888888;
                font-size: 11;
            }

            .stat-value-spacer {
                flex-weight: 0;
                anchor-height: 4;
            }

            .stat-value {
                color: #55FF55;
                font-size: 13;
                font-weight: bold;
            }

            .spacer-small {
                flex-weight: 0;
                anchor-height: 8;
            }

            .spacer-medium {
                flex-weight: 0;
                anchor-height: 12;
            }

            .tab-buttons {
                layout: center;
                flex-weight: 0;
            }

            .tab-btn {
                flex-weight: 0;
                anchor-width: 180;
            }

            .tab-spacer {
                flex-weight: 0;
                anchor-width: 8;
            }

            .content-section {
                layout: top;
                flex-weight: 1;
            }

            .add-friend-section {
                layout: left;
                flex-weight: 0;
                background-color: #2a2a2a(0.6);
            }

            .add-friend-input {
                flex-weight: 1;
                anchor-width: 610;
            }

            .add-friend-btn {
                flex-weight: 0;
            }

            .friend-item {
                layout: left;
                flex-weight: 0;
                background-color: #1a1a1a(0.7);
            }

            .friend-info {
                layout: top;
                flex-weight: 1;
            }

            .friend-name {
                font-size: 14;
                font-weight: bold;
            }

            .friend-status {
                color: #888888;
                font-size: 11;
            }

            .online-indicator {
                color: #55FF55;
            }

            .offline-indicator {
                color: #888888;
            }

            .action-btn {
                flex-weight: 0;
            }

            .request-item {
                layout: left;
                flex-weight: 0;
                background-color: #1a1a1a(0.7);
            }

            .request-info {
                layout: top;
                flex-weight: 1;
            }

            .request-name {
                color: #FFFFFF;
                font-size: 14;
                font-weight: bold;
            }

            .request-time {
                color: #888888;
                font-size: 10;
            }

            .settings-row {
                layout: left;
                flex-weight: 0;
                background-color: #1a1a1a(0.5);
            }

            .setting-label {
                flex-weight: 1;
                color: #FFFFFF;
                font-size: 14;
            }

            .empty-state {
                layout: center;
                flex-weight: 1;
            }

            .empty-text {
                color: #666666;
                font-size: 13;
                text-align: center;
            }
            
            .online-indicator {
                color: #55FF55;
            }
        
            .offline-indicator {
                color: #FF5555;
            }
        </style>

        <div class="page-overlay">
            <div class="decorated-container friends-container" data-hyui-title="""
                    + "\"" + lang.get("ui.title") + "\"" + """
>
                <div class="container-contents">
                    <!-- Header Stats -->
                    <div class="header-section">
                        <div class="stats-row">
                            <div class="stat-item">
                                <p class="stat-label">""" + lang.get("ui.stat.friends") + """
</p>
                                <div class="stat-value-spacer"></div>
                                <p class="stat-value">""").append(friends.size()).append("""
            </p>
        </div>
        <div class="stat-spacer"></div>
        <div class="stat-item">
            <p class="stat-label">""" + lang.get("ui.stat.online") + """
</p>
            <div class="stat-value-spacer"></div>
            <p class="stat-value">""").append(onlineCount).append("""
            </p>
        </div>
        <div class="stat-spacer"></div>
        <div class="stat-item">
            <p class="stat-label">""" + lang.get("ui.stat.requests") + """
</p>
            <div class="stat-value-spacer"></div>
            <p class="stat-value">""").append(incoming.size()).append("""
                    </p>
                </div>
            </div>
        </div>

        <div class="spacer-medium"></div>

        <!-- Tab Navigation -->
        <div class="tab-buttons">
            <button id="tab-friends" class="tab-btn""");
            sb.append(currentTab == FriendsUI.Tab.FRIENDS ? " tab-active" : "");
            sb.append("\">").append(lang.get("ui.tab.friends")).append("""
</button>
        <div class="tab-spacer"></div>
        <button id="tab-requests" class="tab-btn""");
            sb.append(currentTab == FriendsUI.Tab.REQUESTS ? " tab-active" : "");
            sb.append("\">").append(lang.format("ui.tab.requests", "count", String.valueOf(incoming.size())));
            sb.append("""
</button>
        <div class="tab-spacer"></div>
        <button id="tab-pending" class="tab-btn""");
            sb.append(currentTab == FriendsUI.Tab.PENDING ? " tab-active" : "");
            sb.append("\">").append(lang.format("ui.tab.pending", "count", String.valueOf(outgoing.size())));
            sb.append("""
</button>
        <div class="tab-spacer"></div>
        <button id="tab-settings" class="tab-btn""");
            sb.append(currentTab == FriendsUI.Tab.SETTINGS ? " tab-active" : "");
            sb.append("\">").append(lang.get("ui.tab.settings")).append("""
</button>
                    </div>

                    <div class="spacer-medium"></div>

                    <!-- Tab Content -->
                    <div class="content-section">
        """);

            sb.append(content);

            sb.append("""
                    </div>
                </div>
            </div>
        </div>
        """);

            return sb.toString();
        });
    }

    private CompletableFuture<String> buildFriendsContentAsync(List<FriendData> friends) {
        Lang lang = plugin.getLang();
        StringBuilder sb = new StringBuilder();

        String placeholder = lang.get("ui.add-friend-placeholder");
        String addBtn = lang.get("ui.add-friend-btn");
        sb.append("<div class=\"add-friend-section\">\n")
          .append("    <input type=\"text\" id=\"add-friend-input\" class=\"add-friend-input\" value=\"\" placeholder=\"")
          .append(placeholder).append("\" data-hyui-tooltiptext=\"").append(placeholder).append("\" />\n")
          .append("    <button id=\"add-friend-btn\" class=\"add-friend-btn\">").append(addBtn).append("</button>\n")
          .append("</div>\n<div class=\"spacer-medium\"></div>\n");

        if (friends.isEmpty()) {
            sb.append("<div class=\"empty-state\">\n    <p class=\"empty-text\">")
              .append(lang.get("ui.no-friends"))
              .append("</p>\n</div>\n");
            return CompletableFuture.completedFuture(sb.toString());
        }

        List<CompletableFuture<FriendDisplay>> friendDisplayFutures = friends.stream()
                .map(friend -> {
                    // Check both the game universe (authoritative) and our online set.
                    // The online set may lag behind during the async profile-load window.
                    boolean isOnline = Universe.get().getPlayer(friend.getFriendUuid()) != null
                            || plugin.getAPI().isOnline(friend.getFriendUuid());
                    CompletableFuture<String> usernameFuture = plugin.getUsernameCache().getUsernameAsync(friend.getFriendUuid());

                    if (isOnline) {
                        return usernameFuture.thenApply(username -> new FriendDisplay(friend, username, true, 0));
                    } else {
                        return usernameFuture.thenApply(username -> {
                            long lastSeen = plugin.getAPI().getLastSeen(friend.getFriendUuid());
                            return new FriendDisplay(friend, username, false, lastSeen);
                        });
                    }
                })
                .collect(Collectors.toList());

        return CompletableFuture.allOf(friendDisplayFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<FriendDisplay> displays = friendDisplayFutures.stream()
                            .map(CompletableFuture::join)
                            .sorted((a, b) -> Boolean.compare(b.isOnline(), a.isOnline()))
                            .collect(Collectors.toList());

                    sb.append("<div style=\"layout-mode: TopScrolling; flex-weight: 1;\" data-hyui-scrollbar-style='\"Common.ui\" \"DefaultScrollbarStyle\"'>");

                    for (int i = 0; i < displays.size(); i++) {
                        FriendDisplay display = displays.get(i);
                        String statusText;
                        String statusClass;
                        if (display.isOnline()) {
                            statusText = lang.get("ui.status.online");
                            statusClass = "online-indicator";
                        } else if (display.lastSeen() > 0) {
                            statusText = lang.format("ui.status.last-seen", "time", MessageUtil.formatRelativeTime(display.lastSeen()));
                            statusClass = "offline-indicator";
                        } else {
                            statusText = lang.get("ui.status.offline");
                            statusClass = "offline-indicator";
                        }
                        String addedDate = lang.format("ui.friends-since", "time", MessageUtil.formatRelativeTime(display.friend().getSince()));

                        String removeId = "remove-" + display.friend().getFriendUuid().toString().replace("-", "");
                        sb.append("""
                        <div class="friend-item">
                            <div class="friend-info">
                                <p class="friend-name">%s</p>
                                <p class="%s" style="font-size: 11;">• %s</p>
                                <p class="friend-status">%s</p>
                            </div>
                            <button id="%s" class="action-btn deny-btn">%s</button>
                        </div>
                        """.formatted(display.username(), statusClass, statusText, addedDate, removeId, lang.get("ui.remove-btn")));

                        if (i < displays.size() - 1) {
                            sb.append("<div class=\"spacer-small\"></div>\n");
                        }
                    }

                    sb.append("</div>");

                    return sb.toString();
                });
    }

    private CompletableFuture<String> buildRequestsContentAsync(List<FriendRequest> incoming) {
        Lang lang = plugin.getLang();
        StringBuilder sb = new StringBuilder();

        if (incoming.isEmpty()) {
            sb.append("<div class=\"empty-state\">\n    <p class=\"empty-text\">")
              .append(lang.get("ui.no-requests"))
              .append("</p>\n</div>\n");
            return CompletableFuture.completedFuture(sb.toString());
        }

        // Fetch all usernames asynchronously
        List<CompletableFuture<RequestDisplay>> requestDisplayFutures = incoming.stream()
                .map(request -> plugin.getUsernameCache().getUsernameAsync(request.getSenderUuid())
                        .thenApply(username -> new RequestDisplay(request, username)))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(requestDisplayFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<RequestDisplay> displays = requestDisplayFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

                    sb.append("<div style=\"layout-mode: TopScrolling;\" data-hyui-scrollbar-style='\"Common.ui\" \"DefaultScrollbarStyle\"'>");

                    for (int i = 0; i < displays.size(); i++) {
                        RequestDisplay display = displays.get(i);
                        String timeAgo = MessageUtil.formatRelativeTime(display.request.getSentAt());
                        String expiresIn = getExpirationTime(display.request.getSentAt());

                        sb.append("""
                        <div class="request-item">
                            <div class="request-info">
                                <p class="request-name">%s</p>
                                <p class="request-time">%s • %s</p>
                            </div>
                            <button id="accept-%d" class="action-btn accept-btn">%s</button>
                            <button id="deny-%d" class="action-btn deny-btn">%s</button>
                        </div>
                        """.formatted(display.username, timeAgo,
                                lang.format("ui.expires-in", "time", expiresIn),
                                i, lang.get("ui.accept-btn"),
                                i, lang.get("ui.deny-btn")));

                        if (i < displays.size() - 1) {
                            sb.append("<div class=\"spacer-small\"></div>\n");
                        }
                    }

                    sb.append("</div>");

                    return sb.toString();
                });
    }

    private CompletableFuture<String> buildPendingContentAsync(List<FriendRequest> outgoing) {
        Lang lang = plugin.getLang();
        StringBuilder sb = new StringBuilder();

        if (outgoing.isEmpty()) {
            sb.append("<div class=\"empty-state\">\n    <p class=\"empty-text\">")
              .append(lang.get("ui.no-pending"))
              .append("</p>\n</div>\n");
            return CompletableFuture.completedFuture(sb.toString());
        }

        // Fetch all usernames asynchronously
        List<CompletableFuture<RequestDisplay>> requestDisplayFutures = outgoing.stream()
                .map(request -> plugin.getUsernameCache().getUsernameAsync(request.getReceiverUuid())
                        .thenApply(username -> new RequestDisplay(request, username)))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(requestDisplayFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<RequestDisplay> displays = requestDisplayFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

                    sb.append("<div style=\"layout-mode: TopScrolling;\" data-hyui-scrollbar-style='\"Common.ui\" \"DefaultScrollbarStyle\"'>");

                    for (int i = 0; i < displays.size(); i++) {
                        RequestDisplay display = displays.get(i);
                        String timeAgo = MessageUtil.formatRelativeTime(display.request.getSentAt());
                        String expiresIn = getExpirationTime(display.request.getSentAt());

                        sb.append("""
                        <div class="request-item">
                            <div class="request-info">
                                <p class="request-name">%s</p>
                                <p class="request-time">%s • %s</p>
                            </div>
                            <button id="cancel-%d" class="action-btn cancel-btn">%s</button>
                        </div>
                        """.formatted(lang.format("ui.sent-to", "player", display.username),
                                timeAgo,
                                lang.format("ui.expires-in", "time", expiresIn),
                                i, lang.get("ui.cancel-btn")));

                        if (i < displays.size() - 1) {
                            sb.append("<div class=\"spacer-small\"></div>\n");
                        }
                    }

                    sb.append("</div>");

                    return sb.toString();
                });
    }

    private String buildSettingsContent(FriendSettings settings) {
        Lang lang = plugin.getLang();
        return "<div class=\"settings-row\">\n"
                + "    <p class=\"setting-label\">" + lang.get("ui.setting.notifications") + "</p>\n"
                + "    <input type=\"checkbox\" id=\"toggle-notifications\" "
                + (settings.isNotificationsEnabled() ? "checked" : "") + " />\n"
                + "</div>\n"
                + "<div class=\"spacer-small\"></div>\n"
                + "<div class=\"settings-row\">\n"
                + "    <p class=\"setting-label\">" + lang.get("ui.setting.requests") + "</p>\n"
                + "    <input type=\"checkbox\" id=\"toggle-requests\" "
                + (settings.isAllowRequests() ? "checked" : "") + " />\n"
                + "</div>\n";
    }

    private void setupEventListeners(PageBuilder page, PlayerRef playerRef, Store<EntityStore> store,
                                     UUID playerUuid, List<FriendData> friends,
                                     List<FriendRequest> incoming, List<FriendRequest> outgoing,
                                     FriendsUI.Tab currentTab) {

        page.addEventListener("tab-friends", CustomUIEventBindingType.Activating,
                event -> openFriendsGUI(playerRef, store, playerUuid, FriendsUI.Tab.FRIENDS));
        page.addEventListener("tab-requests", CustomUIEventBindingType.Activating,
                event -> openFriendsGUI(playerRef, store, playerUuid, FriendsUI.Tab.REQUESTS));
        page.addEventListener("tab-pending", CustomUIEventBindingType.Activating,
                event -> openFriendsGUI(playerRef, store, playerUuid, FriendsUI.Tab.PENDING));
        page.addEventListener("tab-settings", CustomUIEventBindingType.Activating,
                event -> openFriendsGUI(playerRef, store, playerUuid, FriendsUI.Tab.SETTINGS));

        switch (currentTab) {
            case FRIENDS -> setupFriendsListeners(page, playerRef, store, playerUuid, friends);
            case REQUESTS -> setupRequestsListeners(page, playerRef, store, playerUuid, incoming);
            case PENDING -> setupPendingListeners(page, playerRef, store, playerUuid, outgoing);
            case SETTINGS -> setupSettingsListeners(page, playerRef, playerUuid);
        }
    }

    private void setupFriendsListeners(PageBuilder page, PlayerRef playerRef, Store<EntityStore> store,
                                       UUID playerUuid, List<FriendData> friends) {

        // Store the current username input
        final String[] currentUsername = {""};

        // Listen for text field changes
        page.addEventListener("add-friend-input", CustomUIEventBindingType.ValueChanged, (event, ctx) -> {
            java.util.Optional<String> valueOpt = ctx.getValue("add-friend-input", String.class);
            currentUsername[0] = valueOpt.orElse("");
        });

        // Add friend button listener
        page.addEventListener("add-friend-btn", CustomUIEventBindingType.Activating, (event, ctx) -> {
            String username = currentUsername[0].trim();
            if (username.isEmpty()) {
                playerRef.sendMessage(Message.raw(plugin.getLang().get("ui.please-enter-username")).color(Color.RED));
                return;
            }

            // Find target player
            PlayerRef target = Universe.get().getPlayerByUsername(username, EXACT_IGNORE_CASE);
            if (target == null) {
                playerRef.sendMessage(Message.raw(plugin.getLang().format("ui.player-not-found", "player", username)).color(Color.RED));
                return;
            }

            // Send friend request
            FriendsManager.FriendRequestResult result = plugin.getAPI().sendFriendRequest(
                    playerUuid,
                    target.getUuid()
            );

            handleRequestResult(playerRef, target, result);

            // Refresh the UI
            openFriendsGUI(playerRef, store, playerUuid, FriendsUI.Tab.FRIENDS);
        });
        // Remove friend listeners - keyed by UUID to avoid index/sort-order mismatch
        for (FriendData friend : friends) {
            String removeId = "remove-" + friend.getFriendUuid().toString().replace("-", "");
            page.addEventListener(removeId, CustomUIEventBindingType.Activating, event -> {
                plugin.getAPI().removeFriend(playerUuid, friend.getFriendUuid());

                // Async username fetch for message
                plugin.getUsernameCache().getUsernameAsync(friend.getFriendUuid())
                        .thenAccept(username -> {
                            playerRef.sendMessage(Message.raw(plugin.getLang().format("ui.removed-from-friends", "player", username)).color(Color.GREEN));
                        });

                openFriendsGUI(playerRef, store, playerUuid, FriendsUI.Tab.FRIENDS);
            });
        }
    }

    private void setupRequestsListeners(PageBuilder page, PlayerRef playerRef, Store<EntityStore> store,
                                        UUID playerUuid, List<FriendRequest> incoming) {
        for (int i = 0; i < incoming.size(); i++) {
            final int index = i;

            page.addEventListener("accept-" + i, CustomUIEventBindingType.Activating, event -> {
                FriendRequest request = incoming.get(index);
                plugin.getAPI().acceptFriendRequest(playerUuid, request.getSenderUuid());

                // Async username fetch for message
                plugin.getUsernameCache().getUsernameAsync(request.getSenderUuid())
                        .thenAccept(username -> {
                            playerRef.sendMessage(Message.raw(plugin.getLang().format("ui.now-friends", "player", username)).color(Color.GREEN));
                        });

                openFriendsGUI(playerRef, store, playerUuid, FriendsUI.Tab.REQUESTS);
            });

            page.addEventListener("deny-" + i, CustomUIEventBindingType.Activating, event -> {
                FriendRequest request = incoming.get(index);
                plugin.getAPI().denyFriendRequest(playerUuid, request.getSenderUuid());

                // Async username fetch for message
                plugin.getUsernameCache().getUsernameAsync(request.getSenderUuid())
                        .thenAccept(username -> {
                            playerRef.sendMessage(Message.raw(plugin.getLang().format("ui.denied-request", "player", username)).color(Color.RED));
                        });

                openFriendsGUI(playerRef, store, playerUuid, FriendsUI.Tab.REQUESTS);
            });
        }
    }

    private void setupPendingListeners(PageBuilder page, PlayerRef playerRef, Store<EntityStore> store,
                                       UUID playerUuid, List<FriendRequest> outgoing) {
        for (int i = 0; i < outgoing.size(); i++) {
            final int index = i;
            page.addEventListener("cancel-" + i, CustomUIEventBindingType.Activating, event -> {
                FriendRequest request = outgoing.get(index);
                plugin.getAPI().cancelFriendRequest(playerUuid, request.getReceiverUuid());

                // Async username fetch for message
                plugin.getUsernameCache().getUsernameAsync(request.getReceiverUuid())
                        .thenAccept(username -> {
                            playerRef.sendMessage(Message.raw(plugin.getLang().format("ui.cancelled-request", "player", username)).color(Color.YELLOW));
                        });

                openFriendsGUI(playerRef, store, playerUuid, FriendsUI.Tab.PENDING);
            });
        }
    }

    private void setupSettingsListeners(PageBuilder page, PlayerRef playerRef, UUID playerUuid) {
        page.addEventListener("toggle-notifications", CustomUIEventBindingType.ValueChanged, (event, ctx) -> {
            plugin.getAPI().toggleNotifications(playerUuid);
            FriendSettings settings = plugin.getAPI().getSettings(playerUuid);
            boolean enabled = settings != null && settings.isNotificationsEnabled();
            Lang lang = plugin.getLang();
            String state = enabled ? lang.get("ui.state.enabled") : lang.get("ui.state.disabled");
            playerRef.sendMessage(Message.raw(lang.format("ui.notifications-toggled", "state", state)).color(Color.GREEN));
        });

        page.addEventListener("toggle-requests", CustomUIEventBindingType.ValueChanged, (event, ctx) -> {
            plugin.getAPI().toggleAllowRequests(playerUuid);
            FriendSettings settings = plugin.getAPI().getSettings(playerUuid);
            boolean enabled = settings != null && settings.isAllowRequests();
            Lang lang = plugin.getLang();
            String state = enabled ? lang.get("ui.state.enabled") : lang.get("ui.state.disabled");
            playerRef.sendMessage(Message.raw(lang.format("ui.requests-toggled", "state", state)).color(Color.GREEN));
        });
    }

    private void handleRequestResult(PlayerRef sender, PlayerRef target, FriendsManager.FriendRequestResult result) {
        Lang lang = plugin.getLang();
        String prefix = lang.get("prefix") + " ";
        switch (result) {
            case SUCCESS -> {
                sender.sendMessage(Message.raw("")
                        .insert(Message.raw(prefix).color(Color.YELLOW).bold(true))
                        .insert(Message.raw(lang.get("request.sent")).color(Color.GRAY))
                        .insert(Message.raw(target.getUsername()).color(Color.GREEN).bold(true))
                        .insert(Message.raw(lang.get("request.sent-suffix")).color(Color.GRAY)));

                target.sendMessage(Message.raw("")
                        .insert(Message.raw(prefix).color(Color.YELLOW).bold(true))
                        .insert(Message.raw(sender.getUsername()).color(Color.GREEN).bold(true))
                        .insert(Message.raw(lang.get("request.received")).color(Color.GRAY))
                        .insert(Message.raw(lang.format("request.received-cmd", "player", sender.getUsername())).color(new Color(0, 255, 255)))
                        .insert(Message.raw(lang.get("request.received-suffix")).color(Color.GRAY)));
            }
            case AUTO_ACCEPTED -> {
                sender.sendMessage(Message.raw("")
                        .insert(Message.raw(prefix).color(Color.YELLOW).bold(true))
                        .insert(Message.raw(lang.get("request.now-friends")).color(Color.GRAY))
                        .insert(Message.raw(target.getUsername()).color(Color.GREEN).bold(true))
                        .insert(Message.raw(lang.get("request.now-friends-suffix")).color(Color.GRAY)));

                target.sendMessage(Message.raw("")
                        .insert(Message.raw(prefix).color(Color.YELLOW).bold(true))
                        .insert(Message.raw(lang.get("request.now-friends")).color(Color.GRAY))
                        .insert(Message.raw(sender.getUsername()).color(Color.GREEN).bold(true))
                        .insert(Message.raw(lang.get("request.now-friends-suffix")).color(Color.GRAY)));
            }
            case ALREADY_FRIENDS ->
                    sender.sendMessage(Message.raw(lang.format("request.already-friends", "player", target.getUsername())).color(Color.RED));
            case REQUEST_ALREADY_SENT ->
                    sender.sendMessage(Message.raw(lang.format("request.already-sent", "player", target.getUsername())).color(Color.RED));
            case CANNOT_ADD_SELF ->
                    sender.sendMessage(Message.raw(lang.get("request.cannot-self")).color(Color.RED));
            case RECEIVER_NOT_ACCEPTING_REQUESTS ->
                    sender.sendMessage(Message.raw(lang.format("request.not-accepting", "player", target.getUsername())).color(Color.RED));
            case PLAYER_BLOCKED ->
                    sender.sendMessage(Message.raw(lang.format("request.you-blocked", "player", target.getUsername())).color(Color.RED));
            case BLOCKED_BY_PLAYER ->
                    sender.sendMessage(Message.raw(lang.format("request.not-accepting", "player", target.getUsername())).color(Color.RED));
            case SENDER_FRIENDS_LIST_FULL ->
                    sender.sendMessage(Message.raw(lang.get("request.sender-full")).color(Color.RED));
            case RECEIVER_FRIENDS_LIST_FULL ->
                    sender.sendMessage(Message.raw(lang.format("request.receiver-full", "player", target.getUsername())).color(Color.RED));
            case REQUEST_ON_COOLDOWN ->
                    sender.sendMessage(Message.raw(lang.format("request.cooldown", "player", target.getUsername())).color(Color.RED));
            default ->
                    sender.sendMessage(Message.raw(lang.format("request.failed", "result", result.name())).color(Color.RED));
        }
    }

    private String getExpirationTime(long sentAt) {
        long timeRemaining = requestExpiryMs - (System.currentTimeMillis() - sentAt);

        if (timeRemaining <= 0) {
            return plugin.getLang().get("ui.expired");
        }

        long days = timeRemaining / (24 * 60 * 60 * 1000L);
        if (days > 0) {
            return days + (days == 1 ? " day" : " days");
        }

        long hours = timeRemaining / (60 * 60 * 1000L);
        if (hours > 0) {
            return hours + (hours == 1 ? " hour" : " hours");
        }

        long minutes = timeRemaining / (60 * 1000L);
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }
}