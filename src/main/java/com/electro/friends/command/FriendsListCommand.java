package com.electro.friends.command;

import com.electro.friends.FriendsPlugin;
import com.electro.friends.model.FriendData;
import com.electro.friends.model.FriendsProfile;
import com.electro.friends.util.Lang;
import com.electro.friends.util.MessageUtil;
import com.electro.friends.util.UsernameCache;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class FriendsListCommand extends CommandBase {
    private final FriendsPlugin plugin;

    public FriendsListCommand(@Nonnull FriendsPlugin plugin) {
        super("list", "View your friends list");
        this.requirePermission("friends");
        this.plugin = plugin;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {

        PlayerRef playerRef = Universe.get().getPlayer(context.sender().getUuid());

        Lang lang = plugin.getLang();
        FriendsProfile profile = plugin.getAPI().getProfile(playerRef.getUuid());
        if (profile == null) {
            playerRef.sendMessage(Message.raw(lang.get("error.profile-not-loaded")).color(Color.RED));
            return;
        }

        List<FriendData> friends = profile.getFriendsList();

        if (friends.isEmpty()) {
            playerRef.sendMessage(Message.raw("")
                    .insert(Message.raw(lang.get("prefix") + " ").color(Color.YELLOW).bold(true))
                    .insert(Message.raw(lang.get("list.no-friends")).color(Color.GRAY))
                    .insert(Message.raw(lang.get("list.no-friends-cmd")).color(new Color(0, 255, 255)))
                    .insert(Message.raw(lang.get("list.no-friends-suffix")).color(Color.GRAY)));
            return;
        }

        // Separate online and offline friends
        List<FriendData> onlineFriends = new ArrayList<>();
        List<FriendData> offlineFriends = new ArrayList<>();

        for (FriendData friend : friends) {
            if (plugin.getAPI().isOnline(friend.getFriendUuid())) {
                onlineFriends.add(friend);
            } else {
                offlineFriends.add(friend);
            }
        }

        // Fetch all usernames asynchronously and build the message
        buildFriendsListMessageAsync(onlineFriends, offlineFriends, friends.size()).thenAccept(message -> {
            playerRef.sendMessage(message);
        }).exceptionally(ex -> {
            playerRef.sendMessage(Message.raw(lang.get("list.error")).color(Color.RED));
            ex.printStackTrace();
            return null;
        });
    }

    private CompletableFuture<Message> buildFriendsListMessageAsync(
            @Nonnull List<FriendData> onlineFriends,
            @Nonnull List<FriendData> offlineFriends,
            int totalFriends) {

        UsernameCache cache = plugin.getUsernameCache();
        List<CompletableFuture<FriendDisplay>> futures = new ArrayList<>();

        // Create futures for online friends
        for (FriendData friend : onlineFriends) {
            CompletableFuture<FriendDisplay> future = cache.getUsernameAsync(friend.getFriendUuid())
                    .thenApply(username -> new FriendDisplay(
                            username,
                            MessageUtil.formatRelativeTime(friend.getSince()),
                            true // isOnline
                    ));
            futures.add(future);
        }

        // Create futures for offline friends
        for (FriendData friend : offlineFriends) {
            CompletableFuture<FriendDisplay> future = cache.getUsernameAsync(friend.getFriendUuid())
                    .thenApply(username -> new FriendDisplay(
                            username,
                            MessageUtil.formatRelativeTime(friend.getSince()),
                            false // isOnline
                    ));
            futures.add(future);
        }

        // Wait for all futures to complete and build the message
        Lang lang = plugin.getLang();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    // Build message header
                    Message message = Message.raw("\n")
                            .insert(Message.raw("========================================\n").color(Color.YELLOW).bold(true))
                            .insert(Message.raw("  Friends List ").color(Color.YELLOW).bold(true))
                            .insert(Message.raw("(" + totalFriends + " total)\n").color(Color.GRAY))
                            .insert(Message.raw("========================================\n").color(Color.YELLOW).bold(true));

                    // Add online friends
                    if (!onlineFriends.isEmpty()) {
                        message.insert(Message.raw(lang.format("list.online-header", "count", String.valueOf(onlineFriends.size()))).color(Color.GREEN).bold(true));

                        for (int i = 0; i < onlineFriends.size(); i++) {
                            FriendDisplay display = futures.get(i).join();

                            message.insert(Message.raw("  • ").color(Color.GREEN))
                                    .insert(Message.raw(display.username).color(Color.WHITE).bold(true))
                                    .insert(Message.raw(lang.format("list.friends-since", "time", display.since)).color(Color.GRAY));
                        }
                    }

                    // Add offline friends
                    if (!offlineFriends.isEmpty()) {
                        message.insert(Message.raw(lang.format("list.offline-header", "count", String.valueOf(offlineFriends.size()))).color(Color.RED).bold(true));

                        for (int i = 0; i < offlineFriends.size(); i++) {
                            FriendDisplay display = futures.get(onlineFriends.size() + i).join();

                            message.insert(Message.raw("  • ").color(Color.GRAY))
                                    .insert(Message.raw(display.username).color(Color.GRAY))
                                    .insert(Message.raw(lang.format("list.friends-since", "time", display.since)).color(Color.DARK_GRAY));
                        }
                    }

                    message.insert(Message.raw("========================================\n").color(Color.YELLOW).bold(true));

                    return message;
                });
    }

    // Helper record to hold friend display information
    private record FriendDisplay(String username, String since, boolean isOnline) {}
}