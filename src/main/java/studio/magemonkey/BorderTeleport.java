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

    private static BorderTeleport instance;
    private MySQLManager mysqlManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Retrieve MySQL settings from ConfigHandler.
        String host = ConfigHandler.getMySQLHost();
        int port = ConfigHandler.getMySQLPort();
        String database = ConfigHandler.getMySQLDatabase();
        String username = ConfigHandler.getMySQLUsername();
        String password = ConfigHandler.getMySQLPassword();

        mysqlManager = new MySQLManager(host, port, database, username, password);
        mysqlManager.connect();
        if (!mysqlManager.isConnected()) {
            getLogger().severe("MySQL connection failed, disabling BorderTeleport plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        mysqlManager.setupTable();

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

    public static BorderTeleport getInstance() {
        return instance;
    }
}
