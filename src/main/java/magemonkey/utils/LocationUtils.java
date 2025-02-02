package magemonkey.utils;

import magemonkey.BorderTeleport;

public class LocationUtils {
    private final BorderTeleport plugin;

    public LocationUtils(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    public boolean isWithinBounds(int x, int z) {
        return x >= plugin.getMinX() && x <= plugin.getMaxX() && z >= plugin.getMinZ() && z <= plugin.getMaxZ();
    }

    public boolean isNearBorder(int x, int z) {
        return x <= plugin.getMinX() + plugin.getSafeZoneDistance() ||
                x >= plugin.getMaxX() - plugin.getSafeZoneDistance() ||
                z <= plugin.getMinZ() + plugin.getSafeZoneDistance() ||
                z >= plugin.getMaxZ() - plugin.getSafeZoneDistance();
    }

    public boolean isInSafeZone(double x, double z) {
        return x <= plugin.getMinX() + plugin.getSafeZoneDistance() ||
                x >= plugin.getMaxX() - plugin.getSafeZoneDistance() ||
                z <= plugin.getMinZ() + plugin.getSafeZoneDistance() ||
                z >= plugin.getMaxZ() - plugin.getSafeZoneDistance();
    }
}
