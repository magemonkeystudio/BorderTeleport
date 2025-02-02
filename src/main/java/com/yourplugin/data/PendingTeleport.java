package com.yourplugin.data;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class PendingTeleport {
    private final Location location;
    private final long timestamp;
    private int retryCount;
    private long lastRetryTime;
    private final MountData mountData;

    public PendingTeleport(Location location, Entity mount) {
        this.location = location;
        this.timestamp = System.currentTimeMillis();
        this.retryCount = 0;
        this.lastRetryTime = System.currentTimeMillis();
        this.mountData = mount != null ? new MountData(mount) : null;
    }

    public boolean hasExpired(long timeoutSeconds, long gracePeriodSeconds) {
        long totalTimeout = (timeoutSeconds + gracePeriodSeconds) * 1000;
        return System.currentTimeMillis() - timestamp > totalTimeout;
    }

    public boolean canRetry(long retryDelaySeconds, int maxRetries) {
        if (retryCount >= maxRetries) return false;
        return System.currentTimeMillis() - lastRetryTime >= retryDelaySeconds * 1000;
    }

    public void incrementRetry() {
        retryCount++;
        lastRetryTime = System.currentTimeMillis();
    }

    // Getters
    public Location getLocation() {
        return location;
    }

    public MountData getMountData() {
        return mountData;
    }

    public int getRetryCount() {
        return retryCount;
    }
}