// TeleportHandler.java
package magemonkey.handlers;

import magemonkey.BorderTeleport;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import java.io.*;
import java.util.HashMap;
import java.util.UUID;

public class TeleportHandler implements PluginMessageListener {
    private final BorderTeleport plugin;
    private final HashMap<UUID, Long> teleportCooldowns = new HashMap<>();
    private static final String TELEPORT_SUBCHANNEL = "BorderTP";

    public TeleportHandler(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String subchannel = in.readUTF();

            if (subchannel.equals(TELEPORT_SUBCHANNEL)) {
                String coordData = in.readUTF();
                String[] coords = coordData.split(",");
                double x = Double.parseDouble(coords[0]);
                double y = Double.parseDouble(coords[1]);
                double z = Double.parseDouble(coords[2]);

                plugin.getLogger().info("DEBUG: Received coordinates for " + player.getName() + ": x=" + x + ", y=" + y + ", z=" + z);

                Location currentLoc = player.getLocation();
                Location newLoc = new Location(
                        currentLoc.getWorld(),
                        x,
                        y,
                        z,
                        currentLoc.getYaw(),
                        currentLoc.getPitch()
                );

                player.teleport(newLoc);
                plugin.getLogger().info("DEBUG: Teleported " + player.getName() + " to " + newLoc);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error processing teleport coordinates: " + e.getMessage());
        }
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
        double targetY = loc.getY();
        double targetZ = adjustCoordinate(loc.getZ(), direction, "z");

        plugin.getLogger().info("DEBUG: Sending coordinates for " + player.getName() + ": x=" + targetX + ", y=" + targetY + ", z=" + targetZ);

        try {
            ByteArrayOutputStream coordBytes = new ByteArrayOutputStream();
            DataOutputStream coordOut = new DataOutputStream(coordBytes);
            coordOut.writeUTF("Forward");
            coordOut.writeUTF(targetServer);
            coordOut.writeUTF(TELEPORT_SUBCHANNEL);

            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgOut = new DataOutputStream(msgBytes);
            msgOut.writeUTF(targetX + "," + targetY + "," + targetZ);

            coordOut.writeShort(msgBytes.toByteArray().length);
            coordOut.write(msgBytes.toByteArray());

            player.sendPluginMessage(plugin, "BungeeCord", coordBytes.toByteArray());

            ByteArrayOutputStream serverBytes = new ByteArrayOutputStream();
            DataOutputStream serverOut = new DataOutputStream(serverBytes);
            serverOut.writeUTF("Connect");
            serverOut.writeUTF(targetServer);

            player.sendPluginMessage(plugin, "BungeeCord", serverBytes.toByteArray());
            plugin.getLogger().info("DEBUG: Sent transfer request to " + targetServer);

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to send teleport data: " + e.getMessage());
        }
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
}