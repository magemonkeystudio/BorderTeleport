package com.yourplugin.handlers;

import data.MountData;
import data.PendingTeleport;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import utils.LocationUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

public class TeleportHandler {

    private final BorderTeleport plugin;
    private Logger logger;

    public TeleportHandler(BorderTeleport plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void handleBorderCrossing(Player player, double x, double z) {
        String direction = null;

        if (x >= plugin.maxX) direction = "east";
        else if (x <= plugin.minX) direction = "west";
        else if (z >= plugin.maxZ) direction = "south";
        else if (z <= plugin.minZ) direction = "north";

        if (direction != null) {
            String targetServer = LocationUtils.getTargetServer(plugin, direction);
            if (targetServer != null) {
                attemptTeleport(player, targetServer, x, z);
            }
        }
    }

    public void attemptTeleport(Player player, String targetServer, double x, double z) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check cooldown
        if (plugin.teleportCooldowns.containsKey(playerId)) {
            long lastTeleport = plugin.teleportCooldowns.get(playerId);
            if (currentTime - lastTeleport < plugin.teleportCooldownMs) {
                return;
            }
        }

        // Check server status
        if (!plugin.serverStatus.getOrDefault(targetServer, false)) {
            if (plugin.notifyServerOffline) {
                player.sendMessage(plugin.offlineMessage);
            }
            pushBackPlayer(player);
            return;
        }

        // Update cooldown and attempt teleport
        plugin.teleportCooldowns.put(playerId, currentTime);
        sendPlayerToServer(player, targetServer);
    }

    public void sendPlayerToServer(Player player, String server) {
        Location loc = player.getLocation();

        Entity mount = player.getVehicle();

        // First, send the player's location data through a custom plugin channel
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF("Forward");
            out.writeUTF(server);
            out.writeUTF("BorderTeleport");

            // Write location data to a separate byte array
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgOut = new DataOutputStream(msgBytes);
            msgOut.writeUTF(player.getUniqueId().toString());
            msgOut.writeDouble(loc.getX());
            msgOut.writeDouble(loc.getY());
            msgOut.writeDouble(loc.getZ());
            msgOut.writeFloat(loc.getYaw());
            msgOut.writeFloat(loc.getPitch());
            msgOut.writeUTF(loc.getWorld().getName());

            // Write mount data if present
            msgOut.writeBoolean(mount != null);
            if (mount != null) {
                msgOut.writeUTF(mount.getType().name());
                msgOut.writeUTF(mount.getCustomName() != null ? mount.getCustomName() : "");
                msgOut.writeDouble(mount instanceof LivingEntity ? ((LivingEntity) mount).getHealth() : 0);
                writeAttributes(msgOut, mount);
                msgOut.writeBoolean(mount instanceof Tameable);
                if (mount instanceof Tameable) {
                    Tameable tameable = (Tameable) mount;
                    msgOut.writeBoolean(tameable.getOwner() != null);
                    if (tameable.getOwner() != null) {
                        msgOut.writeUTF(tameable.getOwner().getUniqueId().toString());
                    }
                }
            }

            // Write the location data message
            byte[] msgByteArray = msgBytes.toByteArray();
            out.writeShort(msgByteArray.length);
            out.write(msgByteArray);

            player.sendPluginMessage(this, "BungeeCord", b.toByteArray());
        } catch (IOException e) {
            logger.severe(String.format("[BorderTeleport] Failed to send location data for %s: %s",
                    player.getName(), e.getMessage()));
        }

        // If there's a mount, remove it after sending the data
        if (mount != null) {
            mount.remove();
        }

        // Then, send the actual connect request
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(this, "BungeeCord", b.toByteArray());

