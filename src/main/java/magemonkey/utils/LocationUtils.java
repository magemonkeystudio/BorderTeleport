// LocationUtils.java
package magemonkey.utils;

import magemonkey.BorderTeleport;

public class LocationUtils {
    private final BorderTeleport plugin;

    public LocationUtils(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    public boolean isWithinBounds(int x, int z) {
        return x >= plugin.getMinX() && x <= plugin.getMaxX() &&
                z >= plugin.getMinZ() && z <= plugin.getMaxZ();
    }

}