package com.cpvpprac.singlebio;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandHandler implements CommandExecutor {

    private final CPVPSingleBiome plugin;
    private final ConfigManager config;
    private final ResetManager resetManager;

    public CommandHandler(CPVPSingleBiome plugin, ConfigManager config, ResetManager resetManager) {
        this.plugin = plugin;
        this.config = config;
        this.resetManager = resetManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cpvp.admin")) {
            sender.sendMessage("§cYou don't have permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /cpvp reset <world>");
                    return true;
                }
                String worldName = args[1];
                sender.sendMessage("§eResetting world: " + worldName);
                resetManager.resetSingleWorld(worldName, () ->
                        sender.sendMessage("§aReset complete for: " + worldName));
            }
            case "resetall" -> {
                sender.sendMessage("§eStarting full arena reset...");
                plugin.getServer().getScheduler().runTask(plugin, resetManager::resetAllWorlds);
            }
            case "status" -> {
                sender.sendMessage("§6=== CPVPSingleBiome Status ===");
                sender.sendMessage("§fMaintenance: " + (resetManager.isMaintenanceActive() ? "§cON" : "§aOFF"));
                sender.sendMessage("§fEnabled worlds: §e" + String.join(", ", config.getEnabledWorlds()));
                sender.sendMessage("§fNext reset time: §e" + config.getResetTime());
            }
            case "reload" -> {
                config.reload();
                resetManager.shutdown();
                resetManager.startScheduler();
                sender.sendMessage("§aConfig reloaded and scheduler restarted.");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== CPVPSingleBiome ===");
        sender.sendMessage("§e/cpvp reset <world> §f- Reset a single world");
        sender.sendMessage("§e/cpvp resetall §f- Reset all enabled worlds");
        sender.sendMessage("§e/cpvp status §f- Show current status");
        sender.sendMessage("§e/cpvp reload §f- Reload config");
    }
}
