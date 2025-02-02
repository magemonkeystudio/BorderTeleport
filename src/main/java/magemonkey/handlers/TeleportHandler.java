package magemonkey.handlers;

import magemonkey.BorderTeleport;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class TeleportHandler implements PluginMessageListener {
    private final BorderTeleport plugin;

    public TeleportHandler(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // Handle teleport message from BungeeCord
    }

    public void attemptTeleport(Player player, String direction) {
        String targetServer = plugin.getConfigHandler().getNeighboringServer(plugin.getCurrentRegionKey(), direction);

        if (targetServer != null) {
            plugin.getPluginLogger().info("[BorderTeleport] Teleporting player " + player.getName() + " to " + targetServer);
            // Implement teleport logic
        } else {
            plugin.getPluginLogger().warning("[ERROR] No neighboring server found for direction: " + direction);
        }
    }

    public void cleanupExpiredTeleports() {
        plugin.getPluginLogger().info("[BorderTeleport] Cleaning up expired teleport requests...");
        // Implement cleanup logic
    }
}
