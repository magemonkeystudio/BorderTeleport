package studio.magemonkey.listeners;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import studio.magemonkey.BorderTeleport;
import studio.magemonkey.database.MySQLManager;
import studio.magemonkey.handlers.ConfigHandler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BorderListener implements Listener {
    private final BorderTeleport plugin;
    private final MySQLManager mysql;
    private final ConfigHandler configHandler;

    // Current region boundaries.
    private final int currentMinX;
    private final int currentMaxX;
    private final int currentMinZ;
    private final int currentMaxZ;
    private final String currentRegionKey; // e.g., "southwest"

    public BorderListener(BorderTeleport plugin, MySQLManager mysql) {
        this.plugin = plugin;
        this.mysql = mysql;
        this.configHandler = plugin.getConfigHandler();

        // Get current region key and boundaries from config.
        this.currentRegionKey = configHandler.getCurrentServerName();
        ConfigurationSection regionSection = configHandler.getCurrentRegionSection();
        if (regionSection == null) {
            plugin.getLogger().severe("Region configuration for server " + currentRegionKey + " is missing!");
            throw new IllegalStateException("Region configuration missing for " + currentRegionKey);
        }
        // Use default values to avoid NPE if keys are missing.
        this.currentMinX = regionSection.getInt("min-x", Integer.MIN_VALUE);
        this.currentMaxX = regionSection.getInt("max-x", Integer.MAX_VALUE);
        this.currentMinZ = regionSection.getInt("min-z", Integer.MIN_VALUE);
        this.currentMaxZ = regionSection.getInt("max-z", Integer.MAX_VALUE);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        // 'to' is assumed to be non-null by the API.

        // If the player is still within the current region, do nothing.
        if (isWithinRegion(to)) {
            return;
        }

        // Determine destination region based on the player's new location.
        String destinationRegionKey = configHandler.getRegionForLocation(to);
        if (destinationRegionKey == null || destinationRegionKey.equalsIgnoreCase(currentRegionKey)) {
            return;
        }
        // Get the destination server name from the region configuration.
        ConfigurationSection destSection = plugin.getConfig().getConfigurationSection("regions." + destinationRegionKey);
        if (destSection == null) {
            plugin.getLogger().severe("No configuration section for region: " + destinationRegionKey);
            return;
        }
        String destServer = destSection.getString("server-name");
        if (destServer == null) {
            plugin.getLogger().severe("No server-name defined for region: " + destinationRegionKey);
            return;
        }

        // Save the transfer data to MySQL.
        try {
            mysql.savePlayerTransfer(
                    player.getUniqueId().toString(),
                    destServer,
                    to.getBlockX(),
                    to.getBlockY(),
                    to.getBlockZ(),
                    getTransferDirection(to)
            );
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving player transfer data: " + e.getMessage());
        }

        // Send a plugin message to BungeeCord to transfer the player.
        sendPlayerToServer(player, destServer);
    }

    private boolean isWithinRegion(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return (x >= currentMinX && x <= currentMaxX && z >= currentMinZ && z <= currentMaxZ);
    }

    private String getTransferDirection(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        if (x < currentMinX) {
            return "WEST";
        } else if (x > currentMaxX) {
            return "EAST";
        } else if (z < currentMinZ) {
            return "NORTH";
        } else if (z > currentMaxZ) {
            return "SOUTH";
        }
        return "UNKNOWN";
    }

    private void sendPlayerToServer(Player player, String server) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(outStream);
            dataOut.writeUTF("Connect");
            dataOut.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", outStream.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("Error sending plugin message: " + e.getMessage());
        }
    }
}
