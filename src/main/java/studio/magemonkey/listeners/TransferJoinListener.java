package studio.magemonkey.listeners;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.BorderTeleport;
import studio.magemonkey.database.MySQLManager;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

public class TransferJoinListener implements Listener {
    private final BorderTeleport plugin;
    private final MySQLManager mysql;
    private final Gson gson = new Gson();

    public TransferJoinListener(BorderTeleport plugin, MySQLManager mysql) {
        this.plugin = plugin;
        this.mysql = mysql;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Defer to next tick so the player is fully loaded
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Fetch data asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                MySQLManager.TransferData data = mysql.getTransferData(player.getUniqueId().toString());
                if (data != null) {
                    plugin.getLogger().info("[DEBUG][Async] Found transfer data for " + player.getName()
                            + " => x=" + data.x + ", y=" + data.y + ", z=" + data.z
                            + ", yaw=" + data.yaw + ", pitch=" + data.pitch
                            + ", direction=" + data.direction
                            + ", horseData=" + (data.horseData != null));

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline()) {
                            plugin.getLogger().info("[DEBUG] " + player.getName()
                                    + " has logged out before teleport, skipping.");
                            return;
                        }

                        // Teleport player
                        Location newLoc = player.getLocation().clone();
                        newLoc.setX(data.x);
                        newLoc.setY(data.y);
                        newLoc.setZ(data.z);
                        newLoc.setYaw(data.yaw);
                        newLoc.setPitch(data.pitch);

                        player.teleport(newLoc);

                        // If there's horse data, spawn a new horse
                        if (data.horseData != null && !data.horseData.isEmpty()) {
                            spawnHorseForPlayer(player, data.horseData);
                        }

                        // Delete from DB asynchronously
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            plugin.getLogger().info("[DEBUG][Async] Deleting transfer data for " + player.getName());
                            mysql.deleteTransferData(player.getUniqueId().toString());
                        });
                    });
                } else {
                    plugin.getLogger().info("[DEBUG][Async] No transfer data found for " + player.getName());
                }
            });
        });
    }

    private void spawnHorseForPlayer(Player player, String horseJson) {
        plugin.getLogger().info("[DEBUG] Re-spawning horse for " + player.getName()
                + " from data=" + horseJson);

        JsonObject obj = gson.fromJson(horseJson, JsonObject.class);
        if (obj == null) return;

        // Spawn horse
        Horse horse = (Horse) player.getWorld().spawnEntity(player.getLocation(), EntityType.HORSE);

        // Re-apply color, style
        if (obj.has("color")) {
            horse.setColor(Horse.Color.valueOf(obj.get("color").getAsString()));
        }
        if (obj.has("style")) {
            horse.setStyle(Horse.Style.valueOf(obj.get("style").getAsString()));
        }

        // Health
        if (obj.has("maxHealth")) {
            double maxHealth = obj.get("maxHealth").getAsDouble();
            if (horse.getAttribute(Attribute.MAX_HEALTH) != null) {
                horse.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
            }
        }
        if (obj.has("health")) {
            horse.setHealth(obj.get("health").getAsDouble());
        }

        // Jump Strength
        if (obj.has("jumpStrength")) {
            horse.setJumpStrength(obj.get("jumpStrength").getAsDouble());
        }

        // Tamed + Owner
        boolean isTamed = false;
        if (obj.has("tamed")) {
            isTamed = obj.get("tamed").getAsBoolean();
        }
        horse.setTamed(isTamed);

        if (obj.has("ownerUUID")) {
            String ownerStr = obj.get("ownerUUID").getAsString();
            if (!ownerStr.isEmpty()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(ownerStr));
                horse.setOwner(op);
            }
        }

        // Re-apply saddle & armor from JSON
        if (obj.has("saddle")) {
            String saddleJson = obj.get("saddle").getAsString();
            if (!saddleJson.isEmpty()) {
                ItemStack saddleItem = jsonToItemStack(saddleJson);
                if (saddleItem != null) {
                    horse.getInventory().setSaddle(saddleItem);
                }
            }
        }

        if (obj.has("armor")) {
            String armorJson = obj.get("armor").getAsString();
            if (!armorJson.isEmpty()) {
                ItemStack armorItem = jsonToItemStack(armorJson);
                if (armorItem != null) {
                    horse.getInventory().setArmor(armorItem);
                }
            }
        }

        // Delay mounting so the entity is fully spawned
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !horse.isDead()) {
                horse.addPassenger(player);
            }
        }, 10L);
    }

    // -----------------------------------------------------------------------
    // Use ConfigurationSerializable approach to restore an ItemStack from JSON
    // -----------------------------------------------------------------------
    private ItemStack jsonToItemStack(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Type typeToken = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> map = gson.fromJson(json, typeToken);
            // item deserialize
            return ItemStack.deserialize(map);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
