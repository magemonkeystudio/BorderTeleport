package magemonkey.handlers;

import magemonkey.BorderTeleport;
import magemonkey.data.PendingTeleport;
import magemonkey.utils.LocationUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

public class TeleportHandler {
    private final BorderTeleport plugin;
    private final Logger logger;

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
                attemptTeleport(player, targetServer);
            }
        }
    }

    public void attemptTeleport(Player player, String targetServer) {
        if (!plugin.serverStatus.getOrDefault(targetServer, false)) {
            if (plugin.notifyServerOffline) {
                player.sendMessage(plugin.offlineMessage);
            }
            Location safeLoc = LocationUtils.getSafeLocation(plugin, player, player.getLocation());
            player.teleport(safeLoc);
            return;
        }

        sendPlayerToServer(player, targetServer);
    }

    private void sendPlayerToServer(Player player, String server) {
        Location loc = player.getLocation();
        Entity mount = player.getVehicle();

        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF("Forward");
            out.writeUTF(server);
            out.writeUTF("BorderTeleport");

            // Write location data
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
                Component mountName = mount.customName();
                msgOut.writeUTF(mountName != null ? mountName.toString() : "");
                msgOut.writeDouble(mount instanceof LivingEntity living ? living.getHealth() : 0);
            }

            // Send the data
            byte[] msgByteArray = msgBytes.toByteArray();
            out.writeShort(msgByteArray.length);
            out.write(msgByteArray);

            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());

            // Remove mount after data is sent
            if (mount != null) {
                mount.remove();
            }

        } catch (IOException e) {
            logger.severe(String.format("[BorderTeleport] Failed to send location data for %s: %s",
                    player.getName(), e.getMessage()));
            return;
        }

        // Send server transfer request
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());

            logger.info(String.format("[BorderTeleport] Sending %s to server %s at location: %s",
                    player.getName(), server, LocationUtils.formatLocation(loc)));
        } catch (IOException e) {
            logger.severe(String.format("[BorderTeleport] Failed to transfer %s to server %s: %s",
                    player.getName(), server, e.getMessage()));
        }
    }

    public void onPluginMessageReceived(Player player, byte[] message) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(message);
             DataInputStream dataIn = new DataInputStream(in)) {

            String channel = dataIn.readUTF();
            if (!"BorderTeleport".equals(channel)) {
                return;
            }

            String playerUUID = dataIn.readUTF();
            double x = dataIn.readDouble();
            double y = dataIn.readDouble();
            double z = dataIn.readDouble();
            float yaw = dataIn.readFloat();
            float pitch = dataIn.readFloat();

            // Adjust coordinates based on border crossing
            if (x >= plugin.maxX - 1) {
                x = plugin.minX + 10;
            } else if (x <= plugin.minX + 1) {
                x = plugin.maxX - 10;
            } else if (z >= plugin.maxZ - 1) {
                z = plugin.minZ + 10;
            } else if (z <= plugin.minZ + 1) {
                z = plugin.maxZ - 10;
            }

            World world = player.getWorld();
            Location targetLoc = new Location(world, x, y, z, yaw, pitch);
            UUID uuid = UUID.fromString(playerUUID);

            // Handle mount data
            Entity mount = null;
            if (dataIn.readBoolean()) {
                mount = player.getVehicle();
            }

            plugin.pendingTeleports.put(uuid, new PendingTeleport(targetLoc, mount));
            logger.info(String.format("[BorderTeleport] Stored teleport location for %s: %s",
                    playerUUID, LocationUtils.formatLocation(targetLoc)));

        } catch (IOException e) {
            logger.severe("[BorderTeleport] Error processing plugin message: " + e.getMessage());
        }
    }

    public void attemptPendingTeleport(Player player, PendingTeleport pending) {
        if (!pending.canRetry(plugin.retryDelaySeconds, plugin.maxRetries)) {
            logger.warning(String.format("[BorderTeleport] Max retries reached for %s - handling with %s action",
                    player.getName(), plugin.expireAction));
            handleExpiredTeleport(player, pending);
            plugin.pendingTeleports.remove(player.getUniqueId());
            return;
        }

        pending.incrementRetry();

        new BukkitRunnable() {
            @Override
            public void run() {
                Location targetLoc = pending.getLocation();
                targetLoc.setY(targetLoc.getWorld().getHighestBlockYAt(
                        targetLoc.getBlockX(), targetLoc.getBlockZ()) + 1);

                if (player.teleport(targetLoc)) {
                    if (pending.getMountData() != null) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Entity mount = pending.getMountData().recreateMount(targetLoc.getWorld(), targetLoc);
                                if (mount != null) {
                                    mount.addPassenger(player);
                                    logger.info("[BorderTeleport] Recreated mount for " + player.getName());
                                }
                            }
                        }.runTaskLater(plugin, 5L);
                    }

                    logger.info(String.format("[BorderTeleport] Successfully teleported %s to: %s",
                            player.getName(), LocationUtils.formatLocation(targetLoc)));
                    plugin.pendingTeleports.remove(player.getUniqueId());
                } else {
                    logger.warning(String.format("[BorderTeleport] Failed teleport attempt %d/%d for %s",
                            pending.getRetryCount(), plugin.maxRetries, player.getName()));

                    if (pending.canRetry(plugin.retryDelaySeconds, plugin.maxRetries)) {
                        attemptPendingTeleport(player, pending);
                    }
                }
            }
        }.runTaskLater(plugin, 5L);
    }

    public void handleExpiredTeleport(Player player, PendingTeleport pending) {
        switch (plugin.expireAction.toUpperCase()) {
            case "SPAWN" -> {
                player.teleport(player.getWorld().getSpawnLocation());
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Your teleport request expired. You have been sent to spawn."));
            }
            case "PREVIOUS_LOCATION" -> {
                player.teleport(pending.getLocation());
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Your teleport request expired. You have been sent back."));
            }
            case "REJECT" -> player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Your teleport request expired. Please try again."));
            default -> {
                player.teleport(player.getWorld().getSpawnLocation());
                logger.warning("[BorderTeleport] Unknown expire-action: " + plugin.expireAction + ". Defaulting to SPAWN.");
            }
        }
    }
}