package com.cpvpprac.singlebio;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final CPVPSingleBiome plugin;
    private FileConfiguration config;

    public ConfigManager(CPVPSingleBiome plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // --- World terrain config ---

    public int getBaseHeight(String worldKey) {
        return config.getInt("worlds." + worldKey + ".terrain.base-height", 64);
    }

    public double getFlatness(String worldKey) {
        return config.getDouble("worlds." + worldKey + ".terrain.flatness", 0.6);
    }

    public int getHeightVariation(String worldKey) {
        return config.getInt("worlds." + worldKey + ".terrain.height-variation", 12);
    }

    public double getNoiseScale(String worldKey) {
        return config.getDouble("worlds." + worldKey + ".terrain.noise-scale", 0.004);
    }

    public int getNoiseOctaves(String worldKey) {
        return config.getInt("worlds." + worldKey + ".terrain.noise-octaves", 6);
    }

    // --- World decoration config ---

    public boolean isDecorationsEnabled(String worldKey) {
        return config.getBoolean("worlds." + worldKey + ".decorations.enabled", true);
    }

    public double getGrassDensity(String worldKey) {
        return config.getDouble("worlds." + worldKey + ".decorations.grass-density", 0.3);
    }

    public double getFlowerDensity(String worldKey) {
        return config.getDouble("worlds." + worldKey + ".decorations.flower-density", 0.05);
    }

    public double getTreeDensity(String worldKey) {
        return config.getDouble("worlds." + worldKey + ".decorations.tree-density", 0.02);
    }

    public double getCactusDensity(String worldKey) {
        return config.getDouble("worlds." + worldKey + ".decorations.cactus-density", 0.015);
    }

    public double getDeadBushDensity(String worldKey) {
        return config.getDouble("worlds." + worldKey + ".decorations.dead-bush-density", 0.04);
    }

    public double getMushroomDensity(String worldKey) {
        return config.getDouble("worlds." + worldKey + ".decorations.mushroom-density", 0.04);
    }

    // --- Reset config ---

    public boolean isResetEnabled() {
        return config.getBoolean("reset.enabled", true);
    }

    public String getResetTime() {
        return config.getString("reset.time", "04:00");
    }

    public String getFallbackWorld() {
        return config.getString("reset.fallback-world", "world");
    }

    public int getDelayBetweenWorldsSeconds() {
        return config.getInt("reset.delay-between-worlds-seconds", 5);
    }

    public boolean isBackupEnabled() {
        return config.getBoolean("reset.backup.enabled", false);
    }

    public String getBackupDirectory() {
        return config.getString("reset.backup.directory", "world-backups");
    }

    public List<ResetManager.Warning> getWarnings() {
        List<ResetManager.Warning> warnings = new ArrayList<>();
        List<Map<?, ?>> raw = config.getMapList("reset.warnings");
        for (Map<?, ?> entry : raw) {
            Object mObj = entry.get("minutes");
            Object msgObj = entry.get("message");
            int minutes = (mObj instanceof Number) ? ((Number) mObj).intValue() : 0;
            String message = (msgObj instanceof String) ? (String) msgObj : "";
            if (minutes > 0 && !message.isEmpty()) {
                warnings.add(new ResetManager.Warning(minutes, message));
            }
        }
        return warnings;
    }

    // --- Chunky config ---

    public boolean isChunkyEnabled() {
        return config.getBoolean("chunky.enabled", true);
    }

    public int getChunkyRadius() {
        return config.getInt("chunky.radius", 500);
    }

    public String getChunkyShape() {
        return config.getString("chunky.shape", "circle");
    }

    public int getChunkyTimeoutMinutes() {
        return config.getInt("chunky.timeout-minutes", 30);
    }

    // --- Maintenance config ---

    public boolean isMaintenanceEnabled() {
        return config.getBoolean("maintenance.enabled", true);
    }

    public String getMaintenanceKickMessage() {
        return config.getString("maintenance.kick-message",
                "§c[CPVPSingleBiome] §fServer is resetting arenas. Please reconnect shortly.");
    }

    public String getMaintenanceBroadcastOn() {
        return config.getString("maintenance.broadcast-on",
                "§c[CPVPSingleBiome] §fMaintenance started — arenas are resetting.");
    }

    public String getMaintenanceBroadcastOff() {
        return config.getString("maintenance.broadcast-off",
                "§a[CPVPSingleBiome] §fArenas reset complete. Maintenance ended.");
    }

    // --- Worlds list ---

    public List<String> getEnabledWorlds() {
        List<String> worlds = new ArrayList<>();
        if (config.isConfigurationSection("worlds")) {
            for (String key : config.getConfigurationSection("worlds").getKeys(false)) {
                if (config.getBoolean("worlds." + key + ".enabled", false)) {
                    worlds.add(key);
                }
            }
        }
        return worlds;
    }

    public String getBiomeName(String worldKey) {
        return config.getString("worlds." + worldKey + ".biome", "PLAINS");
    }
}
