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
        if (plugin.getCurrentRegionKey() == null) {
            return;
        }

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

        boolean crossedBorder = false;
        String direction = null;

        if (from.getX() >= plugin.getMinX() && x < plugin.getMinX()) {
            direction = "west";
            crossedBorder = true;
        } else if (from.getX() <= plugin.getMaxX() && x > plugin.getMaxX()) {
            direction = "east";
            crossedBorder = true;
        } else if (from.getZ() >= plugin.getMinZ() && z < plugin.getMinZ()) {
            direction = "north";
            crossedBorder = true;
        } else if (from.getZ() <= plugin.getMaxZ() && z > plugin.getMaxZ()) {
            direction = "south";
            crossedBorder = true;
        }

        if (crossedBorder && direction != null) {
            plugin.getLogger().info(player.getName() + " crossed " + direction.toUpperCase() + " border");
            plugin.getTeleportHandler().attemptTeleport(player, direction);
        }
    }
}