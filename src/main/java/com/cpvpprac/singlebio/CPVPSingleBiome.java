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
    private boolean mvAutoLoadHintLogged = false;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        resetManager = new ResetManager(this, configManager);
        resetManager.startScheduler();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new ConfigGuiListener(this), this);
        getCommand("cpvpsb").setExecutor(new CommandHandler(this, configManager, resetManager));

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
    // MV autoload helper
    // -------------------------------------------------------------------------

    /**
     * Attempts to set autoload: false for the given world via Multiverse's own API objects.
     * Tries MV5 method names first, falls back to MV4. Logs a one-time hint if the API
     * is incompatible so the admin knows to configure it manually.
     */
    public void trySetMVAutoloadFalse(String worldName) {
        try {
            Plugin mvPlugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            if (mvPlugin == null || !mvPlugin.isEnabled()) return;

            // Resolve WorldManager — try MV5 name first, then MV4
            Object worldManager = null;
            for (String managerMethod : new String[]{"getWorldManager", "getMVWorldManager"}) {
                try {
                    worldManager = mvPlugin.getClass().getMethod(managerMethod).invoke(mvPlugin);
                    if (worldManager != null) break;
                } catch (NoSuchMethodException ignored) {}
            }
            if (worldManager == null) { logAutoLoadHint(); return; }

            // Resolve MVWorld — try MV5 name first, then MV4; unwrap Try/Optional if needed
            Object mvWorld = null;
            for (String worldMethod : new String[]{"getWorld", "getMVWorld"}) {
                try {
                    Method m = worldManager.getClass().getMethod(worldMethod, String.class);
                    mvWorld = unwrapMVResult(m.invoke(worldManager, worldName));
                    if (mvWorld != null) break;
                } catch (NoSuchMethodException ignored) {}
            }
            if (mvWorld == null) {
                getLogger().info("[MV] '" + worldName + "' not yet in MV — autoLoad will be set after mv import.");
                return;
            }

            Method setAutoLoad = mvWorld.getClass().getMethod("setAutoLoad", boolean.class);
            setAutoLoad.invoke(mvWorld, false);

            // Save — try multiple method names across MV versions
            for (String saveMethod : new String[]{"saveWorldsConfig", "saveWorldConfig", "saveConfig"}) {
                try { worldManager.getClass().getMethod(saveMethod).invoke(worldManager); break; }
                catch (NoSuchMethodException ignored) {}
            }

            getLogger().info("[MV] autoLoad: false set for '" + worldName + "' — effective next restart.");
        } catch (Exception e) {
            logAutoLoadHint();
        }
    }

    private Object unwrapMVResult(Object obj) {
        if (obj == null) return null;
        // Vavr Try: isSuccess() + get()
        try {
            Method isSuccess = obj.getClass().getMethod("isSuccess");
            if ((Boolean) isSuccess.invoke(obj))
                return obj.getClass().getMethod("get").invoke(obj);
            return null;
        } catch (Exception ignored) {}
        // Java Optional: isPresent() + get()
        try {
            Method isPresent = obj.getClass().getMethod("isPresent");
            if ((Boolean) isPresent.invoke(obj))
                return obj.getClass().getMethod("get").invoke(obj);
            return null;
        } catch (Exception ignored) {}
        return obj; // direct return
    }

    private void logAutoLoadHint() {
        if (!mvAutoLoadHintLogged) {
            getLogger().info("[MV] Could not set autoLoad automatically (MV API not compatible). "
                    + "Please set 'auto-load: false' manually in plugins/Multiverse-Core/worlds.yml "
                    + "for all arena worlds.");
            mvAutoLoadHintLogged = true;
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
