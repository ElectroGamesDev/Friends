package com.electro.friends.command;

import com.electro.friends.FriendsPlugin;
import com.electro.friends.manager.FriendsManager;
import com.electro.friends.model.FriendRequest;
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


public class CancelFriendCommand extends CommandBase {
    private final FriendsPlugin plugin;

    public CancelFriendCommand(@Nonnull FriendsPlugin plugin) {
        super("cancel", "Cancel a sent friend request");
        this.setAllowsExtraArguments(true);
        this.requirePermission("friends");
        this.plugin = plugin;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef sender = Universe.get().getPlayer(context.sender().getUuid());

        String[] args = context.getInputString().split(" ");

        Lang lang = plugin.getLang();
        if (args.length < 3) {
            sender.sendMessage(Message.raw(lang.get("usage.cancel")).color(Color.RED));
            return;
        }

        String receiverUsername = args[2];

        // Find receiver by checking outgoing requests
        FriendsProfile profile = plugin.getAPI().getProfile(sender.getUuid());
        if (profile == null) {
            sender.sendMessage(Message.raw(lang.get("error.profile-not-loaded")).color(Color.RED));
            return;
        }

        // Find the receiver UUID asynchronously
        findRequestReceiverAsync(profile, receiverUsername).thenAccept(receiverUuid -> {
            if (receiverUuid == null) {
                sender.sendMessage(Message.raw(lang.format("request.no-outgoing", "player", receiverUsername)).color(Color.RED));
                return;
            }

            // Cancel friend request
            FriendsManager.FriendRequestResult result = plugin.getAPI().cancelFriendRequest(
                    sender.getUuid(),
                    receiverUuid
            );

            if (result == FriendsManager.FriendRequestResult.SUCCESS) {
                String prefix = lang.get("prefix") + " ";
                // Fetch username asynchronously for the message
                plugin.getUsernameCache().getUsernameAsync(receiverUuid).thenAccept(username -> {
                    sender.sendMessage(Message.raw("")
                            .insert(Message.raw(prefix).color(Color.YELLOW).bold(true))
                            .insert(Message.raw(lang.get("request.cancelled")).color(Color.GRAY))
                            .insert(Message.raw(username).color(Color.RED))
                            .insert(Message.raw(lang.get("request.cancelled-suffix")).color(Color.GRAY)));
                });
            } else {
                sender.sendMessage(Message.raw(lang.format("request.cancel-failed", "result", result.name())).color(Color.RED));
            }
        }).exceptionally(ex -> {
            sender.sendMessage(Message.raw(lang.get("error.loading-cancel")).color(Color.RED));
            ex.printStackTrace();
            return null;
        });
    }

    private CompletableFuture<UUID> findRequestReceiverAsync(@Nonnull FriendsProfile profile, @Nonnull String username) {
        UsernameCache cache = plugin.getUsernameCache();
        List<FriendRequest> requests = profile.getOutgoingRequests();

        if (requests.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // Create futures for all username lookups
        List<CompletableFuture<RequestMatch>> futures = requests.stream()
                .map(request -> cache.getUsernameAsync(request.getReceiverUuid())
                        .thenApply(receiverName -> new RequestMatch(request.getReceiverUuid(), receiverName)))
                .toList();

        // Wait for all futures and find the matching one
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    for (CompletableFuture<RequestMatch> future : futures) {
                        RequestMatch match = future.join();
                        if (match.username.equalsIgnoreCase(username)) {
                            return match.uuid;
                        }
                    }
                    return null;
                });
    }

    // Helper record to hold UUID and username pairs
    private record RequestMatch(UUID uuid, String username) {}
}