package magemonkey.listeners;

import magemonkey.BorderTeleport;
import magemonkey.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementListener implements Listener {
    private final BorderTeleport plugin;
    private static final double BORDER_DEBUG_DISTANCE = 5.0; // Log when within 5 blocks of border

    public MovementListener(BorderTeleport plugin) {
        this.plugin = plugin;
        // Log boundaries on listener initialization
        plugin.logger.info(String.format("[BorderTeleport] Initialized with boundaries: X(%d to %d), Z(%d to %d)",
                plugin.minX, plugin.maxX, plugin.minZ, plugin.maxZ));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        // Only process if the block position changed
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        double x = to.getX();
        double z = to.getZ();

        // Detailed Z coordinate crossing check
        boolean crossingNorthBorder = from.getZ() > plugin.minZ && to.getZ() <= plugin.minZ;
        boolean crossingSouthBorder = from.getZ() < plugin.maxZ && to.getZ() >= plugin.maxZ;

        // Log when near Z border
        if (Math.abs(z - plugin.minZ) <= BORDER_DEBUG_DISTANCE || Math.abs(z - plugin.maxZ) <= BORDER_DEBUG_DISTANCE) {
            plugin.logger.info(String.format("[BorderTeleport] %s approaching Z border! z=%.2f (minZ=%d, maxZ=%d)",
                    player.getName(), z, plugin.minZ, plugin.maxZ));
        }

        // Log coordinates every time they change
        plugin.logger.info(String.format("[BorderTeleport] %s movement: FROM(x=%.2f, z=%.2f) TO(x=%.2f, z=%.2f)",
                player.getName(), from.getX(), from.getZ(), x, z));

        // Check if player is in safe zone
        if (LocationUtils.isInSafeZone(plugin, x, z)) {
            plugin.logger.info(String.format("[BorderTeleport] %s in safe zone at z=%.2f", player.getName(), z));
            return;
        }

        // Determine border crossing
        String direction = null;
        String reason = null;

        if (x >= plugin.maxX) {
            direction = "east";
            reason = String.format("x (%.2f) >= maxX (%d)", x, plugin.maxX);
        } else if (x <= plugin.minX) {
            direction = "west";
            reason = String.format("x (%.2f) <= minX (%d)", x, plugin.minX);
        } else if (z >= plugin.maxZ) {
            direction = "south";
            reason = String.format("z (%.2f) >= maxZ (%d)", z, plugin.maxZ);
        } else if (z <= plugin.minZ) {
            direction = "north";
            reason = String.format("z (%.2f) <= minZ (%d)", z, plugin.minZ);
        }

        if (direction != null) {
            plugin.logger.info(String.format("[BorderTeleport] Border crossing detected for %s: Direction=%s, Reason=%s",
                    player.getName(), direction, reason));

            // Additional debug info before handling crossing
            plugin.logger.info(String.format("[BorderTeleport] Current server: %s, Region: %s",
                    plugin.currentServerName, plugin.currentRegionKey));
            plugin.logger.info(String.format("[BorderTeleport] Crossing borders: From(%.2f, %.2f) To(%.2f, %.2f)",
                    from.getX(), from.getZ(), x, z));

            plugin.teleportHandler.handleBorderCrossing(player, x, z);
        }
    }
}