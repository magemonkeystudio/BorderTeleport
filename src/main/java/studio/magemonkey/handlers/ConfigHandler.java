package studio.magemonkey.handlers;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigHandler {
    private final JavaPlugin plugin;

    public ConfigHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Returns the current server's name from the config, defaults to "defaultServer" if missing.
    public String getCurrentServerName() {
        return plugin.getConfig().getString("server-name", "defaultServer");
    }

    // Returns the configuration section for the specified region.
    public ConfigurationSection getRegionSection(String regionKey) {
        return plugin.getConfig().getConfigurationSection("regions." + regionKey);
    }

    // Returns the configuration section for the current region.
    // Throws an IllegalStateException if the section is not found.
    public ConfigurationSection getCurrentRegionSection() {
        ConfigurationSection section = getRegionSection(getCurrentServerName());
        if (section == null) {
            throw new IllegalStateException("No region configuration found for server: " + getCurrentServerName());
        }
        return section;
    }

    // Given a location, returns the region key (e.g., "northwest", "southeast") that contains it.
    // Default values are provided if any keys are missing, avoiding a NullPointerException.
    public String getRegionForLocation(Location loc) {
        ConfigurationSection regionsSection = plugin.getConfig().getConfigurationSection("regions");
        if (regionsSection == null) return null;

        for (String key : regionsSection.getKeys(false)) {
            ConfigurationSection region = regionsSection.getConfigurationSection(key);
            if (region == null) continue; // Skip if region section is missing
            int minX = region.getInt("min-x", Integer.MIN_VALUE);
            int maxX = region.getInt("max-x", Integer.MAX_VALUE);
            int minZ = region.getInt("min-z", Integer.MIN_VALUE);
            int maxZ = region.getInt("max-z", Integer.MAX_VALUE);
            double x = loc.getX();
            double z = loc.getZ();
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                return key;
            }
        }
        return null;
    }
}
