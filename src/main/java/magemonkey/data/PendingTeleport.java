package magemonkey.data;

public class PendingTeleport {
    private final long requestTime;
    private final double x;
    private final double z;
    private final String targetServer;

    public PendingTeleport(double x, double z, String targetServer) {
        this.requestTime = System.currentTimeMillis();
        this.x = x;
        this.z = z;
        this.targetServer = targetServer;
    }

    public boolean hasExpired(long timeoutSeconds, long gracePeriodSeconds) {
        long currentTime = System.currentTimeMillis();
        long expirationTime = requestTime + (timeoutSeconds + gracePeriodSeconds) * 1000;
        return currentTime > expirationTime;
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }

    public String getTargetServer() {
        return targetServer;
    }
}
