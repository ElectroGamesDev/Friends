[![Join our Discord](https://img.shields.io/badge/Discord-Join%20Community-7289DA?logo=discord&logoColor=white&style=for-the-badge)](https://discord.gg/Snqz9E58Dr)
[![View on GitHub](https://img.shields.io/badge/GitHub-Source%20Code-181717?logo=github&logoColor=white&style=for-the-badge)](https://github.com/ElectroGamesDev/Friends)
[![Support on Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Me-FF5E5B?logo=kofi&logoColor=white&style=for-the-badge)](https://ko-fi.com/electrogames)

**Friends** is a complete friends system plugin that allows players to connect with each other on your server.  
Send friend requests, manage your friends list, block unwanted interactions, and stay connected with online/offline friend tracking.

***

**Commands: /friends and /friend (alias)**  
**Quick UI Access: /friends ui**

**Permissions:**

`friends` - Allows players to use all standard friend commands  
`friends.admin` - Access to admin commands for managing friends

## Features

### Friends System

Build and maintain your social network on the server.

Core features include:

*   **Friend Requests** - Send, accept, deny, or cancel friend requests
*   **Friends List** - View all your friends with online/offline status
*   **Request Management** - Track incoming and outgoing friend requests
*   **Auto-cleanup** - Expired requests are automatically removed (configurable)
*   **Request Cooldown** - Prevent spam with configurable cooldown timers
*   **Friends Limit** - Configurable maximum friends per player (default: 100)

### Privacy & Control

Take control of your social experience:

*   **Block System** - Block players to prevent friend requests and interactions
*   **Privacy Settings** - Toggle whether you accept friend requests
*   **Notification Settings** - Enable/disable friend-related notifications
*   **Auto-unfriend on Block** - Blocking automatically removes existing friendships

### Request System

Smart friend request handling:

*   **Auto-accept** - If two players request each other, friendship is created automatically
*   **Expiration** - Requests expire after configurable days (default: 7 days)
*   **Cooldown Protection** - Rate limiting prevents request spam (default: 5 minutes)
*   **Mutual Detection** - System detects and handles mutual requests intelligently

***

## In-Game Management

### Command System

Use `/friends` followed by a subcommand:

**Friend Management:**

*   `/friends add <player>` - Send a friend request
*   `/friends accept <player>` - Accept an incoming request
*   `/friends deny <player>` - Deny an incoming request
*   `/friends cancel <player>` - Cancel your outgoing request
*   `/friends unfriend <player>` - Remove a friend

**Information:**

*   `/friends list` - View your friends (online and offline)
*   `/friends requests` - View pending friend requests
*   `/friends settings` - View and manage your settings

**Privacy:**

*   `/friends block <player>` - Block a player
*   `/friends unblock <player>` - Unblock a player

**Settings:**

*   `/friends settings notifications` - Toggle friend notifications
*   `/friends settings requests` - Toggle accepting friend requests

**UI Access:**

*   `/friends ui` - Open the Friends management UI

### Admin Commands

Use `/friends admin` for server management:

*   `/friends admin lookup <player>` - View detailed player info
*   `/friends admin forcefriend <player1> <player2>` - Force a friendship
*   `/friends admin forceunfriend <player1> <player2>` - Force remove friendship
*   `/friends admin clearrequests <player>` - Clear all pending requests
*   `/friends admin reload` - Reload configuration

***

## User Interface

The Friends UI provides a visual interface for managing your social connections:

*   View friends list with online/offline indicators
*   See friend-since timestamps
*   Manage incoming and outgoing requests
*   Quick accept/deny buttons
*   Access settings and privacy controls

Open with: `/friends ui`

***

## Database Support

Friends supports both **SQLite** and **MySQL** databases:

**SQLite (Default):**

*   No additional setup required
*   Data stored in `mods/FriendsData/friends.db`
*   Perfect for smaller servers

**MySQL:**

*   Configure in `config.json`
*   Supports connection pooling
*   Ideal for larger servers or cross-server setups

### Configuration

The `config.json` file allows you to customize:

```
{
  "database": {
    "type": "sqlite",  // or "mysql"
    "mysql": {
      "host": "localhost",
      "port": 3306,
      "database": "friends",
      "username": "root",
      "password": ""
    }
  },
  "maxFriends": 100,
  "requestExpiryDays": 7,
  "requestCooldownMinutes": 5,
  "cleanupIntervalMinutes": 60,
  "connectionPoolSize": 10
}
```

***

## Online Tracking

Friends automatically tracks online/offline status:

*   Real-time online friend count
*   Online/offline indicators in friends list
*   Friend join/leave notifications (if enabled)
*   Last seen timestamps

***

## Discord / Support

If you would like to join the community, suggest features, report bugs, or need some help, join the Discord community! [https://discord.gg/Snqz9E58Dr](https://discord.gg/Snqz9E58Dr)

***

## Support Friends

Want to support Friends? You can donate at [Ko-fi](https://ko-fi.com/electrogames) or share Friends with your server community!

***

# Developer API

Friends includes a comprehensive developer API for integrating with the friends system programmatically.

***

## Getting the plugin instance:

```
FriendsPlugin plugin = FriendsPlugin.get();
```

## Getting the API:

```
FriendsAPI api = plugin.getAPI();
```

## Getting the FriendsManager:

```
FriendsManager manager = plugin.getFriendsManager();
```

***

## Loading Profiles

Profiles must be loaded before accessing friend data:

```
CompletableFuture<FriendsProfile> future = manager.loadProfile(playerUuid);
future.thenAccept(profile -> {
    // Profile is now loaded and cached
});
```

Get a loaded profile:

```
FriendsProfile profile = api.getProfile(playerUuid);
```

Unload a profile when a player leaves:

```
manager.unloadProfile(playerUuid);
```

***

## Friend Operations

### Check if players are friends:

```
boolean areFriends = api.areFriends(player1Uuid, player2Uuid);
```

### Get friends list:

```
List<FriendData> friends = api.getFriends(playerUuid);
for (FriendData friend : friends) {
    UUID friendUuid = friend.getFriendUuid();
    long friendsSince = friend.getSince();
}
```

### Get friend count:

```
int count = api.getFriendCount(playerUuid);
```

### Get online friends:

```
List<UUID> onlineFriends = api.getOnlineFriends(playerUuid);
int onlineCount = api.getOnlineFriendsCount(playerUuid);
```

### Remove a friend:

```
FriendsManager.FriendRequestResult result = api.removeFriend(playerUuid, friendUuid);
if (result == FriendsManager.FriendRequestResult.SUCCESS) {
    // Friendship removed
}
```

***

## Friend Request Operations

### Send a friend request:

```
FriendsManager.FriendRequestResult result = api.sendFriendRequest(senderUuid, receiverUuid);

switch (result) {
    case SUCCESS:
        // Request sent successfully
        break;
    case AUTO_ACCEPTED:
        // Mutual request - now friends
        break;
    case ALREADY_FRIENDS:
        // Already friends
        break;
    case REQUEST_ALREADY_SENT:
        // Duplicate request
        break;
    case PLAYER_BLOCKED:
        // Sender blocked receiver
        break;
    case BLOCKED_BY_PLAYER:
        // Receiver blocked sender
        break;
    case SENDER_FRIENDS_LIST_FULL:
    case RECEIVER_FRIENDS_LIST_FULL:
        // Friends list full
        break;
    case REQUEST_ON_COOLDOWN:
        // Must wait before sending another request
        break;
    // ... other cases
}
```

### Accept a friend request:

```
FriendsManager.FriendRequestResult result = api.acceptFriendRequest(receiverUuid, senderUuid);
```

### Deny a friend request:

```
FriendsManager.FriendRequestResult result = api.denyFriendRequest(receiverUuid, senderUuid);
```

### Cancel an outgoing request:

```
FriendsManager.FriendRequestResult result = api.cancelFriendRequest(senderUuid, receiverUuid);
```

### Get pending requests:

```
List<FriendRequest> incoming = api.getIncomingRequests(playerUuid);
List<FriendRequest> outgoing = api.getOutgoingRequests(playerUuid);

for (FriendRequest request : incoming) {
    UUID senderUuid = request.getSenderUuid();
    long sentAt = request.getSentAt();
}
```

***

## Block Operations

### Block a player:

```
api.blockPlayer(playerUuid, targetUuid);
```

This automatically:

*   Removes existing friendship (if any)
*   Cancels pending requests between the players
*   Prevents future friend requests

### Unblock a player:

```
api.unblockPlayer(playerUuid, targetUuid);
```

### Check if blocked:

```
boolean isBlocked = api.isBlocked(playerUuid, targetUuid);
```

### Get blocked players:

```
Set<UUID> blockedPlayers = api.getBlockedPlayers(playerUuid);
```

***

## Settings Operations

### Get player settings:

```
FriendSettings settings = api.getSettings(playerUuid);
if (settings != null) {
    boolean notificationsEnabled = settings.isNotificationsEnabled();
    boolean allowRequests = settings.isAllowRequests();
}
```

### Toggle notifications:

```
api.toggleNotifications(playerUuid);
```

### Toggle accepting requests:

```
api.toggleAllowRequests(playerUuid);
```

***

## Online Status Tracking

### Check if player is online:

```
boolean isOnline = api.isOnline(playerUuid);
```

### Get all online players:

```
Set<UUID> onlinePlayers = api.getOnlinePlayers();
```

### Mark player online/offline (handled automatically by plugin):

```
manager.markOnline(playerUuid);
manager.markOffline(playerUuid);
```

***

## Additional Features

### Get mutual friends:

```
List<UUID> mutualFriends = api.getMutualFriends(player1Uuid, player2Uuid);
```

### Get last seen timestamp:

```
long lastSeen = api.getLastSeen(playerUuid);
```

### Force operations (admin):

```
// Force add friendship (bypasses all checks)
manager.forceAddFriend(player1Uuid, player2Uuid);

// Force remove friendship
manager.forceRemoveFriend(player1Uuid, player2Uuid);

// Clear all requests for a player
manager.clearAllRequests(playerUuid);
```

### Cleanup expired requests:

```
// Returns number of deleted requests
int deleted = manager.cleanupExpiredRequests();
```

***

## Result Enum

All friend request operations return a `FriendRequestResult`:

```
public enum FriendRequestResult {
    SUCCESS,                           // Operation completed successfully
    AUTO_ACCEPTED,                     // Mutual request created friendship
    ALREADY_FRIENDS,                   // Players are already friends
    NOT_FRIENDS,                       // Players are not friends
    REQUEST_ALREADY_SENT,              // Duplicate request
    REQUEST_NOT_FOUND,                 // Request doesn't exist
    CANNOT_ADD_SELF,                   // Can't friend yourself
    SENDER_PROFILE_NOT_LOADED,         // Sender profile not in cache
    RECEIVER_PROFILE_NOT_LOADED,       // Receiver profile not in cache
    RECEIVER_NOT_ACCEPTING_REQUESTS,   // Privacy setting blocks requests
    PLAYER_BLOCKED,                    // Sender blocked receiver
    BLOCKED_BY_PLAYER,                 // Receiver blocked sender
    SENDER_FRIENDS_LIST_FULL,          // Sender at max friends
    RECEIVER_FRIENDS_LIST_FULL,        // Receiver at max friends
    REQUEST_ON_COOLDOWN                // Must wait before retry
}
```

***

## Configuration API

### Reload configuration:

```
plugin.reloadConfig();
```

### Get configuration values:

```
int maxFriends = plugin.getMaxFriends();
int requestExpiryDays = plugin.getRequestExpiryDays();
```

***

## Credits

This plugin has been made possible by [HyUI](https://www.curseforge.com/hytale/mods/hyui).