package com.cpvpprac.singlebio;

import com.cpvpprac.singlebio.generator.SingleBiomeChunkGenerator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class CPVPSingleBiome extends JavaPlugin {

    private static CPVPSingleBiome instance;
    private ConfigManager configManager;
    private ResetManager resetManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        resetManager = new ResetManager(this, configManager);
        resetManager.startScheduler();

        getCommand("cpvp").setExecutor(new CommandHandler(this, configManager, resetManager));
        getLogger().info("CPVPSingleBiome enabled.");
    }

    @Override
    public void onDisable() {
        if (resetManager != null) resetManager.shutdown();
        getLogger().info("CPVPSingleBiome disabled.");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        String biomeKey = (id != null && !id.isEmpty()) ? id.toLowerCase() : worldName.toLowerCase();
        SingleBiomeChunkGenerator.BiomeType biomeType = SingleBiomeChunkGenerator.BiomeType.fromKey(biomeKey);
        return new SingleBiomeChunkGenerator(configManager, biomeType);
    }

    public static CPVPSingleBiome getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ResetManager getResetManager() {
        return resetManager;
    }
}
