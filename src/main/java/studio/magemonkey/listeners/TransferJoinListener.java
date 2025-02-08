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
    private final int offset;

    public TransferJoinListener(BorderTeleport plugin, MySQLManager mysql) {
        this.plugin = plugin;
        this.mysql = mysql;
        // Use a default offset of 20 blocks if not defined in the config.
        this.offset = plugin.getConfig().getInt("transfer.offset", 20);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Delay to ensure the player is fully loaded.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            MySQLManager.TransferData data = mysql.getTransferData(player.getUniqueId().toString());
            if (data != null) {
                Location newLoc = player.getLocation().clone();
                newLoc.setX(data.x);
                newLoc.setY(data.y);
                newLoc.setZ(data.z);

                // Apply the offset in the direction the player crossed.
                switch(data.direction.toUpperCase()) {
                    case "NORTH":
                        newLoc.setZ(newLoc.getZ() - offset);
                        break;
                    case "SOUTH":
                        newLoc.setZ(newLoc.getZ() + offset);
                        break;
                    case "EAST":
                        newLoc.setX(newLoc.getX() + offset);
                        break;
                    case "WEST":
                        newLoc.setX(newLoc.getX() - offset);
                        break;
                }

                player.teleport(newLoc);
                mysql.deleteTransferData(player.getUniqueId().toString());
                player.sendMessage("You have been transferred to a new server location!");
            }
        }, 20L); // 20 ticks delay (~1 second)
    }
}
