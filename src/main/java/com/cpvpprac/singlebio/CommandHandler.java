package com.cpvpprac.singlebio;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

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
        if (!sender.hasPermission("cpvpsb.admin")) {
            sender.sendMessage("§cYou don't have permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reset" -> handleReset(sender, args);
            case "chunky" -> handleChunky(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // /cpvpsb reset <status|reload|now|<world>>
    // -------------------------------------------------------------------------

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /cpvpsb reset <status|reload|now|<world>>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "status" -> {
                sender.sendMessage("§6=== CPVPSingleBiome Status ===");
                sender.sendMessage("§fMaintenance: " + (resetManager.isMaintenanceActive() ? "§cON" : "§aOFF"));
                sender.sendMessage("§fEnabled worlds: §e" + String.join(", ", config.getEnabledWorlds()));
                sender.sendMessage("§fNext reset time: §e" + config.getResetTime());
                sender.sendMessage("§fReset enabled: §e" + config.isResetEnabled());
            }
            case "reload" -> {
                config.reload();
                resetManager.shutdown();
                resetManager.startScheduler();
                sender.sendMessage("§aConfig reloaded and scheduler restarted.");
            }
            case "now" -> {
                sender.sendMessage("§eStarting full arena reset...");
                plugin.getServer().getScheduler().runTask(plugin, resetManager::resetAllWorlds);
            }
            default -> {
                String worldName = args[1];
                sender.sendMessage("§eResetting world: §f" + worldName);
                resetManager.resetSingleWorld(worldName, () ->
                        sender.sendMessage("§aReset complete for: §f" + worldName));
            }
        }
    }

    // -------------------------------------------------------------------------
    // /cpvpsb chunky <start <world>|start-all>
    // -------------------------------------------------------------------------

    private void handleChunky(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /cpvpsb chunky <start <world>|start-all>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "start" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /cpvpsb chunky start <world>");
                    return;
                }
                String worldName = args[2];
                resetManager.triggerChunky(worldName);
                sender.sendMessage("§aChunky pregeneration started for: §f" + worldName);
            }
            case "start-all" -> {
                List<String> worlds = config.getEnabledWorlds();
                if (worlds.isEmpty()) {
                    sender.sendMessage("§cNo enabled worlds in config.");
                    return;
                }
                for (String worldName : worlds) {
                    resetManager.triggerChunky(worldName);
                }
                sender.sendMessage("§aChunky pregeneration started for: §f" + String.join(", ", worlds));
            }
            default -> sender.sendMessage("§cUsage: /cpvpsb chunky <start <world>|start-all>");
        }
    }

    // -------------------------------------------------------------------------

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== CPVPSingleBiome ===");
        sender.sendMessage("§e/cpvpsb reset status §f- Show status");
        sender.sendMessage("§e/cpvpsb reset reload §f- Reload config and restart scheduler");
        sender.sendMessage("§e/cpvpsb reset now §f- Reset all enabled worlds immediately");
        sender.sendMessage("§e/cpvpsb reset <world> §f- Reset a single world");
        sender.sendMessage("§e/cpvpsb chunky start <world> §f- Start Chunky for one world");
        sender.sendMessage("§e/cpvpsb chunky start-all §f- Start Chunky for all enabled worlds");
    }
}
