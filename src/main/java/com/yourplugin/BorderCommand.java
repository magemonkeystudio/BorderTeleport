package com.yourplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class BorderCommand implements CommandExecutor {
    private final BorderTeleport plugin;

    public BorderCommand(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender instanceof Player && !sender.hasPermission("borderteleport.reload")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            plugin.reloadConfig();
            plugin.loadRegionBoundaries();
            sender.sendMessage(ChatColor.GREEN + "BorderTeleport configuration reloaded.");
            plugin.getLogger().info("[BorderTeleport] Configuration reloaded by " + sender.getName());
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /border reload");
        return true;
    }
}