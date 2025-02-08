package studio.magemonkey.borderteleport.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import studio.magemonkey.borderteleport.BorderTeleport;
import studio.magemonkey.borderteleport.database.MySQLManager;
import studio.magemonkey.borderteleport.handlers.ConfigHandler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class BorderListener implements Listener {
    private final BorderTeleport plugin;
    private final MySQLManager mysql;
    private final ConfigHandler configHandler;
    private final long cooldownMs;
    private final int safeZoneDistance;

    // Map to store teleport cooldown timestamps for players.
    private final HashMap<String, Long> teleportCooldowns = new HashMap<>();

    // Current region boundaries.
    private final int currentMinX;
    private final int currentMaxX;
    private final int currentMinZ;
    private final int currentMaxZ;
    private final String currentRegionKey; // e.g., "southwest"

    public BorderListener(BorderTeleport plugin, MySQLManager mysql) {
        this.plugin = plugin;
        this.mysql = mysql;
        this.configHandler = plugin.getConfigHandler();
        this.safeZoneDistance = configHandler.getSafeZoneDistance();
        this.cooldownMs = configHandler.getTeleportCooldownMs();

        // Get current region key and boundaries from config.
        this.currentRegionKey = configHandler.getCurrentServerName();
        org.bukkit.configuration.ConfigurationSection regionSection = configHandler.getCurrentRegionSection();
        this.currentMinX = regionSection.getInt("min-x");
        this.currentMaxX = regionSection.getInt("max-x");
        this.currentMinZ = regionSection.getInt("min-z");
        this.currentMaxZ = regionSection.getInt("max-z");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        // Check if the player has left the current region and is outside the safe zone.
        if (!isWithinRegion(to) && isOutsideSafeZone(to)) {
            long now = System.currentTimeMillis();
            String uuid = player.getUniqueId().toString();
            if (teleportCooldowns.containsKey(uuid) && now - teleportCooldowns.get(uuid) < cooldownMs) {
                return;
            }

            // Determine destination region based on the player's new location.
            String destinationRegionKey = configHandler.getRegionForLocation(to);
            if (destinationRegionKey == null || destinationRegionKey.equalsIgnoreCase(currentRegionKey)) {
                return;
            }
            // Get the destination server name from the region configuration.
            String destServer = plugin.getConfig().getConfigurationSection("regions." + destinationRegionKey)
                    .getString("server-name");

            // Save the transfer data to MySQL.
            mysql.savePlayerTransfer(
                    uuid,
                    destServer,
                    to.getBlockX(),
                    to.getBlockY(),
                    to.getBlockZ(),
                    getTransferDirection(to)
            );

            // Set teleport cooldown.
            teleportCooldowns.put(uuid, now);

            // Send a plugin message to BungeeCord to transfer the player.
            sendPlayerToServer(player, destServer);
        }
    }

    private boolean isWithinRegion(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return (x >= currentMinX && x <= currentMaxX && z >= currentMinZ && z <= currentMaxZ);
    }

    private boolean isOutsideSafeZone(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        if (Math.abs(x - currentMinX) < safeZoneDistance || Math.abs(currentMaxX - x) < safeZoneDistance) {
            return false;
        }
        if (Math.abs(z - currentMinZ) < safeZoneDistance || Math.abs(currentMaxZ - z) < safeZoneDistance) {
            return false;
        }
        return true;
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
            e.printStackTrace();
        }
    }
}
