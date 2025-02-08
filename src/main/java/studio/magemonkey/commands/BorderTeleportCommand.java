package studio.magemonkey.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.BorderTeleport;

public class BorderTeleportCommand implements CommandExecutor {

    private final BorderTeleport plugin;

    public BorderTeleportCommand(@NotNull BorderTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        // Usage: /borderteleport reload
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /borderteleport reload")
                    .color(NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            // Check for the required permission node "borderteleport.reload"
            if (!sender.hasPermission("borderteleport.reload")) {
                sender.sendMessage(Component.text("You do not have permission to perform that command.")
                        .color(NamedTextColor.RED));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(Component.text("BorderTeleport configuration reloaded.")
                    .color(NamedTextColor.GREEN));
            plugin.getLogger().info("BorderTeleport configuration reloaded by " + sender.getName());
            return true;
        }

        sender.sendMessage(Component.text("Unknown subcommand. Usage: /borderteleport reload")
                .color(NamedTextColor.YELLOW));
        return true;
    }
}
