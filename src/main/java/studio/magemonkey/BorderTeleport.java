package studio.magemonkey;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import studio.magemonkey.commands.BorderTeleportCommand;
import studio.magemonkey.database.MySQLManager;
import studio.magemonkey.handlers.ConfigHandler;
import studio.magemonkey.handlers.TeleportHandler;
import studio.magemonkey.listeners.BorderListener;
import studio.magemonkey.listeners.TransferJoinListener;

public class BorderTeleport extends JavaPlugin {

    private MySQLManager mysqlManager;
    private ConfigHandler configHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Load MySQL settings from the config.
        String host = getConfig().getString("mysql.host");
        int port = getConfig().getInt("mysql.port");
        String database = getConfig().getString("mysql.database");
        String username = getConfig().getString("mysql.username");
        String password = getConfig().getString("mysql.password");

        mysqlManager = new MySQLManager(host, port, database, username, password);
        mysqlManager.connect();
        mysqlManager.setupTable();

        // Initialize the configuration handler.
        configHandler = new ConfigHandler(this);

        // Register event listeners.
        getServer().getPluginManager().registerEvents(new BorderListener(this, mysqlManager), this);
        getServer().getPluginManager().registerEvents(new TransferJoinListener(this, mysqlManager), this);

        // Register TeleportHandler for incoming plugin messages on the "BungeeCord" channel.
        TeleportHandler teleportHandler = new TeleportHandler(this);
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", teleportHandler);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Register the /borderteleport command.
        PluginCommand command = getCommand("borderteleport");
        if (command != null) {
            command.setExecutor(new BorderTeleportCommand(this));
        } else {
            getLogger().severe("Command 'borderteleport' not found in plugin.yml!");
        }

        getLogger().info("BorderTeleport enabled.");
    }

    @Override
    public void onDisable() {
        if (mysqlManager != null) {
            mysqlManager.close();
        }
    }

    // Provide access to the configuration handler for other classes.
    public ConfigHandler getConfigHandler() {
        return configHandler;
    }
}
