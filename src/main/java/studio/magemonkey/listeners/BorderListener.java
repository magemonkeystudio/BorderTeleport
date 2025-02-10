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

    // Offset from config
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

        // Retrieve the offset from config
        this.offset = plugin.getConfig().getInt("teleport.offset", 20);

        plugin.getLogger().info("[BorderListener] Initialized for region: " + currentRegionKey
                + " with boundaries: X[" + currentMinX + ", " + currentMaxX + "] Z["
                + currentMinZ + ", " + currentMaxZ + "]");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Debug: Log the movement event
        if (to != null) {
            plugin.getLogger().info("[DEBUG] onPlayerMove: " + player.getName()
                    + " moved from (" + from.getX() + ", " + from.getY() + ", " + from.getZ()
                    + ", yaw=" + from.getYaw() + ", pitch=" + from.getPitch() + ")"
                    + " to (" + to.getX() + ", " + to.getY() + ", " + to.getZ()
                    + ", yaw=" + to.getYaw() + ", pitch=" + to.getPitch() + ")");
        } else {
            // If 'to' is null, just exit
            plugin.getLogger().info("[DEBUG] onPlayerMove: " + player.getName()
                    + " event.getTo() is null, skipping!");
            return;
        }

        // If the player is still in the same region, do nothing
        if (isWithinCurrentRegion(to)) {
            plugin.getLogger().info("[DEBUG] " + player.getName()
                    + " remains within region " + currentRegionKey);
            return;
        }

        // Identify which region the player is moving into
        String destinationRegionKey = ConfigHandler.getRegionForLocation(to);
        plugin.getLogger().info("[DEBUG] Destination region key for " + player.getName()
                + ": " + destinationRegionKey);

        if (destinationRegionKey == null || destinationRegionKey.equalsIgnoreCase(currentRegionKey)) {
            plugin.getLogger().info("[DEBUG] No valid new region (or same) for " + player.getName());
            return;
        }

        // Fetch the corresponding server for that region
        ConfigurationSection destSection = plugin.getConfig().getConfigurationSection("regions." + destinationRegionKey);
        if (destSection == null) {
            plugin.getLogger().severe("[BorderListener] No config section for region: " + destinationRegionKey);
            return;
        }
        String destServer = destSection.getString("server-name");
        if (destServer == null) {
            plugin.getLogger().severe("[BorderListener] No server-name defined for region: " + destinationRegionKey);
            return;
        }
        plugin.getLogger().info("[DEBUG] " + player.getName()
                + " crossing from " + currentRegionKey + " to server: " + destServer);

        // Check cooldown
        String playerId = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        int cooldownSeconds = ConfigHandler.getTeleportRequestTimeout();
        if (pendingTransfers.containsKey(playerId)) {
            long lastRequestTime = pendingTransfers.get(playerId);
            long diff = currentTime - lastRequestTime;
            if (diff < cooldownSeconds * 1000L) {
                plugin.getLogger().info("[DEBUG] Transfer request for " + player.getName()
                        + " is on cooldown (" + diff + "ms < "
                        + cooldownSeconds * 1000L + "ms). Skipping!");
                return;
            }
        }

        // Update the last transfer request time
        pendingTransfers.put(playerId, currentTime);

        // Calculate final coordinates (with offset) before saving to DB
        Location offsetLoc = to.clone();
        applyOffset(offsetLoc);

        // Capture final yaw/pitch
        float yaw = offsetLoc.getYaw();
        float pitch = offsetLoc.getPitch();

        String crossingDirection = getCrossingDirection(to);

        // Debug logging
        plugin.getLogger().info("[DEBUG] " + player.getName()
                + " offsetLoc after applyOffset => X=" + offsetLoc.getX()
                + ", Y=" + offsetLoc.getY() + ", Z=" + offsetLoc.getZ()
                + ", yaw=" + yaw + ", pitch=" + pitch
                + ", crossingDirection=" + crossingDirection);

        // Save data asynchronously, then connect the player once it's saved
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getLogger().info("[DEBUG][Async] Saving transfer data for "
                    + player.getName() + " => server:" + destServer
                    + " x=" + offsetLoc.getBlockX()
                    + " y=" + offsetLoc.getBlockY()
                    + " z=" + offsetLoc.getBlockZ()
                    + " yaw=" + yaw
                    + " pitch=" + pitch);

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
                    plugin.getLogger().info("[DEBUG] Now sending "
                            + player.getName() + " to server " + destServer);
                    sendPlayerToServer(player, destServer);
                } else {
                    plugin.getLogger().info("[DEBUG] " + player.getName()
                            + " is no longer online, skipping server transfer.");
                }
            });
        });
    }

    /**
     * Applies the offset based on which boundary is crossed.
     */
    private void applyOffset(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        if (x < currentMinX) {
            loc.setX(x - offset);
        } else if (x > currentMaxX) {
            loc.setX(x + offset);
        } else if (z < currentMinZ) {
            loc.setZ(z - offset);
        } else if (z > currentMaxZ) {
            loc.setZ(z + offset);
        }
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
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("Error sending plugin message: " + e.getMessage());
        }
    }
}
