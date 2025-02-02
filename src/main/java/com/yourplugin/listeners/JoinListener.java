package com.yourplugin.listeners;

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
        PendingTeleport pending = plugin.pendingTeleports.get(player.getUniqueId());

        if (pending != null) {
            if (pending.hasExpired(plugin.requestTimeoutSeconds, plugin.gracePeriodSeconds)) {
                plugin.pendingTeleports.remove(player.getUniqueId());
                plugin.logger.warning(String.format("[BorderTeleport] Expired teleport request for %s - handling with %s action",
                        player.getName(), plugin.expireAction));

                plugin.teleportHandler.handleExpiredTeleport(player, pending);
                return;
            }

            // Attempt the teleport
            plugin.teleportHandler.attemptPendingTeleport(player, pending);
        }
    }
}