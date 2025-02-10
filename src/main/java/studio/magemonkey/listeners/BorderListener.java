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

    private final int currentMinX;
    private final int currentMaxX;
    private final int currentMinZ;
    private final int currentMaxZ;
    private final String currentRegionKey;

    // For handling cooldowns on repeated transfers
    private final Map<String, Long> pendingTransfers = new HashMap<>();

    // Pull the offset from the config—used when crossing from this server
    private final int offset;

    public BorderListener(BorderTeleport plugin, MySQLManager mysql) {
        this.plugin = plugin;
        this.mysql = mysql;

        // Current region info
        this.currentRegionKey = ConfigHandler.getCurrentServerName();
        ConfigurationSection regionSection = ConfigHandler.getCurrentRegionSection();
        this.currentMinX = regionSection.getInt("min-x", Integer.MIN_VALUE);
        this.currentMaxX = regionSection.getInt("max-x", Integer.MAX_VALUE);
        this.currentMinZ = regionSection.getInt("min-z", Integer.MIN_VALUE);
        this.currentMaxZ = regionSection.getInt("max-z", Integer.MAX_VALUE);

        // We'll use the same offset config that was originally in TransferJoinListener
        this.offset = plugin.getConfig().getInt("teleport.offset", 20);

        plugin.getLogger().info("BorderListener initialized for region: " + currentRegionKey +
                " with boundaries: X[" + currentMinX + ", " + currentMaxX + "] Z[" + currentMinZ + ", " + currentMaxZ + "]");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        // Check if the player is still in the same region
        if (isWithinCurrentRegion(to)) {
            return;
        }

        // Identify which region the player is moving into
        String destinationRegionKey = ConfigHandler.getRegionForLocation(to);
        if (destinationRegionKey == null || destinationRegionKey.equalsIgnoreCase(currentRegionKey)) {
            // No valid new region found (or it's the same region, which can happen on boundary edges).
            return;
        }

        // Fetch the corresponding server for that region
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

        // Check cooldown
        String playerId = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        int cooldownSeconds = ConfigHandler.getTeleportRequestTimeout();
        if (pendingTransfers.containsKey(playerId)) {
            long lastRequestTime = pendingTransfers.get(playerId);
            if (currentTime - lastRequestTime < cooldownSeconds * 1000L) {
                return;
            }
        }
        // Update the last transfer request time
        pendingTransfers.put(playerId, currentTime);

        // Calculate final coordinates (with offset) before saving to DB
        Location offsetLoc = to.clone();
        applyOffset(offsetLoc);

        // Store the actual yaw/pitch so the player faces the same direction
        float yaw = offsetLoc.getYaw();
        float pitch = offsetLoc.getPitch();

        // Also keep a textual cardinal direction if you want. We won't use it to set facing though.
        String crossingDirection = getCrossingDirection(to);

        // Save data asynchronously, then connect the player once it's saved
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            mysql.savePlayerTransfer(
                    player.getUniqueId().toString(),
                    destServer,
                    offsetLoc.getBlockX(),
                    offsetLoc.getBlockY(),
                    offsetLoc.getBlockZ(),
                    crossingDirection,
                    yaw,
                    pitch
            );

            // Switch back to the main thread to send them to the next server
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    sendPlayerToServer(player, destServer);
                }
            });
        });
    }

    /**
     * Applies the offset based on which side of this region the player is crossing.
     * If they're crossing the north boundary, we shift them further north, etc.
     * This ensures they arrive already offset on the next server.
     */
    private void applyOffset(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        // If crossing the "west" boundary:
        if (x < currentMinX) {
            loc.setX(x - offset);
        }
        // If crossing the "east" boundary:
        else if (x > currentMaxX) {
            loc.setX(x + offset);
        }
        // If crossing the "north" boundary:
        else if (z < currentMinZ) {
            loc.setZ(z - offset);
        }
        // If crossing the "south" boundary:
        else if (z > currentMaxZ) {
            loc.setZ(z + offset);
        }
        // If none apply, it's an unknown scenario, no offset needed.
    }

    private boolean isWithinCurrentRegion(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return (x >= currentMinX && x <= currentMaxX && z >= currentMinZ && z <= currentMaxZ);
    }

    private String getCrossingDirection(Location loc) {
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
            // Standard BungeeCord sub-channel for transferring a single player
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("Error sending plugin message: " + e.getMessage());
        }
    }
}
