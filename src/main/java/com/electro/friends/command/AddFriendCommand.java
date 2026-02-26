package com.electro.friends.command;

import com.electro.friends.FriendsPlugin;
import com.electro.friends.manager.FriendsManager;
import com.electro.friends.util.Lang;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.awt.*;

import static com.hypixel.hytale.server.core.NameMatching.EXACT_IGNORE_CASE;


public class AddFriendCommand extends CommandBase {
    private final FriendsPlugin plugin;

    public AddFriendCommand(@Nonnull FriendsPlugin plugin) {
        super("add", "Send a friend request");
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
            sender.sendMessage(Message.raw(lang.get("usage.add")).color(Color.RED));
            return;
        }

        String targetUsername = args[2];

        // Find target player
        PlayerRef target = findPlayerByUsername(targetUsername);
        if (target == null) {
            sender.sendMessage(Message.raw(lang.format("block.not-found", "player", targetUsername)).color(Color.RED));
            return;
        }

        // Send friend request
        FriendsManager.FriendRequestResult result = plugin.getAPI().sendFriendRequest(
                sender.getUuid(),
                target.getUuid()
        );

        handleRequestResult(sender, target, result, "send");
    }

    private void handleRequestResult(
            @Nonnull PlayerRef sender,
            @Nonnull PlayerRef target,
            @Nonnull FriendsManager.FriendRequestResult result,
            @Nonnull String action) {

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

    private PlayerRef findPlayerByUsername(@Nonnull String username) {
        return Universe.get().getPlayerByUsername(username, EXACT_IGNORE_CASE);
    }
}