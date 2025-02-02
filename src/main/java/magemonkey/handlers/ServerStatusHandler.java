package magemonkey.handlers;

import magemonkey.BorderTeleport;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Logger;

public class ServerStatusHandler {
    private final BorderTeleport plugin;
    private final Logger logger;

    public ServerStatusHandler(BorderTeleport plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void checkServerStatuses() {
        // Log current server information
        logger.info("[BorderTeleport] Current server name: " + plugin.currentServerName);
        logger.info("[BorderTeleport] Current region key: " + plugin.currentRegionKey);
        logger.info("[BorderTeleport] Boundaries: X(" + plugin.minX + " to " + plugin.maxX +
                "), Z(" + plugin.minZ + " to " + plugin.maxZ + ")");

        ConfigurationSection regions = plugin.getConfig().getConfigurationSection("regions");
        if (regions == null) {
            logger.severe("[BorderTeleport] No regions defined in config!");
            return;
        }

        // Log all configured regions
        logger.info("[BorderTeleport] Configured regions:");
        for (String regionKey : regions.getKeys(false)) {
            String serverName = regions.getString(regionKey + ".server-name");
            if (serverName != null) {
                int minX = regions.getInt(regionKey + ".min-x");
                int maxX = regions.getInt(regionKey + ".max-x");
                int minZ = regions.getInt(regionKey + ".min-z");
                int maxZ = regions.getInt(regionKey + ".max-z");
                logger.info(String.format("[BorderTeleport] Region %s: server=%s, X(%d to %d), Z(%d to %d)",
                        regionKey, serverName, minX, maxX, minZ, maxZ));

                // Set server status if it's not our server
                if (!serverName.equals(plugin.currentServerName)) {
                    plugin.serverStatus.put(serverName, true);
                    logger.info("[BorderTeleport] Setting server " + serverName + " as online");
                }
            }
        }
    }
}