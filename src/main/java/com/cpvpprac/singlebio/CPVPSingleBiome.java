package com.cpvpprac.singlebio;

import com.cpvpprac.singlebio.generator.SingleBiomeChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CPVPSingleBiome extends JavaPlugin {

    private static CPVPSingleBiome instance;
    private ConfigManager configManager;
    private ResetManager resetManager;

    // -------------------------------------------------------------------------
    // onLoad — runs before ANY plugin's onEnable, including Multiverse-Core.
    // We set autoload: false for all configured arena worlds so MV's onEnable
    // reads the modified worlds.yml and skips those worlds entirely.
    // CPVPSingleBiome then loads them itself in onEnable with the correct generator.
    // -------------------------------------------------------------------------

    @Override
    public void onLoad() {
        saveDefaultConfig();
        setMVAutoload(readEnabledWorldsFromConfig(), false);
    }

    @Override
    public void onEnable() {
        instance = this;
        // Config already saved in onLoad; ConfigManager re-reads from disk
        configManager = new ConfigManager(this);
        resetManager = new ResetManager(this, configManager);

        // Load arena worlds that MV skipped (autoload: false set in onLoad)
        loadArenaWorlds();

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

    // -------------------------------------------------------------------------
    // Startup world loading
    // -------------------------------------------------------------------------

    private void loadArenaWorlds() {
        for (String worldName : configManager.getEnabledWorlds()) {
            if (Bukkit.getWorld(worldName) != null) {
                // Unlikely after autoload:false fix, but handle gracefully
                getLogger().info("[Startup] World already loaded: " + worldName);
                continue;
            }

            SingleBiomeChunkGenerator.BiomeType biomeType =
                    SingleBiomeChunkGenerator.BiomeType.fromKey(worldName);
            World.Environment env = biomeType == SingleBiomeChunkGenerator.BiomeType.END
                    ? World.Environment.THE_END : World.Environment.NORMAL;

            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(env);
            creator.generator(new SingleBiomeChunkGenerator(configManager, biomeType));

            World loaded = creator.createWorld();
            if (loaded != null) {
                getLogger().info("[Startup] Arena world loaded: " + worldName);
            } else {
                getLogger().warning("[Startup] Failed to load arena world: " + worldName);
            }
        }
    }

    // -------------------------------------------------------------------------
    // MV autoload helper — called from onLoad (List) and ResetManager (single)
    // -------------------------------------------------------------------------

    /**
     * Sets autoload for a single world in Multiverse-Core/worlds.yml.
     * Called by ResetManager after mv import to keep autoload: false.
     */
    public void setMVAutoload(String worldName, boolean autoload) {
        setMVAutoload(List.of(worldName), autoload);
    }

    private void setMVAutoload(List<String> worldNames, boolean autoload) {
        File mvWorldsFile = new File(getDataFolder().getParentFile(), "Multiverse-Core/worlds.yml");
        if (!mvWorldsFile.exists()) return;

        YamlConfiguration mvConfig = YamlConfiguration.loadConfiguration(mvWorldsFile);
        boolean modified = false;

        for (String worldName : worldNames) {
            String sectionPath = "worlds." + worldName;
            if (!mvConfig.contains(sectionPath)) continue; // World not yet in MV — nothing to do

            String autoloadPath = sectionPath + ".autoload";
            if (mvConfig.getBoolean(autoloadPath, true) != autoload) {
                mvConfig.set(autoloadPath, autoload);
                modified = true;
                getLogger().info("[MVConfig] " + worldName + ": autoload=" + autoload);
            }
        }

        if (modified) {
            try {
                mvConfig.save(mvWorldsFile);
            } catch (IOException e) {
                getLogger().warning("[MVConfig] Could not save MV worlds.yml: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Config bootstrap — used in onLoad before ConfigManager is available
    // -------------------------------------------------------------------------

    private List<String> readEnabledWorldsFromConfig() {
        List<String> worlds = new ArrayList<>();
        FileConfiguration config = getConfig();
        if (!config.isConfigurationSection("worlds")) return worlds;
        for (String key : config.getConfigurationSection("worlds").getKeys(false)) {
            if (config.getBoolean("worlds." + key + ".enabled", false)) {
                worlds.add(key);
            }
        }
        return worlds;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

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
