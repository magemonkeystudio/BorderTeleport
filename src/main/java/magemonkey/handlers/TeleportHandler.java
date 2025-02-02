package magemonkey.handlers;

// ... existing imports ...

public class TeleportHandler {
    // ... existing code ...

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
                        }.runTaskLater(plugin, 5L); // Short delay to ensure player is loaded
                    }

                    logger.info(String.format("[BorderTeleport] Successfully teleported %s to stored location: %s",
                            player.getName(), LocationUtils.formatLocation(targetLoc)));
                    plugin.pendingTeleports.remove(player.getUniqueId());
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
            case "REJECT" -> {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Your teleport request expired. Please try again."));
            }
            default -> {
                player.teleport(player.getWorld().getSpawnLocation());
                logger.warning("[BorderTeleport] Unknown expire-action: " + plugin.expireAction + ". Defaulting to SPAWN.");
            }
        }
    }

    // ... rest of the existing code ...
}