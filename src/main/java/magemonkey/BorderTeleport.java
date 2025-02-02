package magemonkey;

import magemonkey.commands.BorderCommand;
import magemonkey.data.PendingTeleport;
import magemonkey.handlers.ConfigurationHandler;
import magemonkey.handlers.ServerStatusHandler;
import magemonkey.handlers.TeleportHandler;
import magemonkey.listeners.JoinListener;
import magemonkey.listeners.MovementListener;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
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

    public ConfigurationHandler configHandler;
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

        new BukkitRunnable() {
            @Override
            public void run() {
                statusHandler.checkServerStatuses();
            }
        }.runTaskTimer(this, 100L, 600L);

        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredTeleports();
            }
        }.runTaskTimer(this, 200L, 200L);

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

        if (getCommand("border") != null) {
            getCommand("border").setExecutor(new BorderCommand(this));
        }

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