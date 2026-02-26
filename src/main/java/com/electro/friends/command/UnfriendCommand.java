package com.electro.friends.command;

import com.electro.friends.FriendsPlugin;
import com.electro.friends.manager.FriendsManager;
import com.electro.friends.model.FriendData;
import com.electro.friends.model.FriendsProfile;
import com.electro.friends.util.Lang;
import com.electro.friends.util.UsernameCache;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class UnfriendCommand extends CommandBase {
    private final FriendsPlugin plugin;

    public UnfriendCommand(@Nonnull FriendsPlugin plugin) {
        super("unfriend", "Remove a friend");
        this.setAllowsExtraArguments(true);
        this.requirePermission("friends");
        this.plugin = plugin;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {

        PlayerRef playerRef = Universe.get().getPlayer(context.sender().getUuid());

        String[] args = context.getInputString().split(" ");

        Lang lang = plugin.getLang();
        if (args.length == 2) {
            playerRef.sendMessage(Message.raw(lang.get("usage.unfriend")).color(Color.RED));
            return;
        }

        String targetUsername = args[2];

        // Get player's profile
        FriendsProfile profile = plugin.getAPI().getProfile(playerRef.getUuid());
        if (profile == null) {
            playerRef.sendMessage(Message.raw(lang.get("error.profile-not-loaded")).color(Color.RED));
            return;
        }

        // Find friend by username asynchronously
        findFriendByUsernameAsync(profile, targetUsername).thenAccept(friendUuid -> {
            if (friendUuid == null) {
                playerRef.sendMessage(Message.raw(lang.format("unfriend.not-friends", "player", targetUsername)).color(Color.RED));
                return;
            }

            // Remove friend
            FriendsManager.FriendRequestResult result = plugin.getAPI().removeFriend(
                    playerRef.getUuid(),
                    friendUuid
            );

            if (result == FriendsManager.FriendRequestResult.SUCCESS) {
                String prefix = lang.get("prefix") + " ";
                // Fetch username asynchronously for the message
                plugin.getUsernameCache().getUsernameAsync(friendUuid).thenAccept(username -> {
                    playerRef.sendMessage(Message.raw("")
                            .insert(Message.raw(prefix).color(Color.YELLOW).bold(true))
                            .insert(Message.raw(lang.get("unfriend.removed")).color(Color.GRAY))
                            .insert(Message.raw(username).color(Color.RED))
                            .insert(Message.raw(lang.get("unfriend.removed-suffix")).color(Color.GRAY)));
                });
            } else {
                playerRef.sendMessage(Message.raw(lang.format("unfriend.failed", "result", result.name())).color(Color.RED));
            }
        }).exceptionally(ex -> {
            playerRef.sendMessage(Message.raw(lang.get("error.loading-removal")).color(Color.RED));
            ex.printStackTrace();
            return null;
        });
    }

    private CompletableFuture<UUID> findFriendByUsernameAsync(@Nonnull FriendsProfile profile, @Nonnull String username) {
        UsernameCache cache = plugin.getUsernameCache();
        List<FriendData> friends = profile.getFriendsList();

        if (friends.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // Create futures for all username lookups
        List<CompletableFuture<FriendMatch>> futures = friends.stream()
                .map(friend -> cache.getUsernameAsync(friend.getFriendUuid())
                        .thenApply(friendName -> new FriendMatch(friend.getFriendUuid(), friendName)))
                .toList();

        // Wait for all futures and find the matching one
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    for (CompletableFuture<FriendMatch> future : futures) {
                        FriendMatch match = future.join();
                        if (match.username.equalsIgnoreCase(username)) {
                            return match.uuid;
                        }
                    }
                    return null;
                });
    }

    // Helper record to hold UUID and username pairs
    private record FriendMatch(UUID uuid, String username) {}
}