package studio.magemonkey.borderteleport.handlers;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigHandler {
    private final JavaPlugin plugin;

    public ConfigHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Returns the current server's name from the config.
    public String getCurrentServerName() {
        return plugin.getConfig().getString("server-name");
    }

    // Returns the configuration section for the specified region.
    public ConfigurationSection getRegionSection(String regionKey) {
        return plugin.getConfig().getConfigurationSection("regions." + regionKey);
    }

    // Returns the configuration section for the current region.
    public ConfigurationSection getCurrentRegionSection() {
        return getRegionSection(getCurrentServerName());
    }

    // Returns the safe zone distance (in blocks) from the border.
    public int getSafeZoneDistance() {
        return plugin.getConfig().getInt("safe-zone-distance", 2);
    }

    // Returns the teleport cooldown in milliseconds.
    public int getTeleportCooldownMs() {
        return plugin.getConfig().getInt("teleport-cooldown-ms", 3000);
    }

    // Returns the offline server message.
    public String getOfflineServerMessage() {
        return plugin.getConfig().getString("server-offline-message", "&cThe path ahead is closed. The server is offline.");
    }

    // Returns whether to notify players when the destination server is offline.
    public boolean shouldNotifyServerOffline() {
        return plugin.getConfig().getBoolean("notify-server-offline", true);
    }

    // Returns the offline pushback distance (in blocks).
    public int getOfflinePushbackDistance() {
        return plugin.getConfig().getInt("offline-pushback-distance", 5);
    }

    // Given a location, returns the region key (e.g., "northwest", "southeast") that contains it.
    public String getRegionForLocation(Location loc) {
        ConfigurationSection regionsSection = plugin.getConfig().getConfigurationSection("regions");
        if (regionsSection == null) return null;

        for (String key : regionsSection.getKeys(false)) {
            ConfigurationSection region = regionsSection.getConfigurationSection(key);
            int minX = region.getInt("min-x");
            int maxX = region.getInt("max-x");
            int minZ = region.getInt("min-z");
            int maxZ = region.getInt("max-z");
            double x = loc.getX();
            double z = loc.getZ();
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                return key;
            }
        }
        return null;
    }
}
