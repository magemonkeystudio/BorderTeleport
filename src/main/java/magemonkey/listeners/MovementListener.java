// MovementListener.java
package magemonkey.listeners;

import magemonkey.BorderTeleport;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementListener implements Listener {
    private final BorderTeleport plugin;

    public MovementListener(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        Player player = event.getPlayer();
        double x = to.getX();
        double z = to.getZ();

        plugin.getLogger().info(player.getName() + " movement: FROM(x=" + from.getX() +
                ", z=" + from.getZ() + ") TO(x=" + x + ", z=" + z + ")");

        if (x <= plugin.getMinX()) {
            plugin.getLogger().info(player.getName() + " crossed WEST border");
            plugin.getTeleportHandler().attemptTeleport(player, "west");
        } else if (x >= plugin.getMaxX()) {
            plugin.getLogger().info(player.getName() + " crossed EAST border");
            plugin.getTeleportHandler().attemptTeleport(player, "east");
        } else if (z <= plugin.getMinZ()) {
            plugin.getLogger().info(player.getName() + " crossed NORTH border");
            plugin.getTeleportHandler().attemptTeleport(player, "north");
        } else if (z >= plugin.getMaxZ()) {
            plugin.getLogger().info(player.getName() + " crossed SOUTH border");
            plugin.getTeleportHandler().attemptTeleport(player, "south");
        }
    }
}