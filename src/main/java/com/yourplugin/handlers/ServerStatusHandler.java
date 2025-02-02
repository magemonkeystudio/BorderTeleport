package com.yourplugin.handlers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class ServerStatusHandler {

    private final BorderTeleport plugin;
    private Logger logger;

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
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF("ServerStatus");
            out.writeUTF(server);

            // Send through any online player
            if (!Bukkit.getOnlinePlayers().isEmpty()) {
                Bukkit.getOnlinePlayers().iterator().next()
                        .sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
            }
        } catch (IOException e) {
            logger.warning(String.format("[BorderTeleport] Failed to check status of server %s: %s",
                    server, e.getMessage()));
        }
    }
}