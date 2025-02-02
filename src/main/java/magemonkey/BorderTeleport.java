package magemonkey;

import magemonkey.config.ConfigurationHandler;
import magemonkey.data.PendingTeleport;
import magemonkey.handlers.TeleportHandler;
import magemonkey.utils.LocationUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

public class BorderTeleport extends JavaPlugin {
    private static BorderTeleport instance;
    private ConfigurationHandler configHandler;
    private TeleportHandler teleportHandler;
    private LocationUtils locationUtils;
    private HashMap<UUID, PendingTeleport> pendingTeleports;
    private Logger logger;
    private String currentServerName;
    private String currentRegionKey;
    private int minX, maxX, minZ, maxZ;
    private int safeZoneDistance;
    private int pushbackDistance;
    private int requestTimeoutSeconds;
    private int gracePeriodSeconds;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        configHandler = new ConfigurationHandler(this);
        teleportHandler = new TeleportHandler(this);
        locationUtils = new LocationUtils(this);
        pendingTeleports = new HashMap<>();

        // Load configurations
        configHandler.loadConfig();
    }

    public static BorderTeleport getInstance() {
        return instance;
    }

    public ConfigurationHandler getConfigHandler() {
        return configHandler;
    }

    public TeleportHandler getTeleportHandler() {
        return teleportHandler;
    }

    public LocationUtils getLocationUtils() {
        return locationUtils;
    }

    public Logger getPluginLogger() {
        return logger;
    }

    public String getCurrentServerName() {
        return currentServerName;
    }

    public String getCurrentRegionKey() {
        return currentRegionKey;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getSafeZoneDistance() {
        return safeZoneDistance;
    }

    public int getPushbackDistance() {
        return pushbackDistance;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public int getGracePeriodSeconds() {
        return gracePeriodSeconds;
    }

    public HashMap<UUID, PendingTeleport> getPendingTeleports() {
        return pendingTeleports;
    }
}
