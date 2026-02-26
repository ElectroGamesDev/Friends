package com.electro.friends.command;

import com.electro.friends.FriendsPlugin;
import com.electro.friends.model.FriendSettings;
import com.electro.friends.model.FriendsProfile;
import com.electro.friends.util.Lang;
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
import java.util.List;


public class FriendSettingsCommand extends CommandBase {
    private final FriendsPlugin plugin;

    public FriendSettingsCommand(@Nonnull FriendsPlugin plugin) {
        super("settings", "Manage friend settings");
        setAllowsExtraArguments(true);
        this.requirePermission("friends");
        this.plugin = plugin;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef playerRef = Universe.get().getPlayer(context.sender().getUuid());

        String[] args = context.getInputString().split(" ");

        Lang lang = plugin.getLang();
        FriendsProfile profile = plugin.getAPI().getProfile(playerRef.getUuid());
        if (profile == null) {
            playerRef.sendMessage(Message.raw(lang.get("error.profile-not-loaded")).color(Color.RED));
            return;
        }

        if (args.length == 2) {
            showSettings(playerRef, profile);
            return;
        }

        String setting = args[2].toLowerCase();
        String prefix = lang.get("prefix") + " ";

        switch (setting) {
            case "notifications", "notifs" -> {
                plugin.getAPI().toggleNotifications(playerRef.getUuid());
                FriendSettings settings = profile.getSettings();
                boolean enabled = settings.isNotificationsEnabled();
                String state = enabled ? lang.get("settings.enabled") : lang.get("settings.disabled");

                playerRef.sendMessage(Message.raw("")
                        .insert(Message.raw(prefix).color(Color.YELLOW).bold(true))
                        .insert(Message.raw(lang.get("settings.notifications")).color(Color.GRAY))
                        .insert(Message.raw(state).color(enabled ? Color.GREEN : Color.RED).bold(true)));
            }
            case "requests", "allowrequests" -> {
                plugin.getAPI().toggleAllowRequests(playerRef.getUuid());
                FriendSettings settings = profile.getSettings();
                boolean enabled = settings.isAllowRequests();
                String state = enabled ? lang.get("settings.enabled") : lang.get("settings.disabled");

                playerRef.sendMessage(Message.raw("")
                        .insert(Message.raw(prefix).color(Color.YELLOW).bold(true))
                        .insert(Message.raw(lang.get("settings.requests-toggle")).color(Color.GRAY))
                        .insert(Message.raw(state).color(enabled ? Color.GREEN : Color.RED).bold(true)));
            }
            default -> {
                playerRef.sendMessage(Message.raw(lang.format("error.unknown-setting", "setting", setting)).color(Color.RED));
                sendUsage(playerRef);
            }
        }
    }

    private void showSettings(@Nonnull PlayerRef player, @Nonnull FriendsProfile profile) {
        Lang lang = plugin.getLang();
        FriendSettings settings = profile.getSettings();
        boolean notifsEnabled = settings.isNotificationsEnabled();
        boolean requestsEnabled = settings.isAllowRequests();

        Message message = Message.raw("\n")
                .insert(Message.raw("========================================\n").color(Color.YELLOW).bold(true))
                .insert(Message.raw("  Friend Settings\n").color(Color.YELLOW).bold(true))
                .insert(Message.raw("========================================\n").color(Color.YELLOW).bold(true))
                .insert(Message.raw("\n" + lang.get("settings.notifications-title")).color(Color.GRAY))
                .insert(Message.raw(notifsEnabled ? lang.get("settings.enabled") : lang.get("settings.disabled"))
                        .color(notifsEnabled ? Color.GREEN : Color.RED).bold(true))
                .insert(Message.raw("\n  Toggle with: ").color(Color.GRAY))
                .insert(Message.raw(lang.get("settings.toggle-notifs-cmd")).color(new Color(0, 255, 255)))
                .insert(Message.raw("\n\n" + lang.get("settings.accept-title")).color(Color.GRAY))
                .insert(Message.raw(requestsEnabled ? lang.get("settings.enabled") : lang.get("settings.disabled"))
                        .color(requestsEnabled ? Color.GREEN : Color.RED).bold(true))
                .insert(Message.raw("\n  Toggle with: ").color(Color.GRAY))
                .insert(Message.raw(lang.get("settings.toggle-requests-cmd")).color(new Color(0, 255, 255)))
                .insert(Message.raw("\n========================================\n").color(Color.YELLOW).bold(true));

        player.sendMessage(message);
    }

    private void sendUsage(@Nonnull PlayerRef player) {
        player.sendMessage(Message.raw("Settings Commands:").color(Color.YELLOW).bold(true));
        player.sendMessage(Message.raw("  /friends settings - View settings").color(Color.GRAY));
        player.sendMessage(Message.raw("  /friends settings notifications - Toggle notifications").color(Color.GRAY));
        player.sendMessage(Message.raw("  /friends settings requests - Toggle accepting requests").color(Color.GRAY));
    }
}
