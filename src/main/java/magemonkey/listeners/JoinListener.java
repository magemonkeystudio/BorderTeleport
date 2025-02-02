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
        PendingTeleport pending = plugin.getPendingTeleports().get(player.getUniqueId().toString());

        if (pending != null) {
            if (System.currentTimeMillis() - pending.getTimestamp() > plugin.getRequestTimeoutSeconds() * 1000) {
                plugin.getPluginLogger().info("[BorderTeleport] Expired teleport request for player " + player.getName());
                plugin.getPendingTeleports().remove(player.getUniqueId().toString());
                plugin.getTeleportHandler().handleExpiredTeleport(player, pending);
            } else {
                plugin.getTeleportHandler().attemptPendingTeleport(player, pending);
            }
        }
    }
}
