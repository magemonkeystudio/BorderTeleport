package studio.magemonkey.borderteleport.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import studio.magemonkey.borderteleport.BorderTeleport;

public class BorderTeleportCommand implements CommandExecutor {

    private final BorderTeleport plugin;

    public BorderTeleportCommand(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Usage: /borderteleport reload
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /borderteleport reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("borderteleport.reload")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to perform that command.");
                return true;
            }
            plugin.reloadConfig();
            // If you use any cached configuration objects, reinitialize them here.
            sender.sendMessage(ChatColor.GREEN + "BorderTeleport configuration reloaded.");
            plugin.getLogger().info("BorderTeleport configuration reloaded by " + sender.getName());
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Unknown subcommand. Usage: /borderteleport reload");
        return true;
    }
}