            logger.info(String.format("[BorderTeleport] Sending %s to server %s at location: %s",
                    player.getName(), server, LocationUtils.formatLocation(loc)));
        } catch (IOException e) {
            logger.severe(String.format("[BorderTeleport] Failed to send %s to server %s: %s",
                    player.getName(), server, e.getMessage()));
        }
    }

    private void writeAttributes(DataOutputStream out, Entity entity) throws IOException {
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;

            Map<String, Double> attributes = new HashMap<>();
            for (Attribute attribute : Attribute.values()) {
                AttributeInstance instance = living.getAttribute(attribute);
                if (instance != null) {
                    attributes.put(attribute.name(), instance.getBaseValue());
                }
            }

            out.writeInt(attributes.size());
            for (Map.Entry<String, Double> entry : attributes.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeDouble(entry.getValue());
            }
        } else {
            out.writeInt(0);
        }
    }

    public void pushBackPlayer(Player player) {
        Location currentLoc = player.getLocation();
        Location safeLoc = LocationUtils.getSafeLocation(plugin, player, currentLoc);

        // Log the pushback for debugging
        logger.info(String.format("[BorderTeleport] Pushing back player %s from %s to %s",
                player.getName(),
                LocationUtils.formatLocation(currentLoc),
                LocationUtils.formatLocation(safeLoc)));

        // Teleport the player to the safe location
        player.teleport(safeLoc);
    }

    public void attemptPendingTeleport(Player player, PendingTeleport pending) {
        if (!pending.canRetry(plugin.retryDelaySeconds, plugin.maxRetries)) {
            logger.warning(String.format("[BorderTeleport] Max retries reached for %s - handling with %s action",
                    player.getName(), plugin.expireAction));
            handleExpiredTeleport(player, pending);
            plugin.pendingTeleports.remove(player.getUniqueId());
            return;
        }

        // Increment retry count
        pending.incrementRetry();

        // Attempt the teleport
        new BukkitRunnable() {
            @Override
            public void run() {
                Location targetLoc = pending.getLocation();
                // Ensure the target location is safe
                targetLoc.setY(targetLoc.getWorld().getHighestBlockYAt(
                        targetLoc.getBlockX(), targetLoc.getBlockZ()) + 1);

                if (player.teleport(targetLoc)) {
                    // If mount data exists, spawn and mount the entity
                    if (pending.getMountData() != null) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Entity mount = pending.getMountData().recreateMount(targetLoc.getWorld(), targetLoc);
                                if (mount != null) {
                                    mount.addPassenger(player);
                                    logger.info(String.format("[BorderTeleport] Recreated mount for %s", player.getName()));
                                }
                            }
                        }.runTaskLater(BorderTeleport.this, 5L); // Short delay to ensure player is loaded
                    }

                    logger.info(String.format("[BorderTeleport] Successfully teleported %s to stored location: %s",
                            player.getName(), formatLocation(targetLoc)));
                    pendingTeleports.remove(player.getUniqueId());
                } else {
                    logger.warning(String.format("[BorderTeleport] Failed teleport attempt %d/%d for %s",
                            pending.getRetryCount(), plugin.maxRetries, player.getName()));

                    // Schedule next retry if we haven't hit the max
                    if (pending.canRetry(plugin.retryDelaySeconds, plugin.maxRetries)) {
                        attemptPendingTeleport(player, pending);
                    }
                }
            }
        }.runTaskLater(plugin, 5L);
    }

    public void handleExpiredTeleport(Player player, PendingTeleport pending) {
        switch (plugin.expireAction.toUpperCase()) {
            case "SPAWN":
                player.teleport(player.getWorld().getSpawnLocation());
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Your teleport request expired. You have been sent to spawn."));
                break;

            case "PREVIOUS_LOCATION":
                player.teleport(pending.location);
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Your teleport request expired. You have been sent back."));
                break;

            case "REJECT":
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Your teleport request expired. Please try again."));
                break;

            default:
                player.teleport(player.getWorld().getSpawnLocation());
                logger.warning("[BorderTeleport] Unknown expire-action: " + expireAction + ". Defaulting to SPAWN.");
                break;
        }
    }
}