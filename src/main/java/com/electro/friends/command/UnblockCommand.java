package com.electro.friends.command;

import com.electro.friends.FriendsPlugin;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.List;

import static com.hypixel.hytale.server.core.NameMatching.EXACT_IGNORE_CASE;


public class UnblockCommand extends CommandBase {
    private final FriendsPlugin plugin;

    public UnblockCommand(@Nonnull FriendsPlugin plugin) {
        super("unblock", "Unblock a player");
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
            sender.sendMessage(Message.raw(lang.get("usage.unblock")).color(Color.RED));
            return;
        }

        String targetUsername = args[2];

        FriendsProfile profile = plugin.getAPI().getProfile(sender.getUuid());
        if (profile == null) {
            sender.sendMessage(Message.raw(lang.get("error.profile-not-loaded")).color(Color.RED));
            return;
        }

        Set<UUID> blockedPlayers = profile.getBlockedPlayers();
        if (blockedPlayers.isEmpty()) {
            sender.sendMessage(Message.raw(lang.get("unblock.no-blocked")).color(Color.RED));
            return;
        }

        // Find blocked player by username asynchronously
        findBlockedByUsernameAsync(blockedPlayers, targetUsername).thenAccept(blockedUuid -> {
            if (blockedUuid == null) {
                sender.sendMessage(Message.raw(lang.format("unblock.not-in-list", "player", targetUsername)).color(Color.RED));
                return;
            }

            plugin.getAPI().unblockPlayer(sender.getUuid(), blockedUuid);

            String prefix = lang.get("prefix") + " ";
            plugin.getUsernameCache().getUsernameAsync(blockedUuid).thenAccept(username -> {
                sender.sendMessage(Message.raw("")
                        .insert(Message.raw(prefix).color(Color.YELLOW).bold(true))
                        .insert(Message.raw(lang.get("unblock.unblocked")).color(Color.GRAY))
                        .insert(Message.raw(username).color(Color.GREEN).bold(true))
                        .insert(Message.raw(lang.get("unblock.unblocked-suffix")).color(Color.GRAY)));
            });
        }).exceptionally(ex -> {
            sender.sendMessage(Message.raw(lang.get("error.loading-unblock")).color(Color.RED));
            ex.printStackTrace();
            return null;
        });
    }

    private CompletableFuture<UUID> findBlockedByUsernameAsync(@Nonnull Set<UUID> blockedPlayers, @Nonnull String username) {
        UsernameCache cache = plugin.getUsernameCache();

        List<CompletableFuture<BlockedMatch>> futures = blockedPlayers.stream()
                .map(uuid -> cache.getUsernameAsync(uuid)
                        .thenApply(name -> new BlockedMatch(uuid, name)))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    for (CompletableFuture<BlockedMatch> future : futures) {
                        BlockedMatch match = future.join();
                        if (match.username.equalsIgnoreCase(username)) {
                            return match.uuid;
                        }
                    }
                    return null;
                });
    }

    private record BlockedMatch(UUID uuid, String username) {}
}
