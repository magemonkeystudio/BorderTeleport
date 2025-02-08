package studio.magemonkey.borderteleport.handlers;

import studio.magemonkey.borderteleport.BorderTeleport;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class TeleportHandler implements PluginMessageListener {
    private final BorderTeleport plugin;
    private static final String TELEPORT_SUBCHANNEL = "BorderTP";
    private final HashMap<UUID, Long> teleportCooldowns = new HashMap<>();

    public TeleportHandler(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    private String cleanCoordinateString(String input) {
        return input.replaceAll("^[^\\d-]+", "");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String subchannel = in.readUTF();
            if (subchannel.equals(TELEPORT_SUBCHANNEL)) {
                String data = in.readUTF().trim();
                plugin.getLogger().info("DEBUG: Raw data received: " + data);
                String[] rawParts = data.split(";");
                List<String> partsList = new ArrayList<>();
                for (String part : rawParts) {
                    if (!part.trim().isEmpty()) {
                        partsList.add(part.trim());
                    }
                }
                if (partsList.size() < 1) {
                    throw new IllegalArgumentException("No coordinate data found in message: " + data);
                }
                String coordinatePart = partsList.get(0);
                String[] coords = coordinatePart.split(",");
                if (coords.length < 5) {
                    throw new IllegalArgumentException("Coordinate data incomplete: " + coordinatePart);
                }
                String rawX = cleanCoordinateString(coords[0].replace("'", "").trim());
                String rawY = cleanCoordinateString(coords[1].replace("'", "").trim());
                String rawZ = cleanCoordinateString(coords[2].replace("'", "").trim());
                String rawYaw = cleanCoordinateString(coords[3].replace("'", "").trim());
                String rawPitch = cleanCoordinateString(coords[4].replace("'", "").trim());
                double x = Double.parseDouble(rawX);
                double y = Double.parseDouble(rawY);
                double z = Double.parseDouble(rawZ);
                float yaw = Float.parseFloat(rawYaw);
                float pitch = Float.parseFloat(rawPitch);
                plugin.getLogger().info("DEBUG: Parsed coordinates - x: " + x + ", y: " + y + ", z: " + z);
                plugin.getLogger().info("DEBUG: Yaw: " + yaw + ", Pitch: " + pitch);
                Location newLoc = new Location(player.getWorld(), x, y, z, yaw, pitch);
                player.teleport(newLoc);
                plugin.getLogger().info("DEBUG: Player teleported to new location with facing direction");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // (The attemptTeleport method is provided as an example for triggering a teleport.)
    public void attemptTeleport(Player player, String direction) {
        // Implementation omitted for brevity.
    }
}
