// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 Norbert Bohnenkamp
package com.cpvpprac.singlebio;

import com.cpvpprac.singlebio.generator.SingleBiomeChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public class CPVPSingleBiome extends JavaPlugin implements Listener {

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

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("cpvp").setExecutor(new CommandHandler(this, configManager, resetManager));

        // Set autoload:false for all configured arena worlds via the MV API.
        // MV is already enabled at this point (STARTUP phase). This modifies the
        // running MVWorld objects and saves worlds.yml through MV's own serializer —
        // no direct YAML file access. Takes effect from the next restart: MV will
        // skip those worlds and CPVPSingleBiome loads them via ServerLoadEvent.
        for (String worldName : configManager.getEnabledWorlds()) {
            trySetMVAutoloadFalse(worldName);
        }

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
    // ServerLoadEvent — loads arena worlds that MV skipped (autoload:false)
    // -------------------------------------------------------------------------

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) return;

        for (String worldName : configManager.getEnabledWorlds()) {
            if (Bukkit.getWorld(worldName) != null) continue; // loaded by MV or already handled

            SingleBiomeChunkGenerator.BiomeType biomeType =
                    SingleBiomeChunkGenerator.BiomeType.fromKey(worldName);
            World.Environment env = biomeType == SingleBiomeChunkGenerator.BiomeType.END
                    ? World.Environment.THE_END : World.Environment.NORMAL;

            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(env);
            creator.generator(new SingleBiomeChunkGenerator(configManager, biomeType));
            World loaded = creator.createWorld();
            if (loaded != null) {
                getLogger().info("[Startup] Loaded arena world: " + worldName);
            } else {
                getLogger().warning("[Startup] Failed to load arena world: " + worldName);
            }
        }
    }

    // -------------------------------------------------------------------------
    // MV autoload helper — uses the same reflection pattern as isMVWorld()
    // -------------------------------------------------------------------------

    /**
     * Sets autoload: false for the given world via Multiverse's own API objects.
     * MV then saves worlds.yml through its own serializer — no direct file access.
     * Best-effort: any exception is caught and logged; nothing crashes.
     * Called after onEnable() and after every mv import in the reset flow.
     */
    public void trySetMVAutoloadFalse(String worldName) {
        try {
            Plugin mvPlugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            if (mvPlugin == null || !mvPlugin.isEnabled()) return;

            Method getWorldManager = mvPlugin.getClass().getMethod("getMVWorldManager");
            Object worldManager = getWorldManager.invoke(mvPlugin);
            if (worldManager == null) return;

            Method getMVWorld = worldManager.getClass().getMethod("getMVWorld", String.class);
            Object mvWorld = getMVWorld.invoke(worldManager, worldName);
            if (mvWorld == null) {
                getLogger().info("[MV] '" + worldName + "' not yet in MV — set autoLoad: false manually after mv import.");
                return;
            }

            Method setAutoLoad = mvWorld.getClass().getMethod("setAutoLoad", boolean.class);
            setAutoLoad.invoke(mvWorld, false);

            Method saveConfig = worldManager.getClass().getMethod("saveWorldsConfig");
            saveConfig.invoke(worldManager);

            getLogger().info("[MV] autoLoad: false set for '" + worldName + "' — effective next restart.");
        } catch (Exception e) {
            getLogger().info("[MV] Could not set autoLoad via MV API for '" + worldName + "': " + e.getMessage()
                    + " — set autoLoad: false manually in Multiverse/worlds.yml.");
        }
    }

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
