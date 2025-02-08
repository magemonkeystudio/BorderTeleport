package studio.magemonkey.handlers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.BorderTeleport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class TeleportHandler implements PluginMessageListener {
    private final BorderTeleport plugin;
    private static final String TELEPORT_SUBCHANNEL = "BorderTP";

    public TeleportHandler(@NotNull BorderTeleport plugin) {
        this.plugin = plugin;
    }

    private String cleanCoordinateString(String input) {
        return input.replaceAll("^[^\\d-]+", "");
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel,
                                        @NotNull Player player,
                                        @NotNull byte[] message) {
        if (!channel.equals("BungeeCord")) return;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String subchannel = in.readUTF();
            if (subchannel.equals(TELEPORT_SUBCHANNEL)) {
                String data = in.readUTF().trim();
                plugin.getLogger().info("DEBUG: Raw data received: " + data);
                String[] rawParts = data.split(";");
                if (rawParts.length == 0) {
                    throw new IllegalArgumentException("No coordinate data found in message: " + data);
                }
                String coordinatePart = rawParts[0];
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
        } catch (IOException e) {
            plugin.getLogger().severe("Error processing message: " + e.getMessage());
        }
    }
}
