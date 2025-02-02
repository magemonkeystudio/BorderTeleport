import commands.BorderCommand;
import handlers.ConfigurationHandler;
import handlers.ServerStatusHandler;
import listeners.JoinListener;
import listeners.MovementListener;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class BorderTeleport extends JavaPlugin implements PluginMessageListener {
    public Logger logger;
    public String currentServerName;
    public String currentRegionKey;
    public final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    public final Map<String, Boolean> serverStatus = new HashMap<>();
    public long teleportCooldownMs;
    public int minX, maxX, minZ, maxZ;
    public int pushbackDistance;
    public int safeZoneDistance;
    public boolean notifyServerOffline;
    public Component offlineMessage;
    public long requestTimeoutSeconds;
    public long gracePeriodSeconds;
    public int maxRetries;
    public long retryDelaySeconds;
    public String expireAction;
    public final Map<UUID, PendingTeleport> pendingTeleports = new HashMap<>();

    private ConfigurationHandler configHandler;
    private ServerStatusHandler statusHandler;
    public TeleportHandler teleportHandler;

    @Override
    public void onEnable() {
        this.logger = getLogger();
        saveDefaultConfig();

        configHandler = new ConfigurationHandler(this);
        statusHandler = new ServerStatusHandler(this);
        teleportHandler = new TeleportHandler(this);

        configHandler.loadConfiguration();
        setupChannels();

        // Start server status check task
        new BukkitRunnable() {
            @Override
            public void run() {
                statusHandler.checkServerStatuses();
            }
        }.runTaskTimer(this, 100L, 600L); // Check every 30 seconds

        // Start pending teleports cleanup task
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredTeleports();
            }
        }.runTaskTimer(this, 200L, 200L); // Check every 10 seconds

        logger.info("[BorderTeleport] Plugin enabled on server: " + currentServerName + " (Region: " + currentRegionKey + ")");
    }

    private void cleanupExpiredTeleports() {
        Iterator<Map.Entry<UUID, PendingTeleport>> iterator = pendingTeleports.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingTeleport> entry = iterator.next();
            if (entry.getValue().hasExpired(requestTimeoutSeconds, gracePeriodSeconds)) {
                logger.info("[BorderTeleport] Removing expired teleport request for player UUID: " + entry.getKey());
                iterator.remove();
            }
        }
    }

    private void setupChannels() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        getCommand("border").setExecutor(new BorderCommand(this));
        Bukkit.getPluginManager().registerEvents(new MovementListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals("BungeeCord")) {
            teleportHandler.onPluginMessageReceived(player, message);
        }
    }
}