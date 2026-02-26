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


public class DenyFriendCommand extends CommandBase {
    private final FriendsPlugin plugin;

    public DenyFriendCommand(@Nonnull FriendsPlugin plugin) {
        super("deny", "Deny a friend request");
        this.setAllowsExtraArguments(true);
        this.requirePermission("friends");
        this.plugin = plugin;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef receiver = Universe.get().getPlayer(context.sender().getUuid());

        String[] args = context.getInputString().split(" ");

        Lang lang = plugin.getLang();
        if (args.length < 3) {
            receiver.sendMessage(Message.raw(lang.get("usage.deny")).color(Color.RED));
            return;
        }

        String senderUsername = args[2];

        // Find sender by checking incoming requests
        FriendsProfile profile = plugin.getAPI().getProfile(receiver.getUuid());
        if (profile == null) {
            receiver.sendMessage(Message.raw(lang.get("error.profile-not-loaded")).color(Color.RED));
            return;
        }

        // Find the sender UUID asynchronously
        findRequestSenderAsync(profile, senderUsername).thenAccept(senderUuid -> {
            if (senderUuid == null) {
                receiver.sendMessage(Message.raw(lang.format("request.no-request-from", "player", senderUsername)).color(Color.RED));
                return;
            }

            // Deny friend request
            FriendsManager.FriendRequestResult result = plugin.getAPI().denyFriendRequest(
                    receiver.getUuid(),
                    senderUuid
            );

            if (result == FriendsManager.FriendRequestResult.SUCCESS) {
                String prefix = lang.get("prefix") + " ";
                // Fetch username asynchronously for the message
                plugin.getUsernameCache().getUsernameAsync(senderUuid).thenAccept(username -> {
                    receiver.sendMessage(Message.raw("")
                            .insert(Message.raw(prefix).color(Color.YELLOW).bold(true))
                            .insert(Message.raw(lang.get("request.denied")).color(Color.GRAY))
                            .insert(Message.raw(username).color(Color.RED))
                            .insert(Message.raw(lang.get("request.denied-suffix")).color(Color.GRAY)));
                });
            } else {
                receiver.sendMessage(Message.raw(lang.format("request.deny-failed", "result", result.name())).color(Color.RED));
            }
        }).exceptionally(ex -> {
            receiver.sendMessage(Message.raw(lang.get("error.loading-deny")).color(Color.RED));
            ex.printStackTrace();
            return null;
        });
    }

    private CompletableFuture<UUID> findRequestSenderAsync(@Nonnull FriendsProfile profile, @Nonnull String username) {
        UsernameCache cache = plugin.getUsernameCache();
        List<FriendRequest> requests = profile.getIncomingRequests();

        if (requests.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // Create futures for all username lookups
        List<CompletableFuture<RequestMatch>> futures = requests.stream()
                .map(request -> cache.getUsernameAsync(request.getSenderUuid())
                        .thenApply(senderName -> new RequestMatch(request.getSenderUuid(), senderName)))
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