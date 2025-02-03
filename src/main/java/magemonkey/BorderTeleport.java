// BorderTeleport.java
package magemonkey;

import magemonkey.handlers.ConfigurationHandler;
import magemonkey.data.PendingTeleport;
import magemonkey.handlers.TeleportHandler;
import magemonkey.utils.LocationUtils;
import magemonkey.listeners.MovementListener;
import magemonkey.listeners.JoinListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

public class BorderTeleport extends JavaPlugin {
    private static BorderTeleport instance;
    public ConfigurationHandler configHandler;
    public TeleportHandler teleportHandler;
    private LocationUtils locationUtils;
    private HashMap<UUID, PendingTeleport> pendingTeleports;
    private Logger logger;
    private String currentServerName;
    private String currentRegionKey;
    private int minX, maxX, minZ, maxZ;
    private int pushbackDistance;
    private int requestTimeoutSeconds;
    private int gracePeriodSeconds;
    private long teleportCooldownMs;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        configHandler = new ConfigurationHandler(this);
        teleportHandler = new TeleportHandler(this);
        locationUtils = new LocationUtils(this);
        pendingTeleports = new HashMap<>();

        saveDefaultConfig();
        reloadConfig();

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", teleportHandler);

        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        loadConfig();
        configHandler.loadConfiguration();

        logger.info("Plugin enabled on server: " + currentServerName + " (Region: " + currentRegionKey + ")");
        logger.info("Boundaries: X(" + minX + " to " + maxX + "), Z(" + minZ + " to " + maxZ + ")");
    }

    public void loadConfig() {
        currentServerName = getConfig().getString("server-name");
        teleportCooldownMs = getConfig().getLong("teleport-cooldown-ms", 3000);
        pushbackDistance = getConfig().getInt("offline-pushback-distance", 5);
        requestTimeoutSeconds = getConfig().getInt("teleport.request-timeout", 30);
        gracePeriodSeconds = getConfig().getInt("teleport.grace-period", 15);

        logger.info("Loaded server name: " + currentServerName);
    }

    // All getters
    public static BorderTeleport getInstance() { return instance; }
    public ConfigurationHandler getConfigHandler() { return configHandler; }
    public TeleportHandler getTeleportHandler() { return teleportHandler; }
    public LocationUtils getLocationUtils() { return locationUtils; }
    public Logger getPluginLogger() { return logger; }
    public String getCurrentServerName() { return currentServerName; }
    public String getCurrentRegionKey() { return currentRegionKey; }
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public int getPushbackDistance() { return pushbackDistance; }
    public int getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
    public int getGracePeriodSeconds() { return gracePeriodSeconds; }
    public long getTeleportCooldownMs() { return teleportCooldownMs; }
    public HashMap<UUID, PendingTeleport> getPendingTeleports() { return pendingTeleports; }

    // Setters
    public void setCurrentServerName(String name) {
        this.currentServerName = name;
        logger.info("Set current server name: " + name);
    }
    public void setCurrentRegionKey(String key) {
        this.currentRegionKey = key;
        logger.info("Set current region key: " + key);
    }
    public void setMinX(int minX) {
        this.minX = minX;
        logger.info("Set minX: " + minX);
    }
    public void setMaxX(int maxX) {
        this.maxX = maxX;
        logger.info("Set maxX: " + maxX);
    }
    public void setMinZ(int minZ) {
        this.minZ = minZ;
        logger.info("Set minZ: " + minZ);
    }
    public void setMaxZ(int maxZ) {
        this.maxZ = maxZ;
        logger.info("Set maxZ: " + maxZ);
    }
}