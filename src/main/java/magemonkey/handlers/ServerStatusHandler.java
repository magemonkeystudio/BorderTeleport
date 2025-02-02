package magemonkey.handlers;

import magemonkey.BorderTeleport;
import org.bukkit.Bukkit;

public class ServerStatusHandler {
    private final BorderTeleport plugin;

    public ServerStatusHandler(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    public void checkServerStatuses() {
        String serverName = plugin.getCurrentServerName();
        String regionKey = plugin.getCurrentRegionKey();

        plugin.getPluginLogger().info("[BorderTeleport] Server Status: " + serverName + " - Region: " + regionKey);
    }

}
