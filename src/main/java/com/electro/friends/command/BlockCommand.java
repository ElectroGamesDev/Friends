package com.electro.friends.command;

import com.electro.friends.FriendsPlugin;
import com.electro.friends.model.FriendsProfile;
import com.electro.friends.util.Lang;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.awt.*;

import static com.hypixel.hytale.server.core.NameMatching.EXACT_IGNORE_CASE;


public class BlockCommand extends CommandBase {
    private final FriendsPlugin plugin;

    public BlockCommand(@Nonnull FriendsPlugin plugin) {
        super("block", "Block a player");
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
            sender.sendMessage(Message.raw(lang.get("usage.block")).color(Color.RED));
            return;
        }

        String targetUsername = args[2];

        // Find target player
        PlayerRef target = Universe.get().getPlayerByUsername(targetUsername, EXACT_IGNORE_CASE);
        if (target == null) {
            sender.sendMessage(Message.raw(lang.format("block.not-found", "player", targetUsername)).color(Color.RED));
            return;
        }

        if (sender.getUuid().equals(target.getUuid())) {
            sender.sendMessage(Message.raw(lang.get("block.cannot-self")).color(Color.RED));
            return;
        }

        FriendsProfile profile = plugin.getAPI().getProfile(sender.getUuid());
        if (profile == null) {
            sender.sendMessage(Message.raw(lang.get("error.profile-not-loaded")).color(Color.RED));
            return;
        }

        if (profile.isBlocked(target.getUuid())) {
            sender.sendMessage(Message.raw(lang.format("block.already-blocked", "player", target.getUsername())).color(Color.RED));
            return;
        }

        plugin.getAPI().blockPlayer(sender.getUuid(), target.getUuid());

        sender.sendMessage(Message.raw("")
                .insert(Message.raw(lang.get("prefix") + " ").color(Color.YELLOW).bold(true))
                .insert(Message.raw(lang.get("block.blocked")).color(Color.GRAY))
                .insert(Message.raw(target.getUsername()).color(Color.RED).bold(true))
                .insert(Message.raw(lang.get("block.blocked-suffix")).color(Color.GRAY)));
    }
}
