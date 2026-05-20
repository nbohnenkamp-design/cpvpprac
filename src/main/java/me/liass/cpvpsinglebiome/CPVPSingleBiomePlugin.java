package me.liass.cpvpsinglebiome;

import me.liass.cpvpsinglebiome.command.CPVPSBCommand;
import me.liass.cpvpsinglebiome.command.CPVPSBTabCompleter;
import me.liass.cpvpsinglebiome.config.ConfigManager;
import me.liass.cpvpsinglebiome.generator.BiomeType;
import me.liass.cpvpsinglebiome.generator.SingleBiomeChunkGenerator;
import me.liass.cpvpsinglebiome.listener.MaintenanceJoinListener;
import me.liass.cpvpsinglebiome.listener.WorldInitListener;
import me.liass.cpvpsinglebiome.reset.ResetManager;

import org.bukkit.command.PluginCommand;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public class CPVPSingleBiomePlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ResetManager resetManager;

    @Override
    public void onLoad() {
        ensureConfigManager();
    }

    @Override
    public void onEnable() {
        ensureConfigManager();

        this.resetManager = new ResetManager(this, this.configManager);

        PluginCommand command = getCommand("cpvpsb");

        if (command != null) {
            command.setExecutor(
                    new CPVPSBCommand(
                            this,
                            this.configManager,
                            this.resetManager
                    )
            );

            command.setTabCompleter(
                    new CPVPSBTabCompleter()
            );
        }

        getServer().getPluginManager().registerEvents(
                new WorldInitListener(),
                this
        );

        getServer().getPluginManager().registerEvents(
                new MaintenanceJoinListener(this),
                this
        );

        getServer().getScheduler().runTask(
                this,
                () -> this.resetManager.start()
        );

        getLogger().info(
                "CPVPSingleBiome v"
                        + getDescription().getVersion()
                        + " enabled."
        );

        getLogger().info(
                "Default biome: "
                        + this.configManager.getDefaultBiome()
        );

        getLogger().info(
                "Generator syntax: /mv create <world> normal -g CPVPSingleBiome:<biome>"
        );
    }

    @Override
    public void onDisable() {
        if (this.resetManager != null) {
            this.resetManager.stop();
        }

        getLogger().info("CPVPSingleBiome disabled.");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(
            String worldName,
            String id
    ) {
        try {
            ensureConfigManager();

            BiomeType fallback =
                    BiomeType.fromStringOrDefault(
                            this.configManager.getDefaultBiome(),
                            BiomeType.DESERT
                    );

            BiomeType biomeType =
                    BiomeType.fromStringOrDefault(
                            id,
                            fallback
                    );

            return new SingleBiomeChunkGenerator(
                    this.configManager,
                    biomeType
            );

        } catch (Throwable t) {
            getLogger().severe(
                    "Failed to build generator for world '"
                            + worldName
                            + "' (id='"
                            + id
                            + "'): "
                            + t.getClass().getSimpleName()
                            + " - "
                            + t.getMessage()
            );

            ensureConfigManager();

            return new SingleBiomeChunkGenerator(
                    this.configManager,
                    BiomeType.DESERT
            );
        }
    }

    private synchronized void ensureConfigManager() {
        if (this.configManager == null) {
            try {
                saveDefaultConfig();
            } catch (Throwable ignored) {
                // Defensive: config creation must not break generator loading.
            }

            this.configManager = new ConfigManager(this);
        }
    }

    public ConfigManager getConfigManager() {
        ensureConfigManager();

        return this.configManager;
    }

    public ResetManager getResetManager() {
        return this.resetManager;
    }
}
