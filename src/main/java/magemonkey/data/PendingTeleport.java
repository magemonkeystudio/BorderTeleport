// PendingTeleport.java
package magemonkey.data;

public class PendingTeleport {
    private final long timestamp;
    private final double x;
    private final double z;
    private final String targetServer;

    public PendingTeleport(double x, double z, String targetServer) {
        this.timestamp = System.currentTimeMillis();
        this.x = x;
        this.z = z;
        this.targetServer = targetServer;
    }

    public long getTimestamp() { return timestamp; }
    public double getX() { return x; }
    public double getZ() { return z; }
    public String getTargetServer() { return targetServer; }

    public boolean hasExpired(long timeoutSeconds, long gracePeriodSeconds) {
        long currentTime = System.currentTimeMillis();
        long expirationTime = timestamp + (timeoutSeconds + gracePeriodSeconds) * 1000;
        return currentTime > expirationTime;
    }

}