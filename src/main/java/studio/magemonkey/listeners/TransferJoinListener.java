package studio.magemonkey.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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

        // Run on the next tick (0 delay), so the player is fully loaded
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Check asynchronously for transfer data
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                MySQLManager.TransferData data = mysql.getTransferData(player.getUniqueId().toString());
                if (data != null) {
                    // Switch to the main thread for teleportation
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline()) {
                            return; // If they've logged out, skip.
                        }

                        // We skip the offset here; it's already applied on the source server.
                        // Just use the coordinates + yaw/pitch from the DB
                        Location newLoc = player.getLocation().clone();
                        newLoc.setX(data.x);
                        newLoc.setY(data.y);
                        newLoc.setZ(data.z);
                        newLoc.setYaw(data.yaw);
                        newLoc.setPitch(data.pitch);

                        player.teleport(newLoc);

                        // Now remove the data from the DB
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            mysql.deleteTransferData(player.getUniqueId().toString());
                        });
                    });
                }
            });
        });
    }
}
