package magemonkey.config;

import magemonkey.BorderTeleport;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigurationHandler {
    private final BorderTeleport plugin;
    private final Logger logger;
    private final Map<String, ConfigurationSection> regionConfigs;

    public ConfigurationHandler(BorderTeleport plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.regionConfigs = new HashMap<>();
    }

    public void loadConfiguration() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        regionConfigs.clear();

        if (config.contains("regions")) {
            ConfigurationSection regionsSection = config.getConfigurationSection("regions");
            if (regionsSection != null) {
                for (String regionKey : regionsSection.getKeys(false)) {
                    ConfigurationSection regionSection = regionsSection.getConfigurationSection(regionKey);
                    if (regionSection != null) {
                        regionConfigs.put(regionKey, regionSection);
                        logger.info("[BorderTeleport] Loaded region: " + regionKey + " -> Server: " + regionSection.getString("server"));
                    }
                }
            }
        } else {
            logger.warning("[BorderTeleport] No regions found in config.yml!");
        }
    }

    public String getRegionKeyByServerName(String serverName) {
        for (String regionKey : regionConfigs.keySet()) {
            ConfigurationSection section = regionConfigs.get(regionKey);
            String regionServer = section.getString("server");

            if (serverName.equalsIgnoreCase(regionServer)) {
                return regionKey;
            }
        }
        return null;
    }

    public String getNeighboringServer(String regionKey, String direction) {
        ConfigurationSection section = regionConfigs.get(regionKey);
        if (section != null) {
            return section.getString("neighbors." + direction, null);
        }
        return null;
    }

    public Map<String, ConfigurationSection> getRegionConfigs() {
        return regionConfigs;
    }
}
