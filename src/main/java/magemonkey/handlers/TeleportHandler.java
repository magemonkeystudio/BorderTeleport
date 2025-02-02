// TeleportHandler.java
package magemonkey.handlers;

import magemonkey.BorderTeleport;
import magemonkey.data.PendingTeleport;
import org.bukkit.Location;
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
        if (!channel.equals("BungeeCord")) return;
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

        Location loc = player.getLocation();
        double targetX = adjustCoordinate(loc.getX(), direction, "x");
        double targetZ = adjustCoordinate(loc.getZ(), direction, "z");

        plugin.getLogger().info("Creating pending teleport from " + loc + " to x=" + targetX + ", z=" + targetZ);

        PendingTeleport pending = new PendingTeleport(targetX, targetZ, targetServer);
        plugin.getPendingTeleports().put(player.getUniqueId(), pending);
        sendToServer(player, targetServer);
    }

    private double adjustCoordinate(double coord, String direction, String axis) {
        if (axis.equals("x")) {
            if (direction.equals("east")) return 10;
            if (direction.equals("west")) return -10;
        } else if (axis.equals("z")) {
            if (direction.equals("south")) return 10;
            if (direction.equals("north")) return -10;
        }
        return coord;
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