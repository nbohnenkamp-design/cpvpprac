package com.cpvpprac.singlebio;

import com.cpvpprac.singlebio.generator.SingleBiomeChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

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

        // ServerLoadEvent fires after all plugins (incl. MV) have fully enabled
        // and all worlds are loaded. Used to load any arena world that MV skipped
        // because autoload is set to false in MV's worlds.yml.
        getServer().getPluginManager().registerEvents(this, this);

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

    /**
     * Loads any configured arena world that was not auto-loaded by Multiverse
     * (i.e. autoload: false in MV's worlds.yml). Safe to call here because
     * ServerLoadEvent fires after STARTUP phase — Paper allows createWorld() at this point.
     *
     * If MV auto-loaded a world with the wrong generator (autoload: true and the
     * "Plugin not enabled" warning appeared), we skip it here to avoid disrupting
     * an already-loaded world. The user can suppress that warning permanently by
     * setting autoload: false for the arena worlds in Multiverse/worlds.yml.
     */
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) return;

        for (String worldName : configManager.getEnabledWorlds()) {
            if (Bukkit.getWorld(worldName) != null) continue; // already loaded by MV

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
