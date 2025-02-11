package studio.magemonkey.listeners;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import studio.magemonkey.BorderTeleport;
import studio.magemonkey.database.MySQLManager;
import studio.magemonkey.handlers.ConfigHandler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class BorderListener implements Listener {
    private final BorderTeleport plugin;
    private final MySQLManager mysql;

    private final int currentMinX;
    private final int currentMaxX;
    private final int currentMinZ;
    private final int currentMaxZ;
    private final String currentRegionKey;

    // For handling cooldown on repeated transfers
    private final Map<String, Long> pendingTransfers = new HashMap<>();
    private final int offset;

    private final Gson gson = new Gson();

    public BorderListener(BorderTeleport plugin, MySQLManager mysql) {
        this.plugin = plugin;
        this.mysql = mysql;

        this.currentRegionKey = ConfigHandler.getCurrentServerName();
        ConfigurationSection regionSection = ConfigHandler.getCurrentRegionSection();
        this.currentMinX = regionSection.getInt("min-x", Integer.MIN_VALUE);
        this.currentMaxX = regionSection.getInt("max-x", Integer.MAX_VALUE);
        this.currentMinZ = regionSection.getInt("min-z", Integer.MIN_VALUE);
        this.currentMaxZ = regionSection.getInt("max-z", Integer.MAX_VALUE);

        this.offset = plugin.getConfig().getInt("teleport.offset", 20);

        plugin.getLogger().info("[BorderListener] Initialized for region: " + currentRegionKey
                + " with boundaries: X[" + currentMinX + ", " + currentMaxX + "] Z[" + currentMinZ + ", " + currentMaxZ + "]");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        if (isWithinCurrentRegion(to)) {
            return;
        }

        // Identify which region the player is moving into
        String destinationRegionKey = ConfigHandler.getRegionForLocation(to);
        if (destinationRegionKey == null || destinationRegionKey.equalsIgnoreCase(currentRegionKey)) {
            return;
        }

        // Fetch the corresponding server
        ConfigurationSection destSection = plugin.getConfig().getConfigurationSection("regions." + destinationRegionKey);
        if (destSection == null) {
            plugin.getLogger().severe("[BorderListener] No config section for region: " + destinationRegionKey);
            return;
        }
        String destServer = destSection.getString("server-name");
        if (destServer == null) {
            plugin.getLogger().severe("[BorderListener] No server-name defined for region: " + destinationRegionKey);
            return;
        }

        // Check cooldown
        String playerId = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        int cooldownSeconds = ConfigHandler.getTeleportRequestTimeout();
        if (pendingTransfers.containsKey(playerId)) {
            long lastRequestTime = pendingTransfers.get(playerId);
            if (currentTime - lastRequestTime < cooldownSeconds * 1000L) {
                return;
            }
        }
        pendingTransfers.put(playerId, currentTime);

        // Calculate final coords (with offset)
        Location offsetLoc = to.clone();
        applyOffset(offsetLoc);

        float yaw = offsetLoc.getYaw();
        float pitch = offsetLoc.getPitch();
        String crossingDirection = getCrossingDirection(to);

        // Check if player is riding a horse
        String serializedHorseData = null;
        Entity vehicle = player.getVehicle();
        if (vehicle != null && vehicle instanceof Horse) {
            Horse horse = (Horse) vehicle;

            // Build a JSON object
            JsonObject obj = new JsonObject();
            obj.addProperty("color", horse.getColor().name());
            obj.addProperty("style", horse.getStyle().name());

            // Health
            obj.addProperty("health", horse.getHealth());
            double maxHealth = 20.0;
            if (horse.getAttribute(Attribute.MAX_HEALTH) != null) {
                maxHealth = horse.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            }
            obj.addProperty("maxHealth", maxHealth);

            // Jump Strength
            obj.addProperty("jumpStrength", horse.getJumpStrength());

            // Tamed + Owner
            obj.addProperty("tamed", horse.isTamed());
            if (horse.getOwner() != null) {
                obj.addProperty("ownerUUID", horse.getOwner().getUniqueId().toString());
            }

            // Serialize saddle & armor
            ItemStack saddle = horse.getInventory().getSaddle();
            if (saddle != null) {
                obj.addProperty("saddle", itemStackToJson(saddle));
            } else {
                obj.addProperty("saddle", "");
            }

            ItemStack armor = horse.getInventory().getArmor();
            if (armor != null) {
                obj.addProperty("armor", itemStackToJson(armor));
            } else {
                obj.addProperty("armor", "");
            }

            // Convert to string
            serializedHorseData = gson.toJson(obj);

            // Remove horse from world
            horse.remove();
        }

        // Save data asynchronously, then connect player
        String finalHorseData = serializedHorseData;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            mysql.savePlayerTransfer(
                    player.getUniqueId().toString(),
                    destServer,
                    offsetLoc.getBlockX(),
                    offsetLoc.getBlockY(),
                    offsetLoc.getBlockZ(),
                    crossingDirection,
                    yaw,
                    pitch,
                    finalHorseData
            );

            // Switch back to main thread to connect them
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    sendPlayerToServer(player, destServer);
                }
            });
        });
    }

    private void applyOffset(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        if (x < currentMinX) {
            loc.setX(x - offset);
        } else if (x > currentMaxX) {
            loc.setX(x + offset);
        } else if (z < currentMinZ) {
            loc.setZ(z - offset);
        } else if (z > currentMaxZ) {
            loc.setZ(z + offset);
        }
    }

    private boolean isWithinCurrentRegion(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return (x >= currentMinX && x <= currentMaxX && z >= currentMinZ && z <= currentMaxZ);
    }

    private String getCrossingDirection(Location loc) {
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
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("Error sending plugin message: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Use ConfigurationSerializable approach to store an ItemStack as JSON
    // -----------------------------------------------------------------------
    private String itemStackToJson(ItemStack item) {
        if (item == null) {
            return "";
        }
        try {
            // item.serialize() => Map<String, Object>
            Map<String, Object> serialized = item.serialize();
            return gson.toJson(serialized);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
