package com.electro.friends.command;

import com.electro.friends.FriendsPlugin;
import com.electro.friends.manager.FriendsManager;
import com.electro.friends.model.FriendData;
import com.electro.friends.model.FriendRequest;
import com.electro.friends.model.FriendsProfile;
import com.electro.friends.util.MessageUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.hypixel.hytale.server.core.NameMatching.EXACT_IGNORE_CASE;


public class FriendsAdminCommand extends CommandBase {
    private final FriendsPlugin plugin;

    public FriendsAdminCommand(@Nonnull FriendsPlugin plugin) {
        super("admin", "Friends admin commands");
        this.setAllowsExtraArguments(true);
        this.requirePermission("friends.admin");
        this.plugin = plugin;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef sender = Universe.get().getPlayer(context.sender().getUuid());

        String[] args = context.getInputString().split(" ");

        if (args.length < 3) {
            sendUsage(sender);
            return;
        }

        String subCommand = args[2].toLowerCase();

        switch (subCommand) {
            case "lookup" -> handleLookup(sender, args);
            case "forcefriend" -> handleForceFriend(sender, args);
            case "forceunfriend" -> handleForceUnfriend(sender, args);
            case "clearrequests" -> handleClearRequests(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender);
        }
    }

    private void handleLookup(@Nonnull PlayerRef sender, @Nonnull String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Message.raw("Usage: /friends admin lookup <player>").color(Color.RED));
            return;
        }

        String targetUsername = args[3];
        PlayerRef target = Universe.get().getPlayerByUsername(targetUsername, EXACT_IGNORE_CASE);
        if (target == null) {
            sender.sendMessage(Message.raw("Player not found or not online: " + targetUsername).color(Color.RED));
            return;
        }

        FriendsProfile profile = plugin.getAPI().getProfile(target.getUuid());
        if (profile == null) {
            sender.sendMessage(Message.raw("Profile not loaded for: " + targetUsername).color(Color.RED));
            return;
        }

        List<FriendData> friends = profile.getFriendsList();
        List<FriendRequest> incoming = profile.getIncomingRequests();
        List<FriendRequest> outgoing = profile.getOutgoingRequests();
        Set<UUID> blocked = profile.getBlockedPlayers();

        // Build async username lists
        CompletableFuture.runAsync(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("\n========== Friends Admin: ").append(target.getUsername()).append(" ==========\n");
            sb.append("UUID: ").append(target.getUuid()).append("\n");
            sb.append("Friends: ").append(friends.size()).append("/").append(plugin.getMaxFriends()).append("\n");
            sb.append("Incoming Requests: ").append(incoming.size()).append("\n");
            sb.append("Outgoing Requests: ").append(outgoing.size()).append("\n");
            sb.append("Blocked Players: ").append(blocked.size()).append("\n");
            sb.append("Notifications: ").append(profile.getSettings().isNotificationsEnabled() ? "ON" : "OFF").append("\n");
            sb.append("Allow Requests: ").append(profile.getSettings().isAllowRequests() ? "ON" : "OFF").append("\n");

            // Friends list
            if (!friends.isEmpty()) {
                sb.append("\nFriends List:\n");
                for (FriendData friend : friends) {
                    String name = plugin.getUsernameCache().getUsernameCached(friend.getFriendUuid());
                    boolean online = plugin.getAPI().isOnline(friend.getFriendUuid());
                    sb.append("  - ").append(name).append(online ? " (Online)" : " (Offline)").append("\n");
                }
            }

            // Blocked list
            if (!blocked.isEmpty()) {
                sb.append("\nBlocked Players:\n");
                for (UUID blockedUuid : blocked) {
                    String name = plugin.getUsernameCache().getUsernameCached(blockedUuid);
                    sb.append("  - ").append(name).append("\n");
                }
            }

            sb.append("=================================================");

            sender.sendMessage(Message.raw(sb.toString()).color(Color.YELLOW));
        });
    }

    private void handleForceFriend(@Nonnull PlayerRef sender, @Nonnull String[] args) {
        if (args.length < 5) {
            sender.sendMessage(Message.raw("Usage: /friends admin forcefriend <player1> <player2>").color(Color.RED));
            return;
        }

        PlayerRef player1 = Universe.get().getPlayerByUsername(args[3], EXACT_IGNORE_CASE);
        PlayerRef player2 = Universe.get().getPlayerByUsername(args[4], EXACT_IGNORE_CASE);

        if (player1 == null) {
            sender.sendMessage(Message.raw("Player not found or not online: " + args[3]).color(Color.RED));
            return;
        }
        if (player2 == null) {
            sender.sendMessage(Message.raw("Player not found or not online: " + args[4]).color(Color.RED));
            return;
        }

        if (player1.getUuid().equals(player2.getUuid())) {
            sender.sendMessage(Message.raw("Cannot friend a player with themselves!").color(Color.RED));
            return;
        }

        plugin.getFriendsManager().forceAddFriend(player1.getUuid(), player2.getUuid());

        sender.sendMessage(Message.raw("")
                .insert(Message.raw("[Friends Admin] ").color(Color.RED).bold(true))
                .insert(Message.raw("Forced friendship between ").color(Color.GRAY))
                .insert(Message.raw(player1.getUsername()).color(Color.GREEN))
                .insert(Message.raw(" and ").color(Color.GRAY))
                .insert(Message.raw(player2.getUsername()).color(Color.GREEN))
                .insert(Message.raw(".").color(Color.GRAY)));
    }

    private void handleForceUnfriend(@Nonnull PlayerRef sender, @Nonnull String[] args) {
        if (args.length < 5) {
            sender.sendMessage(Message.raw("Usage: /friends admin forceunfriend <player1> <player2>").color(Color.RED));
            return;
        }

        PlayerRef player1 = Universe.get().getPlayerByUsername(args[3], EXACT_IGNORE_CASE);
        PlayerRef player2 = Universe.get().getPlayerByUsername(args[4], EXACT_IGNORE_CASE);

        if (player1 == null) {
            sender.sendMessage(Message.raw("Player not found or not online: " + args[3]).color(Color.RED));
            return;
        }
        if (player2 == null) {
            sender.sendMessage(Message.raw("Player not found or not online: " + args[4]).color(Color.RED));
            return;
        }

        plugin.getFriendsManager().forceRemoveFriend(player1.getUuid(), player2.getUuid());

        sender.sendMessage(Message.raw("")
                .insert(Message.raw("[Friends Admin] ").color(Color.RED).bold(true))
                .insert(Message.raw("Forced unfriend between ").color(Color.GRAY))
                .insert(Message.raw(player1.getUsername()).color(Color.GREEN))
                .insert(Message.raw(" and ").color(Color.GRAY))
                .insert(Message.raw(player2.getUsername()).color(Color.GREEN))
                .insert(Message.raw(".").color(Color.GRAY)));
    }

    private void handleClearRequests(@Nonnull PlayerRef sender, @Nonnull String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Message.raw("Usage: /friends admin clearrequests <player>").color(Color.RED));
            return;
        }

        PlayerRef target = Universe.get().getPlayerByUsername(args[3], EXACT_IGNORE_CASE);
        if (target == null) {
            sender.sendMessage(Message.raw("Player not found or not online: " + args[3]).color(Color.RED));
            return;
        }

        plugin.getFriendsManager().clearAllRequests(target.getUuid());

        sender.sendMessage(Message.raw("")
                .insert(Message.raw("[Friends Admin] ").color(Color.RED).bold(true))
                .insert(Message.raw("Cleared all friend requests for ").color(Color.GRAY))
                .insert(Message.raw(target.getUsername()).color(Color.GREEN))
                .insert(Message.raw(".").color(Color.GRAY)));
    }

    private void handleReload(@Nonnull PlayerRef sender) {
        plugin.reloadConfig();

        sender.sendMessage(Message.raw("")
                .insert(Message.raw("[Friends Admin] ").color(Color.RED).bold(true))
                .insert(Message.raw("Configuration reloaded.").color(Color.GREEN)));
    }

    private void sendUsage(@Nonnull PlayerRef player) {
        Message message = Message.raw("\n")
                .insert(Message.raw("========================================\n").color(Color.RED).bold(true))
                .insert(Message.raw("  Friends Admin Commands\n").color(Color.RED).bold(true))
                .insert(Message.raw("========================================\n").color(Color.RED).bold(true))
                .insert(Message.raw("\n/friends admin lookup <player>").color(Color.YELLOW))
                .insert(Message.raw(" - View player info\n").color(Color.GRAY))
                .insert(Message.raw("/friends admin forcefriend <p1> <p2>").color(Color.YELLOW))
                .insert(Message.raw(" - Force friendship\n").color(Color.GRAY))
                .insert(Message.raw("/friends admin forceunfriend <p1> <p2>").color(Color.YELLOW))
                .insert(Message.raw(" - Force unfriend\n").color(Color.GRAY))
                .insert(Message.raw("/friends admin clearrequests <player>").color(Color.YELLOW))
                .insert(Message.raw(" - Clear all requests\n").color(Color.GRAY))
                .insert(Message.raw("/friends admin reload").color(Color.YELLOW))
                .insert(Message.raw(" - Reload config\n").color(Color.GRAY))
                .insert(Message.raw("========================================\n").color(Color.RED).bold(true));

        player.sendMessage(message);
    }
}
