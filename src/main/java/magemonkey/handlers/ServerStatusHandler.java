package magemonkey.handlers;

import magemonkey.BorderTeleport;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

public class ServerStatusHandler {
    private final BorderTeleport plugin;
    private final Logger logger;

    public ServerStatusHandler(BorderTeleport plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void checkServerStatuses() {
        ConfigurationSection regions = plugin.getConfig().getConfigurationSection("regions");
        if (regions == null) return;

        for (String regionKey : regions.getKeys(false)) {
            String serverName = regions.getString(regionKey + ".server-name");
            if (serverName != null && !serverName.equals(plugin.currentServerName)) {
                checkServerStatus(serverName);
            }
        }
    }

    public void checkServerStatus(String server) {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

        if (onlinePlayers.isEmpty()) {
            logger.warning("[BorderTeleport] Cannot check server status - no online players");
            return;
        }

        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF("ServerStatus");
            out.writeUTF(server);

            // Send through the first online player
            onlinePlayers.iterator().next()
                    .sendPluginMessage(plugin, "BungeeCord", b.toByteArray());

        } catch (IOException e) {
            logger.warning(String.format("[BorderTeleport] Failed to check status of server %s: %s",
                    server, e.getMessage()));
        }
    }
}