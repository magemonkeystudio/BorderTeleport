package studio.magemonkey.handlers;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import studio.magemonkey.BorderTeleport;

public class ConfigHandler {

    private ConfigHandler() { }

    private static FileConfiguration getConfig() {
        return BorderTeleport.getInstance().getConfig();
    }

    // === MySQL Settings ===

    public static String getMySQLHost() {
        return getConfig().getString("mysql.host", "your_database_host");
    }

    public static int getMySQLPort() {
        return getConfig().getInt("mysql.port", 3306);
    }

    public static String getMySQLDatabase() {
        return getConfig().getString("mysql.database", "your_database_name");
    }

    public static String getMySQLUsername() {
        return getConfig().getString("mysql.username", "your_username");
    }

    public static String getMySQLPassword() {
        return getConfig().getString("mysql.password", "your_password");
    }

    public static boolean useSSL() {
        return getConfig().getBoolean("mysql.useSSL", false);
    }

    public static boolean allowPublicKeyRetrieval() {
        return getConfig().getBoolean("mysql.allowPublicKeyRetrieval", true);
    }

    // === Region Configuration ===

    public static String getCurrentServerName() {
        return getConfig().getString("server-name", "defaultServer");
    }

    public static ConfigurationSection getCurrentRegionSection() {
        ConfigurationSection regionsSection = getConfig().getConfigurationSection("regions");
        if (regionsSection == null) {
            throw new IllegalStateException("No regions configuration found!");
        }
        String currentServer = getCurrentServerName();
        for (String key : regionsSection.getKeys(false)) {
            ConfigurationSection region = regionsSection.getConfigurationSection(key);
            if (region != null) {
                String serverName = region.getString("server-name");
                if (serverName != null && serverName.equalsIgnoreCase(currentServer)) {
                    return region;
                }
            }
        }
        throw new IllegalStateException("No region configuration found for server: " + currentServer);
    }

    public static String getRegionForLocation(Location loc) {
        ConfigurationSection regionsSection = getConfig().getConfigurationSection("regions");
        if (regionsSection == null) return null;
        for (String key : regionsSection.getKeys(false)) {
            ConfigurationSection region = regionsSection.getConfigurationSection(key);
            if (region == null) continue;
            int minX = region.getInt("min-x", Integer.MIN_VALUE);
            int maxX = region.getInt("max-x", Integer.MAX_VALUE);
            int minZ = region.getInt("min-z", Integer.MIN_VALUE);
            int maxZ = region.getInt("max-z", Integer.MAX_VALUE);
            double x = loc.getX();
            double z = loc.getZ();
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                return key;
            }
        }
        return null;
    }

    // === Teleport Settings ===

    public static int getTeleportRequestTimeout() {
        return getConfig().getInt("teleport.request-timeout", 3);
    }
}
