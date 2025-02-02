package magemonkey.handlers;

import magemonkey.BorderTeleport;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Logger;

public class ConfigurationHandler {
    private final BorderTeleport plugin;
    private final Logger logger;

    public ConfigurationHandler(BorderTeleport plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void loadConfiguration() {
        plugin.reloadConfig();

        plugin.currentServerName = plugin.getConfig().getString("server-name", "unknown");
        plugin.teleportCooldownMs = plugin.getConfig().getLong("teleport-cooldown-ms", 3000);
        plugin.pushbackDistance = plugin.getConfig().getInt("offline-pushback-distance", 5);
        plugin.safeZoneDistance = plugin.getConfig().getInt("safe-zone-distance", 2);
        plugin.notifyServerOffline = plugin.getConfig().getBoolean("notify-server-offline", true);

        plugin.requestTimeoutSeconds = plugin.getConfig().getLong("teleport.request-timeout", 30);
        plugin.gracePeriodSeconds = plugin.getConfig().getLong("teleport.grace-period", 15);
        plugin.maxRetries = plugin.getConfig().getInt("teleport.max-retries", 3);
        plugin.retryDelaySeconds = plugin.getConfig().getLong("teleport.retry-delay", 2);
        plugin.expireAction = plugin.getConfig().getString("teleport.expire-action", "SPAWN");

        String rawOfflineMessage = plugin.getConfig().getString("offline-message", "<red>This server is currently offline!");
        plugin.offlineMessage = MiniMessage.miniMessage().deserialize(rawOfflineMessage);

        loadRegionBoundaries();
    }

    public void loadRegionBoundaries() {
        ConfigurationSection regionsSection = plugin.getConfig().getConfigurationSection("regions");
        if (regionsSection == null) {
            logger.severe("[BorderTeleport] No regions defined in config!");
            return;
        }

        boolean found = false;
        for (String regionKey : regionsSection.getKeys(false)) {
            String regionServerName = plugin.getConfig().getString("regions." + regionKey + ".server-name");
            if (plugin.currentServerName.equalsIgnoreCase(regionServerName)) {
                plugin.currentRegionKey = regionKey;
                plugin.minX = plugin.getConfig().getInt("regions." + regionKey + ".min-x");
                plugin.maxX = plugin.getConfig().getInt("regions." + regionKey + ".max-x");
                plugin.minZ = plugin.getConfig().getInt("regions." + regionKey + ".min-z");
                plugin.maxZ = plugin.getConfig().getInt("regions." + regionKey + ".max-z");
                found = true;
                break;
            }
        }

        if (!found) {
            logger.severe("[BorderTeleport] No region configuration found for server: " + plugin.currentServerName);
        }
    }
}