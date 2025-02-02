// TeleportHandler.java
package magemonkey.handlers;

import magemonkey.BorderTeleport;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class TeleportHandler implements PluginMessageListener {
    private final BorderTeleport plugin;
    private final HashMap<UUID, Long> teleportCooldowns = new HashMap<>();

    public TeleportHandler(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        plugin.getLogger().info("Received BungeeCord message for player " + player.getName());
    }

    public void attemptTeleport(Player player, String direction) {
        if (plugin.getCurrentRegionKey() == null) {
            plugin.getLogger().severe("Cannot teleport: current region is null");
            return;
        }

        String targetServer = plugin.getConfigHandler().getNeighboringServer(plugin.getCurrentRegionKey(), direction);
        if (targetServer == null) {
            plugin.getLogger().warning("No neighboring server found for direction: " + direction);
            return;
        }

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (teleportCooldowns.containsKey(playerId) &&
                (currentTime - teleportCooldowns.get(playerId)) < plugin.getTeleportCooldownMs()) {
            return;
        }

        teleportCooldowns.put(playerId, currentTime);
        plugin.getLogger().info("Attempting to teleport " + player.getName() + " to " + targetServer);
        sendToServer(player, targetServer);
    }

    private void sendToServer(Player player, String server) {
        try {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteArray);

            out.writeUTF("Connect");
            out.writeUTF(server);

            player.sendPluginMessage(plugin, "BungeeCord", byteArray.toByteArray());
            plugin.getLogger().info("Sent connect message for " + player.getName() + " to server: " + server);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to send player to server: " + e.getMessage());
        }
    }

}