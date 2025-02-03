// JoinListener.java
package magemonkey.listeners;

import magemonkey.BorderTeleport;
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
        plugin.getLogger().info("DEBUG: Player joined: " + player.getName());

        // Keep the delayed task to ensure player is fully loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                // Don't need loadAndTeleport since we're using plugin messaging now
                // The TeleportHandler will handle the coordinates when received
                plugin.getLogger().info("DEBUG: Player loaded: " + player.getName());
            }
        }.runTaskLater(plugin, 20L);
    }
}