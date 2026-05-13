package com.cpvpprac.singlebio;

import com.cpvpprac.singlebio.generator.SingleBiomeChunkGenerator;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class ResetManager {

    public record Warning(int minutes, String message) {}

    private final CPVPSingleBiome plugin;
    private final ConfigManager config;

    private boolean maintenanceActive = false;
    private BukkitTask schedulerTask;

    public ResetManager(CPVPSingleBiome plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    // -------------------------------------------------------------------------
    // Scheduler
    // -------------------------------------------------------------------------

    public void startScheduler() {
        if (!config.isResetEnabled()) return;

        long ticksUntilReset = ticksUntilNextReset();
        List<Warning> warnings = config.getWarnings();

        // Schedule warning broadcasts
        for (Warning w : warnings) {
            long warningTicks = ticksUntilReset - (w.minutes() * 60L * 20L);
            if (warningTicks > 0) {
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> Bukkit.broadcastMessage(w.message()), warningTicks);
            }
        }

        // Schedule the reset itself
        schedulerTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("Daily scheduler triggered — starting reset.");
            resetAllWorlds();
        }, ticksUntilReset);

        long seconds = ticksUntilReset / 20;
        plugin.getLogger().info("Next reset in " + seconds + "s (" + (seconds / 60) + " min).");
    }

    private long ticksUntilNextReset() {
        String timeStr = config.getResetTime();
        LocalTime target;
        try {
            target = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid reset time '" + timeStr + "', defaulting to 04:00");
            target = LocalTime.of(4, 0);
        }
        LocalTime now = LocalTime.now();
        long secondsUntil = now.until(target, java.time.temporal.ChronoUnit.SECONDS);
        if (secondsUntil <= 0) secondsUntil += 24 * 60 * 60;
        return secondsUntil * 20L;
    }

    public void shutdown() {
        if (schedulerTask != null && !schedulerTask.isCancelled()) {
            schedulerTask.cancel();
        }
    }

    // -------------------------------------------------------------------------
    // Reset all configured worlds
    // -------------------------------------------------------------------------

    public void resetAllWorlds() {
        List<String> worlds = config.getEnabledWorlds();
        if (worlds.isEmpty()) {
            plugin.getLogger().warning("No enabled worlds in config — nothing to reset.");
            return;
        }

        setMaintenance(true);
        resetWorldSequence(worlds, 0);
    }

    private void resetWorldSequence(List<String> worlds, int index) {
        if (index >= worlds.size()) {
            onAllWorldsReset();
            return;
        }

        String worldName = worlds.get(index);
        resetSingleWorld(worldName, () -> {
            long delayTicks = config.getDelayBetweenWorldsSeconds() * 20L;
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> resetWorldSequence(worlds, index + 1), delayTicks);
        });
    }

    // -------------------------------------------------------------------------
    // Reset a single world
    // -------------------------------------------------------------------------

    public void resetSingleWorld(String worldName, Runnable onDone) {
        plugin.getLogger().info("Resetting world: " + worldName);

        // Step 1: teleport players to fallback
        teleportPlayersToFallback(worldName);

        // Step 2: unload world
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }

        // Step 3: mv remove if registered
        if (isMVWorld(worldName)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv remove " + worldName);
            plugin.getLogger().info("mv remove issued for: " + worldName);
        } else {
            plugin.getLogger().info("World not in MV, skipping mv remove: " + worldName);
        }

        // Step 4: backup (optional)
        if (config.isBackupEnabled()) {
            backupWorld(worldName);
        }

        // Step 5: delete world folder
        deleteWorldFolder(worldName);

        // Step 6: create new world on next tick (main thread, after folder is gone)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            createWorld(worldName);

            // Step 7: mv import with -g so the generator is documented in MV's worlds.yml.
            // The world is already loaded via WorldCreator with the correct generator;
            // MV only wraps the existing Bukkit world here.
            String env = getEnvironmentArg(worldName);
            String biomeKey = SingleBiomeChunkGenerator.BiomeType.fromKey(worldName).configKey();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "mv import " + worldName + " " + env + " -g CPVPSingleBiome:" + biomeKey);
            plugin.getLogger().info("mv import issued for: " + worldName);

            // Step 8: chunky pregeneration
            startChunky(worldName);

            if (onDone != null) onDone.run();
        }, 5L);
    }

    // -------------------------------------------------------------------------
    // Called after all worlds have been reset
    // -------------------------------------------------------------------------

    private void onAllWorldsReset() {
        plugin.getLogger().info("All worlds reset. Waiting for Chunky to finish...");

        if (!config.isChunkyEnabled()) {
            setMaintenance(false);
            reschedule();
            return;
        }

        long timeoutTicks = (long) config.getChunkyTimeoutMinutes() * 60 * 20;

        new BukkitRunnable() {
            long elapsed = 0;

            @Override
            public void run() {
                elapsed += 20 * 30;
                if (elapsed >= timeoutTicks) {
                    plugin.getLogger().info("Chunky timeout reached. Disabling maintenance.");
                    setMaintenance(false);
                    reschedule();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20 * 30L, 20 * 30L);
    }

    private void reschedule() {
        startScheduler();
    }

    // -------------------------------------------------------------------------
    // Chunky
    // -------------------------------------------------------------------------

    private void startChunky(String worldName) {
        if (!config.isChunkyEnabled()) return;
        int radius = config.getChunkyRadius();
        String shape = config.getChunkyShape();
        CommandSender console = Bukkit.getConsoleSender();
        Bukkit.dispatchCommand(console, "chunky world " + worldName);
        Bukkit.dispatchCommand(console, "chunky radius " + radius);
        Bukkit.dispatchCommand(console, "chunky shape " + shape);
        Bukkit.dispatchCommand(console, "chunky start");
        plugin.getLogger().info("Chunky pregeneration started for: " + worldName);
    }

    // -------------------------------------------------------------------------
    // Maintenance
    // -------------------------------------------------------------------------

    public void setMaintenance(boolean on) {
        if (!config.isMaintenanceEnabled()) return;
        maintenanceActive = on;
        String cmd = on ? "main on" : "main off";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        String broadcast = on ? config.getMaintenanceBroadcastOn() : config.getMaintenanceBroadcastOff();
        Bukkit.broadcastMessage(broadcast);
        plugin.getLogger().info("Maintenance " + (on ? "enabled" : "disabled") + ".");
    }

    public boolean isMaintenanceActive() {
        return maintenanceActive;
    }

    // -------------------------------------------------------------------------
    // World helpers
    // -------------------------------------------------------------------------

    private void teleportPlayersToFallback(String worldName) {
        World target = Bukkit.getWorld(worldName);
        if (target == null) return;

        World fallback = Bukkit.getWorld(config.getFallbackWorld());
        if (fallback == null) {
            plugin.getLogger().warning("Fallback world '" + config.getFallbackWorld() + "' not found!");
            return;
        }

        for (Player p : target.getPlayers()) {
            p.sendMessage(config.getMaintenanceKickMessage());
            p.teleport(fallback.getSpawnLocation());
        }
    }

    private void createWorld(String worldName) {
        SingleBiomeChunkGenerator.BiomeType biomeType =
                SingleBiomeChunkGenerator.BiomeType.fromKey(worldName);
        World.Environment env = biomeType == SingleBiomeChunkGenerator.BiomeType.END
                ? World.Environment.THE_END
                : World.Environment.NORMAL;

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(env);
        creator.generator(new com.cpvpprac.singlebio.generator.SingleBiomeChunkGenerator(
                plugin.getConfigManager(), biomeType));
        World created = creator.createWorld();
        if (created == null) {
            plugin.getLogger().severe("Failed to create world: " + worldName);
        } else {
            plugin.getLogger().info("World created with generator: " + worldName);
        }
    }

    private String getEnvironmentArg(String worldName) {
        SingleBiomeChunkGenerator.BiomeType biomeType =
                SingleBiomeChunkGenerator.BiomeType.fromKey(worldName);
        return biomeType == SingleBiomeChunkGenerator.BiomeType.END ? "end" : "normal";
    }

    private void deleteWorldFolder(String worldName) {
        File folder = new File(Bukkit.getWorldContainer(), worldName);
        if (!folder.exists()) return;
        try {
            Files.walk(folder.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            plugin.getLogger().info("Deleted world folder: " + worldName);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to delete world folder " + worldName + ": " + e.getMessage());
        }
    }

    private void backupWorld(String worldName) {
        File source = new File(Bukkit.getWorldContainer(), worldName);
        if (!source.exists()) return;
        File backupDir = new File(Bukkit.getWorldContainer(), config.getBackupDirectory());
        backupDir.mkdirs();
        File dest = new File(backupDir, worldName + "_" + System.currentTimeMillis());
        try {
            copyDirectory(source.toPath(), dest.toPath());
            plugin.getLogger().info("Backup created: " + dest.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Backup failed for " + worldName + ": " + e.getMessage());
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Files.copy(src, target.resolve(source.relativize(src)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Multiverse reflection helper
    // -------------------------------------------------------------------------

    private boolean isMVWorld(String worldName) {
        try {
            org.bukkit.plugin.Plugin mvPlugin =
                    Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            if (mvPlugin == null) return false;
            Method getWorldManager = mvPlugin.getClass().getMethod("getMVWorldManager");
            Object worldManager = getWorldManager.invoke(mvPlugin);
            Method isMVWorld = worldManager.getClass().getMethod("isMVWorld", String.class);
            return (boolean) isMVWorld.invoke(worldManager, worldName);
        } catch (Exception e) {
            plugin.getLogger().warning("isMVWorld check failed for '" + worldName + "': " + e.getMessage());
            return false;
        }
    }
}
