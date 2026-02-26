package com.electro.friends.command;

import au.ellie.hyui.builders.PageBuilder;
import com.electro.friends.FriendsPlugin;
import com.electro.friends.manager.FriendsManager;
import com.electro.friends.model.FriendData;
import com.electro.friends.model.FriendRequest;
import com.electro.friends.model.FriendSettings;
import com.electro.friends.model.FriendsProfile;
import com.electro.friends.ui.FriendsUI;
import com.electro.friends.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.hypixel.hytale.server.core.NameMatching.EXACT_IGNORE_CASE;
import static com.hypixel.hytale.server.core.command.commands.player.inventory.InventorySeeCommand.MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD;


public class FriendUICommand extends AbstractAsyncCommand {

    private final FriendsPlugin plugin;

    public FriendUICommand(@Nonnull FriendsPlugin plugin) {
        super("ui", "Open the Friends management UI");
        setAllowsExtraArguments(true);
        this.requirePermission("friends");
        this.plugin = plugin;
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        var sender = commandContext.sender();
        if (!(sender instanceof Player player)) {
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            commandContext.sendMessage(MESSAGE_COMMANDS_ERRORS_PLAYER_NOT_IN_WORLD);
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                plugin.friendsUI.openFriendsGUI(playerRef, store, playerRef.getUuid(), FriendsUI.Tab.FRIENDS);
            }
        }, world);
    }
}