package studio.magemonkey.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import studio.magemonkey.BorderTeleport;
import studio.magemonkey.database.MySQLManager;
import studio.magemonkey.handlers.ConfigHandler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BorderListener implements Listener {
    private final BorderTeleport plugin;
    private final MySQLManager mysql;

    // Current region boundaries.
    private final int currentMinX;
    private final int currentMaxX;
    private final int currentMinZ;
    private final int currentMaxZ;
    private final String currentRegionKey; // e.g., "bottomleft"

    // Map to store the timestamp of the last transfer request for each player (keyed by UUID).
    private final Map<String, Long> pendingTransfers = new HashMap<>();

    public BorderListener(BorderTeleport plugin, MySQLManager mysql) {
        this.plugin = plugin;
        this.mysql = mysql;
        // Retrieve current server name from ConfigHandler.
        this.currentRegionKey = ConfigHandler.getCurrentServerName();
        // Retrieve region configuration for the current server.
        ConfigurationSection regionSection = ConfigHandler.getCurrentRegionSection();
        this.currentMinX = regionSection.getInt("min-x", Integer.MIN_VALUE);
        this.currentMaxX = regionSection.getInt("max-x", Integer.MAX_VALUE);
        this.currentMinZ = regionSection.getInt("min-z", Integer.MIN_VALUE);
        this.currentMaxZ = regionSection.getInt("max-z", Integer.MAX_VALUE);
        plugin.getLogger().info("BorderListener initialized for region: " + currentRegionKey +
                " with boundaries: X[" + currentMinX + ", " + currentMaxX + "] Z[" + currentMinZ + ", " + currentMaxZ + "]");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        plugin.getLogger().info("Player " + player.getName() + " moved to: " + to.toVector().toString());

        if (to == null) {
            plugin.getLogger().warning("Destination location is null for player " + player.getName());
            return;
        }

        // If the player is still within the current region, do nothing.
        if (isWithinRegion(to)) {
            plugin.getLogger().info("Player " + player.getName() + " remains within region " + currentRegionKey);
            return;
        }

        // Determine destination region based on the player's new location.
        String destinationRegionKey = ConfigHandler.getRegionForLocation(to);
        plugin.getLogger().info("Destination region key for " + player.getName() + ": " + destinationRegionKey);
        if (destinationRegionKey == null || destinationRegionKey.equalsIgnoreCase(currentRegionKey)) {
            plugin.getLogger().info("No valid destination region found (or same as current) for " + player.getName());
            return;
        }

        // Get the destination server name from the region configuration.
        ConfigurationSection destSection = plugin.getConfig().getConfigurationSection("regions." + destinationRegionKey);
        if (destSection == null) {
            plugin.getLogger().severe("No configuration section for region: " + destinationRegionKey);
            return;
        }
        String destServer = destSection.getString("server-name");
        if (destServer == null) {
            plugin.getLogger().severe("No server-name defined for region: " + destinationRegionKey);
            return;
        }
        plugin.getLogger().info("Player " + player.getName() + " will be transferred to server: " + destServer);

        // Save the transfer data to MySQL.
        mysql.savePlayerTransfer(
                player.getUniqueId().toString(),
                destServer,
                to.getBlockX(),
                to.getBlockY(),
                to.getBlockZ(),
                getTransferDirection(to)
        );
        plugin.getLogger().info("Transfer data saved for " + player.getName());

        // Check the cooldown for transfer requests.
        String playerId = player.getUniqueId().toString();
        int cooldownSeconds = ConfigHandler.getTeleportRequestTimeout(); // from config
        long currentTime = System.currentTimeMillis();
        if (pendingTransfers.containsKey(playerId)) {
            long lastRequestTime = pendingTransfers.get(playerId);
            if (currentTime - lastRequestTime < cooldownSeconds * 1000L) {
                plugin.getLogger().info("Transfer request for " + player.getName() + " is on cooldown; skipping additional request.");
                return;
            }
        }
        // Update the pending transfer timestamp.
        pendingTransfers.put(playerId, currentTime);

        // Send the transfer request immediately.
        sendPlayerToServer(player, destServer);
        plugin.getLogger().info("Sent server transfer request for " + player.getName() + " to server " + destServer);
    }

    private boolean isWithinRegion(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return (x >= currentMinX && x <= currentMaxX && z >= currentMinZ && z <= currentMaxZ);
    }

    private String getTransferDirection(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        if (x < currentMinX) {
            return "WEST";
        } else if (x > currentMaxX) {
            return "EAST";
        } else if (z < currentMinZ) {
            return "NORTH";
        } else if (z > currentMaxZ) {
            return "SOUTH";
        }
        return "UNKNOWN";
    }

    private void sendPlayerToServer(Player player, String server) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("Error sending plugin message: " + e.getMessage());
        }
    }
}
