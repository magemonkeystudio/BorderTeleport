package com.yourplugin.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import utils.LocationUtils;

public class MovementListener implements Listener {

    private final BorderTeleport plugin;

    public MovementListener(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location loc = event.getTo();
        double x = loc.getX();
        double z = loc.getZ();

        // Check if player is in safe zone
        if (LocationUtils.isInSafeZone(plugin, x, z)) {
            return;
        }

        plugin.teleportHandler.handleBorderCrossing(player, x, z);
    }
}