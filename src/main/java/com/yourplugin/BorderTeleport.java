package com.yourplugin;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

public class BorderTeleport extends JavaPlugin implements Listener, PluginMessageListener {
    private Logger logger;
    // Load the current server name from our plugin's config (not from Bukkit.getServer().getName())
    private String currentServerName;
    // This key identifies the region (under "regions" in config) that corresponds to the current server.
    private String currentRegionKey;
    private HashMap<UUID, Long> teleportCooldowns = new HashMap<>();
    // Teleport cooldown time (in ms) loaded from config.
    private long teleportCooldownMs;
    private int minX, maxX, minZ, maxZ;
    private int pushbackDistance;

    @Override
    public void onEnable() {
        this.logger = getLogger();
        saveDefaultConfig();

        // Load our configuration values and region boundaries.
        loadRegionBoundaries();

        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("border").setExecutor(new BorderCommand(this));
        // Register the BungeeCord plugin messaging channels.
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

        logger.info("[BorderTeleport] Plugin enabled on server: " + currentServerName + " (Region: " + currentRegionKey + ")");
    }

    /**
     * Reloads config values including region boundaries.
     * (Call this from your /border reload command.)
     */
    public void loadRegionBoundaries() {
        // Explicitly load the server name from our config.
        currentServerName = getConfig().getString("server-name", "unknown");
        teleportCooldownMs = getConfig().getLong("teleport-cooldown-ms", 30000);
        pushbackDistance = getConfig().getInt("offline-pushback-distance", 5);

        // Look for a region entry whose internal "server-name" matches our currentServerName.
        ConfigurationSection regionsSection = getConfig().getConfigurationSection("regions");
        if (regionsSection != null) {
            boolean found = false;
            for (String regionKey : regionsSection.getKeys(false)) {
                String regionServerName = getConfig().getString("regions." + regionKey + ".server-name", "");
                if (currentServerName.equalsIgnoreCase(regionServerName)) {
                    currentRegionKey = regionKey;
                    minX = getConfig().getInt("regions." + regionKey + ".min-x");
                    maxX = getConfig().getInt("regions." + regionKey + ".max-x");
                    minZ = getConfig().getInt("regions." + regionKey + ".min-z");
                    maxZ = getConfig().getInt("regions." + regionKey + ".max-z");
                    found = true;
                    break;
                }
            }
            if (!found) {
                logger.warning("[BorderTeleport] Region boundaries for server '" + currentServerName + "' not found in config!");
            }
        } else {
            logger.warning("[BorderTeleport] No regions defined in config!");
        }
    }

    /**
     * (Currently unused) This method would process plugin messages.
     * For direct testing we are bypassing status checks.
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // In this test version, we're not using any status-check messages.
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        double x = loc.getX();
        double z = loc.getZ();

        logger.info("[DEBUG] Player " + player.getName() + " X: " + x + " Z: " + z + " | Current Server: " + currentServerName);
        handleBorderCrossing(player, x, z);
    }

    private void handleBorderCrossing(Player player, double x, double z) {
        // Check east-west boundaries
        if (x >= maxX) {
            logger.info("[DEBUG] " + player.getName() + " crossed EAST border");
            attemptTeleport(player, getTargetServer("east"));
        }
        if (x <= minX) {
            logger.info("[DEBUG] " + player.getName() + " crossed WEST border");
            attemptTeleport(player, getTargetServer("west"));
        }
        // Check north-south boundaries
        if (z >= maxZ) {
            logger.info("[DEBUG] " + player.getName() + " crossed SOUTH border");
            attemptTeleport(player, getTargetServer("south"));
        }
        if (z <= minZ) {
            logger.info("[DEBUG] " + player.getName() + " crossed NORTH border");
            attemptTeleport(player, getTargetServer("north"));
        }
    }

    /**
     * Maps the current region and a direction (north, south, east, or west) to the neighboring region key.
     * (This example assumes you have four regions: northwest, northeast, southwest, and southeast.)
     */
    private String getTargetRegionKey(String direction) {
        if (currentRegionKey == null) return null;
        switch (currentRegionKey.toLowerCase()) {
            case "southwest":
                if (direction.equalsIgnoreCase("north")) return "northwest";
                if (direction.equalsIgnoreCase("east")) return "southeast";
                break;
            case "northwest":
                if (direction.equalsIgnoreCase("south")) return "southwest";
                if (direction.equalsIgnoreCase("east")) return "northeast";
                break;
            case "northeast":
                if (direction.equalsIgnoreCase("south")) return "southeast";
                if (direction.equalsIgnoreCase("west")) return "northwest";
                break;
            case "southeast":
                if (direction.equalsIgnoreCase("north")) return "northeast";
                if (direction.equalsIgnoreCase("west")) return "southwest";
                break;
        }
        return null;
    }

    /**
     * Retrieves the target server name for a given direction using the neighboring region's configuration.
     */
    private String getTargetServer(String direction) {
        String targetRegionKey = getTargetRegionKey(direction);
        if (targetRegionKey == null) return null;
        String targetServer = getConfig().getString("regions." + targetRegionKey + ".server-name", null);
        logger.info("[DEBUG] " + (targetServer != null ? targetServer : "null") + " is the target server for direction " + direction);
        return targetServer;
    }

    private void attemptTeleport(Player player, String server) {
        if (server == null) {
            logger.warning("[DEBUG] " + player.getName() + " crossed a border but no target server is defined.");
            return;
        }
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        if (teleportCooldowns.containsKey(playerId) && (currentTime - teleportCooldowns.get(playerId)) < teleportCooldownMs) {
            return;
        }
        teleportCooldowns.put(playerId, currentTime);
        // Instead of checking server status, directly teleport using our test method.
        testTeleport(player, server);
    }

    /**
     * Directly sends a "Connect" plugin message via BungeeCord to transfer the player.
     * This method bypasses any status check.
     */
    private void testTeleport(Player player, String server) {
        logger.info("[DEBUG] Directly attempting to send " + player.getName() + " to " + server + " server.");
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteArray);
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (IOException e) {
            logger.severe("[ERROR] Failed to write to plugin message: " + e.getMessage());
            return;
        }
        player.sendPluginMessage(this, "BungeeCord", byteArray.toByteArray());
        logger.info("[DEBUG] Direct Connect message sent successfully to " + server);
    }

    private void pushBackPlayer(Player player) {
        Location loc = player.getLocation();
        loc.setX(loc.getX() - pushbackDistance);
        player.teleport(loc);
    }
}