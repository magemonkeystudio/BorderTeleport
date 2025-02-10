package studio.magemonkey.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import studio.magemonkey.BorderTeleport;
import studio.magemonkey.database.MySQLManager;

public class TransferJoinListener implements Listener {
    private final BorderTeleport plugin;
    private final MySQLManager mysql;

    public TransferJoinListener(BorderTeleport plugin, MySQLManager mysql) {
        this.plugin = plugin;
        this.mysql = mysql;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Defer to next tick so the player is fully loaded
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Fetch data asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                MySQLManager.TransferData data = mysql.getTransferData(player.getUniqueId().toString());
                if (data != null) {
                    plugin.getLogger().info("[DEBUG][Async] Found transfer data for " + player.getName()
                            + " => x=" + data.x + ", y=" + data.y + ", z=" + data.z
                            + ", yaw=" + data.yaw + ", pitch=" + data.pitch
                            + ", direction=" + data.direction);

                    // Switch back to the main thread to teleport
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline()) {
                            plugin.getLogger().info("[DEBUG] " + player.getName()
                                    + " has logged out before teleport, skipping.");
                            return;
                        }

                        // Reconstruct the stored location, including yaw/pitch
                        Location newLoc = player.getLocation().clone();
                        newLoc.setX(data.x);
                        newLoc.setY(data.y);
                        newLoc.setZ(data.z);
                        newLoc.setYaw(data.yaw);
                        newLoc.setPitch(data.pitch);

                        plugin.getLogger().info("[DEBUG] Original DB location for " + player.getName()
                                + ": " + newLoc);

                        // Ensure newLoc is safe (not inside a block).
                        // This method nudges the Y level upward until we find air.
                        Location safeLoc = findSafeYAbove(newLoc);

                        plugin.getLogger().info("[DEBUG] Final safe location for " + player.getName()
                                + ": " + safeLoc);

                        // Teleport the player once to the adjusted safe location
                        player.teleport(safeLoc);

                        // Delete from DB asynchronously
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            plugin.getLogger().info("[DEBUG][Async] Deleting transfer data for " + player.getName());
                            mysql.deleteTransferData(player.getUniqueId().toString());
                        });
                    });
                } else {
                    plugin.getLogger().info("[DEBUG][Async] No transfer data found for " + player.getName());
                }
            });
        });
    }

    /**
     * Nudges the Y-level upward until the block at that location is air (non-solid).
     * This extra logging shows which blocks we encounter while moving up.
     */
    private Location findSafeYAbove(Location baseLoc) {
        Location testLoc = baseLoc.clone();
        World world = testLoc.getWorld();
        if (world == null) {
            // If no world, just return the baseLoc
            plugin.getLogger().warning("[DEBUG] World is null in findSafeYAbove, returning baseLoc unchanged.");
            return testLoc;
        }

        while (true) {
            Block blockAtFeet = testLoc.getBlock();
            // You might also consider the player's head block if you have a 2-block-tall entity
            Block blockAtHead = world.getBlockAt(testLoc.getBlockX(), testLoc.getBlockY() + 1, testLoc.getBlockZ());

            Material feetMat = blockAtFeet.getType();
            Material headMat = blockAtHead.getType();

            // Log what we see at the feet & head
            plugin.getLogger().info("[DEBUG] Checking Y=" + testLoc.getBlockY()
                    + " => feet=" + feetMat + " (solid=" + feetMat.isSolid() + "), "
                    + "head=" + headMat + " (solid=" + headMat.isSolid() + ")");

            // If neither feet nor head is solid, we've found a safe spot
            if (!feetMat.isSolid() && !headMat.isSolid()) {
                plugin.getLogger().info("[DEBUG] Found air at Y=" + testLoc.getBlockY() +
                        ". This is our safe location.");
                break;
            }

            // Otherwise, move up by 1
            testLoc.add(0, 1, 0);

            // Safety check: if we exceed world height, stop
            if (testLoc.getY() > world.getMaxHeight()) {
                plugin.getLogger().warning("[DEBUG] Exceeded max world height while nudging up. Breaking out.");
                break;
            }
        }

        return testLoc;
    }
}
