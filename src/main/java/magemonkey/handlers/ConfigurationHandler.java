// ConfigurationHandler.java
package magemonkey.handlers;

import magemonkey.BorderTeleport;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.logging.Logger;

public class ConfigurationHandler {
    private final BorderTeleport plugin;
    private final Logger logger;

    public ConfigurationHandler(BorderTeleport plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void loadConfiguration() {
        String currentServerName = plugin.getCurrentServerName();
        logger.info("Loading configuration for server: " + currentServerName);

        ConfigurationSection regionsSection = plugin.getConfig().getConfigurationSection("regions");
        if (regionsSection == null) {
            logger.severe("No regions section found in config!");
            return;
        }

        logger.info("Configured regions:");
        for (String regionKey : regionsSection.getKeys(false)) {
            String regionServerName = plugin.getConfig().getString("regions." + regionKey + ".server-name");
            int minX = plugin.getConfig().getInt("regions." + regionKey + ".min-x");
            int maxX = plugin.getConfig().getInt("regions." + regionKey + ".max-x");
            int minZ = plugin.getConfig().getInt("regions." + regionKey + ".min-z");
            int maxZ = plugin.getConfig().getInt("regions." + regionKey + ".max-z");

            logger.info("Region " + regionKey + ": server=" + regionServerName +
                    ", X(" + minX + " to " + maxX + "), " +
                    "Z(" + minZ + " to " + maxZ + ")");

            if (currentServerName.equalsIgnoreCase(regionServerName)) {
                plugin.setCurrentRegionKey(regionKey);
                plugin.setMinX(minX);
                plugin.setMaxX(maxX);
                plugin.setMinZ(minZ);
                plugin.setMaxZ(maxZ);
                logger.info("Found matching region: " + regionKey);
            }
        }

        if (plugin.getCurrentRegionKey() == null) {
            logger.severe("No matching region found for server: " + currentServerName);
        }
    }

    public String getNeighboringServer(String currentRegion, String direction) {
        if (currentRegion == null) {
            logger.severe("Cannot get neighboring server: current region is null");
            return null;
        }

        String targetRegion = null;
        switch (currentRegion.toLowerCase()) {
            case "southwest":
                if (direction.equals("north")) targetRegion = "northwest";
                else if (direction.equals("east")) targetRegion = "southeast";
                break;
            case "northwest":
                if (direction.equals("south")) targetRegion = "southwest";
                else if (direction.equals("east")) targetRegion = "northeast";
                break;
            case "northeast":
                if (direction.equals("south")) targetRegion = "southeast";
                else if (direction.equals("west")) targetRegion = "northwest";
                break;
            case "southeast":
                if (direction.equals("north")) targetRegion = "northeast";
                else if (direction.equals("west")) targetRegion = "southwest";
                break;
        }

        if (targetRegion != null) {
            String serverName = plugin.getConfig().getString("regions." + targetRegion + ".server-name");
            logger.info("Found neighboring server " + serverName + " in direction " + direction);
            return serverName;
        }

        logger.warning("No neighboring server found in direction " + direction);
        return null;
    }
}