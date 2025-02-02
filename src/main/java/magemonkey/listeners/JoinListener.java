// JoinListener.java
package magemonkey.listeners;

import magemonkey.BorderTeleport;
import magemonkey.data.PendingTeleport;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private final BorderTeleport plugin;

    public JoinListener(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PendingTeleport pending = plugin.getPendingTeleports().get(player.getUniqueId());

        if (pending != null) {
            if (pending.hasExpired(plugin.getRequestTimeoutSeconds(), plugin.getGracePeriodSeconds())) {
                plugin.getLogger().info("Expired teleport request for player " + player.getName());
                plugin.getPendingTeleports().remove(player.getUniqueId());
            }
        }
    }

}