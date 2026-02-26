package com.electro.friends.command;

import com.electro.friends.FriendsPlugin;
import com.electro.friends.model.FriendRequest;
import com.electro.friends.model.FriendsProfile;
import com.electro.friends.util.Lang;
import com.electro.friends.util.MessageUtil;
import com.electro.friends.util.UsernameCache;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class FriendRequestsCommand extends CommandBase {
    private final FriendsPlugin plugin;

    public FriendRequestsCommand(@Nonnull FriendsPlugin plugin) {
        super("requests", "View friend requests");
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

        List<FriendRequest> incoming = profile.getIncomingRequests();
        List<FriendRequest> outgoing = profile.getOutgoingRequests();

        if (incoming.isEmpty() && outgoing.isEmpty()) {
            playerRef.sendMessage(Message.raw("")
                    .insert(Message.raw(lang.get("prefix") + " ").color(Color.YELLOW).bold(true))
                    .insert(Message.raw(lang.get("requests.none")).color(Color.GRAY)));
            return;
        }

        // Fetch all usernames asynchronously
        buildRequestsMessageAsync(incoming, outgoing).thenAccept(message -> {
            playerRef.sendMessage(message);
        }).exceptionally(ex -> {
            playerRef.sendMessage(Message.raw(lang.get("requests.error")).color(Color.RED));
            ex.printStackTrace();
            return null;
        });
    }

    private CompletableFuture<Message> buildRequestsMessageAsync(
            @Nonnull List<FriendRequest> incoming,
            @Nonnull List<FriendRequest> outgoing) {

        UsernameCache cache = plugin.getUsernameCache();
        List<CompletableFuture<RequestDisplay>> futures = new ArrayList<>();

        // Create futures for incoming requests
        for (FriendRequest request : incoming) {
            CompletableFuture<RequestDisplay> future = cache.getUsernameAsync(request.getSenderUuid())
                    .thenApply(username -> new RequestDisplay(
                            username,
                            MessageUtil.formatRelativeTime(request.getSentAt()),
                            true // isIncoming
                    ));
            futures.add(future);
        }

        // Create futures for outgoing requests
        for (FriendRequest request : outgoing) {
            CompletableFuture<RequestDisplay> future = cache.getUsernameAsync(request.getReceiverUuid())
                    .thenApply(username -> new RequestDisplay(
                            username,
                            MessageUtil.formatRelativeTime(request.getSentAt()),
                            false // isIncoming
                    ));
            futures.add(future);
        }

        // Wait for all futures to complete and build the message
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    // Build message header
                    Message message = Message.raw("\n")
                            .insert(Message.raw("========================================\n").color(Color.YELLOW).bold(true))
                            .insert(Message.raw("  Friend Requests\n").color(Color.YELLOW).bold(true))
                            .insert(Message.raw("========================================\n").color(Color.YELLOW).bold(true));

                    Lang lang = plugin.getLang();

                    // Add incoming requests
                    if (!incoming.isEmpty()) {
                        message.insert(Message.raw(lang.format("requests.incoming-header", "count", String.valueOf(incoming.size()))).color(Color.GREEN).bold(true));

                        for (int i = 0; i < incoming.size(); i++) {
                            RequestDisplay display = futures.get(i).join();

                            message.insert(Message.raw("  • ").color(Color.GREEN))
                                    .insert(Message.raw(display.username).color(Color.WHITE).bold(true))
                                    .insert(Message.raw(lang.format("requests.sent-time", "time", display.sentAt)).color(Color.GRAY))
                                    .insert(Message.raw("    ").color(Color.GRAY))
                                    .insert(Message.raw("/friends friend accept " + display.username).color(new Color(0, 255, 255)))
                                    .insert(Message.raw(" | ").color(Color.GRAY))
                                    .insert(Message.raw("/friends friend deny " + display.username).color(Color.RED))
                                    .insert(Message.raw("\n").color(Color.GRAY));
                        }
                    }

                    // Add outgoing requests
                    if (!outgoing.isEmpty()) {
                        message.insert(Message.raw(lang.format("requests.outgoing-header", "count", String.valueOf(outgoing.size()))).color(Color.YELLOW).bold(true));

                        for (int i = 0; i < outgoing.size(); i++) {
                            RequestDisplay display = futures.get(incoming.size() + i).join();

                            message.insert(Message.raw("  • ").color(Color.YELLOW))
                                    .insert(Message.raw(display.username).color(Color.WHITE))
                                    .insert(Message.raw(lang.format("requests.sent-time", "time", display.sentAt)).color(Color.GRAY))
                                    .insert(Message.raw("    ").color(Color.GRAY))
                                    .insert(Message.raw("/friends friend cancel " + display.username).color(Color.RED))
                                    .insert(Message.raw("\n").color(Color.GRAY));
                        }
                    }

                    message.insert(Message.raw("========================================\n").color(Color.YELLOW).bold(true));

                    return message;
                });
    }

    // Helper record to hold request display information
    private record RequestDisplay(String username, String sentAt, boolean isIncoming) {}
}