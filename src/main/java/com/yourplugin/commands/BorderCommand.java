package com.yourplugin.commands;

import com.yourplugin.BorderTeleport;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BorderCommand implements CommandExecutor {

    private final BorderTeleport plugin;

    public BorderCommand(BorderTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getConfigHandler().loadRegionBoundaries();
            sender.sendMessage("BorderTeleport configuration reloaded.");
            return true;
        }
        return false;
    }
}