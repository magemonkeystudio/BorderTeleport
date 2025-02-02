package magemonkey.commands;

import magemonkey.BorderTeleport;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BorderCommand implements CommandExecutor {
    private final BorderTeleport plugin;

    public BorderCommand(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender has permission to reload
        if (!sender.hasPermission("borderteleport.reload")) {
            sender.sendMessage("§cYou do not have permission to reload BorderTeleport.");
            return true;
        }

        // Check for reload argument
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            // Reload configuration
            plugin.reloadConfig();
            plugin.configHandler.loadConfiguration();

            // Send confirmation message
            sender.sendMessage("§aBorderTeleport configuration reloaded successfully.");

            // Log reload to console
            String reloadedBy = sender instanceof Player player ? player.getName() : "CONSOLE";
            plugin.getLogger().info("BorderTeleport configuration reloaded by " + reloadedBy);

            return true;
        }

        // Show usage if no valid arguments
        sender.sendMessage("§cUsage: /border reload");
        return true;
    }

}