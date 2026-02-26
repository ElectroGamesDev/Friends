package com.electro.friends.command;

import com.electro.friends.FriendsPlugin;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import javax.annotation.Nonnull;


public class FriendsCommands extends AbstractCommandCollection {
    private final FriendsPlugin plugin;

    public FriendsCommands(@Nonnull FriendsPlugin plugin) {
        super("friends", "Friends System Commands");
        this.addAliases("friend");
        this.plugin = plugin;
        this.requirePermission("friends");

        // Add subcommands
        this.addSubCommand(new AddFriendCommand(plugin));
        this.addSubCommand(new AcceptFriendCommand(plugin));
        this.addSubCommand(new DenyFriendCommand(plugin));
        this.addSubCommand(new CancelFriendCommand(plugin));
        this.addSubCommand(new UnfriendCommand(plugin));
        this.addSubCommand(new FriendsListCommand(plugin));
        this.addSubCommand(new FriendRequestsCommand(plugin));
        this.addSubCommand(new FriendSettingsCommand(plugin));
        this.addSubCommand(new FriendUICommand(plugin));
        this.addSubCommand(new BlockCommand(plugin));
        this.addSubCommand(new UnblockCommand(plugin));
        this.addSubCommand(new FriendsAdminCommand(plugin));
    }
}
