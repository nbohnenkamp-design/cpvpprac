package com.cpvpprac.singlebio;

import com.cpvpprac.singlebio.generator.SingleBiomeChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CPVPSingleBiome extends JavaPlugin implements Listener {

    private static CPVPSingleBiome instance;
    private ConfigManager configManager;
    private ResetManager resetManager;

    /**
     * Tracks worlds that WE loaded with the correct generator.
     * Used by WorldLoadEvent to decide whether a reload is needed.
     * Must be populated BEFORE WorldCreator.createWorld() is called.
     */
    private final Set<String> generatorAppliedWorlds = new HashSet<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        resetManager = new ResetManager(this, configManager);

        // Register WorldLoadEvent before scheduler so we catch everything
        getServer().getPluginManager().registerEvents(this, this);

        resetManager.startScheduler();
        getCommand("cpvp").setExecutor(new CommandHandler(this, configManager, resetManager));

        // Remove generator strings from MV worlds.yml so MV stops
        // trying to resolve CPVPSingleBiome before we are enabled.
        // Takes effect from the NEXT restart onward.
        clearMVGeneratorStrings();

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
    // WorldLoadEvent — fixes any arena world loaded without our generator
    // -------------------------------------------------------------------------

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        String worldName = event.getWorld().getName();
        if (!configManager.getEnabledWorlds().contains(worldName)) return;
        if (generatorAppliedWorlds.contains(worldName)) return; // We loaded it — generator is correct

        // This world was loaded by Multiverse (or something else) before our generator
        // was available. Schedule a reload on the next tick — cannot unload during load event.
        getLogger().info("[WorldFix] '" + worldName + "' was loaded without our generator — queuing reload.");
        getServer().getScheduler().runTask(this, () -> {
            World w = Bukkit.getWorld(worldName);
            if (w == null || generatorAppliedWorlds.contains(worldName)) return;

            // Move players away (should be none during startup or reset)
            World fallback = Bukkit.getWorld(configManager.getFallbackWorld());
            for (Player p : w.getPlayers()) {
                if (fallback != null) p.teleport(fallback.getSpawnLocation());
            }

            generatorAppliedWorlds.add(worldName); // Mark BEFORE create so next WorldLoadEvent skips
            Bukkit.unloadWorld(w, true);

            SingleBiomeChunkGenerator.BiomeType biomeType =
                    SingleBiomeChunkGenerator.BiomeType.fromKey(worldName);
            World.Environment env = biomeType == SingleBiomeChunkGenerator.BiomeType.END
                    ? World.Environment.THE_END : World.Environment.NORMAL;

            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(env);
            creator.generator(new SingleBiomeChunkGenerator(configManager, biomeType));
            World loaded = creator.createWorld();

            if (loaded != null) {
                getLogger().info("[WorldFix] Generator now active for: " + worldName);
            } else {
                getLogger().warning("[WorldFix] Failed to reload world: " + worldName);
                generatorAppliedWorlds.remove(worldName);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Generator-Set — ResetManager marks worlds here before creating them
    // -------------------------------------------------------------------------

    public void markGeneratorApplied(String worldName) {
        generatorAppliedWorlds.add(worldName);
    }

    public void unmarkGeneratorApplied(String worldName) {
        generatorAppliedWorlds.remove(worldName);
    }

    // -------------------------------------------------------------------------
    // Remove CPVPSingleBiome generator strings from MV's worlds.yml so MV
    // never tries to resolve them at startup. Worlds are loaded by us via
    // WorldCreator (correct generator) + mv import (no -g flag).
    // Effect: from the NEXT restart onward, no "Plugin not enabled" warning.
    // -------------------------------------------------------------------------

    private void clearMVGeneratorStrings() {
        File mvWorldsFile = new File(getDataFolder().getParentFile(), "Multiverse-Core/worlds.yml");
        if (!mvWorldsFile.exists()) return;

        YamlConfiguration mvConfig = YamlConfiguration.loadConfiguration(mvWorldsFile);
        boolean modified = false;

        for (String worldName : configManager.getEnabledWorlds()) {
            String path = "worlds." + worldName + ".generator";
            String generator = mvConfig.getString(path, "");
            if (generator != null && generator.startsWith("CPVPSingleBiome")) {
                mvConfig.set(path, null);
                modified = true;
                getLogger().info("[MVFix] Cleared generator string for '" + worldName + "' in MV worlds.yml.");
            }
        }

        if (modified) {
            try {
                mvConfig.save(mvWorldsFile);
                getLogger().info("[MVFix] Multiverse worlds.yml updated — no generator warnings on next restart.");
            } catch (IOException e) {
                getLogger().warning("[MVFix] Could not save Multiverse worlds.yml: " + e.getMessage());
            }
        }
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
