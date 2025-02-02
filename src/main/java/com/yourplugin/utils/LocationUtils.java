package com.yourplugin.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LocationUtils {

    public static boolean isInSafeZone(BorderTeleport plugin, double x, double z) {
        // Check if the player is within safeZoneDistance blocks of any border
        return (Math.abs(x - plugin.minX) < plugin.safeZoneDistance ||
                Math.abs(x - plugin.maxX) < plugin.safeZoneDistance ||
                Math.abs(z - plugin.minZ) < plugin.safeZoneDistance ||
                Math.abs(z - plugin.maxZ) < plugin.safeZoneDistance);
    }

    public static String formatLocation(Location loc) {
        return String.format("(%.2f, %.2f, %.2f)", loc.getX(), loc.getY(), loc.getZ());
    }

    public static String getTargetRegionKey(BorderTeleport plugin, String direction) {
        if (plugin.currentRegionKey == null) return null;

        return switch (plugin.currentRegionKey.toLowerCase()) {
            case "southwest" -> direction.equalsIgnoreCase("north") ? "northwest" :
                    direction.equalsIgnoreCase("east") ? "southeast" : null;
            case "northwest" -> direction.equalsIgnoreCase("south") ? "southwest" :
                    direction.equalsIgnoreCase("east") ? "northeast" : null;
            case "northeast" -> direction.equalsIgnoreCase("south") ? "southeast" :
                    direction.equalsIgnoreCase("west") ? "northwest" : null;
            case "southeast" -> direction.equalsIgnoreCase("north") ? "northeast" :
                    direction.equalsIgnoreCase("west") ? "southwest" : null;
            default -> null;
        };
    }

    public static String getTargetServer(BorderTeleport plugin, String direction) {
        String targetRegionKey = getTargetRegionKey(plugin, direction);
        if (targetRegionKey == null) return null;
        return plugin.getConfig().getString("regions." + targetRegionKey + ".server-name");
    }

    public static Location getSafeLocation(BorderTeleport plugin, Player player, Location currentLoc) {
        Location safeLoc = currentLoc.clone();

        // Determine which border was crossed and push back accordingly
        if (currentLoc.getX() >= plugin.maxX) {
            // Crossed east border - push west
            safeLoc.setX(plugin.maxX - plugin.pushbackDistance);
        } else if (currentLoc.getX() <= plugin.minX) {
            // Crossed west border - push east
            safeLoc.setX(plugin.minX + plugin.pushbackDistance);
        } else if (currentLoc.getZ() >= plugin.maxZ) {
            // Crossed south border - push north
            safeLoc.setZ(plugin.maxZ - plugin.pushbackDistance);
        } else if (currentLoc.getZ() <= plugin.minZ) {
            // Crossed north border - push south
            safeLoc.setZ(plugin.minZ + plugin.pushbackDistance);
        }


// Ensure the new location is safe
        safeLoc.setY(player.getWorld().getHighestBlockYAt(safeLoc.getBlockX(), safeLoc.getBlockZ()) + 1);

        return safeLoc;
    }
}