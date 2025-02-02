// JoinListener.java
package magemonkey.listeners;

import magemonkey.BorderTeleport;
import magemonkey.data.PendingTeleport;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class JoinListener implements Listener {
    private final BorderTeleport plugin;

    public JoinListener(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        PendingTeleport pending = plugin.getPendingTeleports().get(player.getUniqueId());

        plugin.getLogger().info("Player joined: " + player.getName());
        plugin.getLogger().info("Current location: " + player.getLocation());
        plugin.getLogger().info("Pending teleport exists: " + (pending != null));

        if (pending != null) {
            plugin.getLogger().info("Target coordinates: x=" + pending.getX() + ", z=" + pending.getZ());
            if (!pending.hasExpired(plugin.getRequestTimeoutSeconds(), plugin.getGracePeriodSeconds())) {
                plugin.getLogger().info("Teleport not expired, scheduling teleport");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location currentLoc = player.getLocation();
                        Location newLoc = new Location(
                                currentLoc.getWorld(),
                                pending.getX(),
                                currentLoc.getY(),
                                pending.getZ(),
                                currentLoc.getYaw(),
                                currentLoc.getPitch()
                        );
                        player.teleport(newLoc);
                        plugin.getLogger().info("Teleported player to: " + newLoc);
                    }
                }.runTaskLater(plugin, 20L);
            } else {
                plugin.getLogger().info("Teleport expired");
            }
            plugin.getPendingTeleports().remove(player.getUniqueId());
        }
    }
}