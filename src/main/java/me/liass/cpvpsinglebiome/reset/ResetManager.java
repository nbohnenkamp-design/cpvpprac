package me.liass.cpvpsinglebiome.reset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import me.liass.cpvpsinglebiome.CPVPSingleBiomePlugin;
import me.liass.cpvpsinglebiome.config.ConfigManager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetManager {

    private final CPVPSingleBiomePlugin plugin;
    private final ConfigManager config;

    private boolean resetInProgress = false;
    private int schedulerTaskId = -1;
    private LocalDate lastAutoResetDate = null;
    private LocalDateTime nextResetAt = null;

    public ResetManager(
            CPVPSingleBiomePlugin plugin,
            ConfigManager config
    ) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        stop();

        if (!config.isResetEnabled()) {
            plugin.getLogger().info("Automatic arena reset is disabled.");
            return;
        }

        calculateNextReset();

        schedulerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                this::tick,
                20L,
                20L * 30L
        );

        plugin.getLogger().info(
                "Automatic arena reset enabled. Next reset: "
                        + nextResetAt
        );
    }

    public void stop() {
        if (schedulerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(schedulerTaskId);
            schedulerTaskId = -1;
        }
    }

    public void reload() {
        config.reload();
        start();
    }

    public boolean isResetInProgress() {
        return resetInProgress;
    }

    public LocalDate getLastAutoResetDate() {
        return lastAutoResetDate;
    }

    public LocalDateTime getNextResetAt() {
        return nextResetAt;
    }

    private void tick() {
        if (resetInProgress) {
            return;
        }

        if (nextResetAt == null) {
            calculateNextReset();
            return;
        }

        ZoneId zone = config.getResetZone();
        LocalDateTime now = LocalDateTime.now(zone);

        long minutesLeft = Duration.between(now, nextResetAt).toMinutes();

        if (config.isWarningEnabled()) {
            for (Integer warning : config.getWarningMinutes()) {
                if (minutesLeft == warning) {
                    Bukkit.broadcastMessage(
                            config.getPrefix()
                                    + "Arena reset in "
                                    + warning
                                    + " minute(s)."
                    );
                }
            }
        }

        if (!now.isBefore(nextResetAt)) {
            if (config.isSkipIfAnyPlayerOnline()
                    && !Bukkit.getOnlinePlayers().isEmpty()) {
                plugin.getLogger().info(
                        "Arena reset skipped because players are online."
                );

                calculateNextReset();
                return;
            }

            resetAll(Bukkit.getConsoleSender());
            lastAutoResetDate = now.toLocalDate();
            calculateNextReset();
        }
    }

    private void calculateNextReset() {
        ZoneId zone = config.getResetZone();

        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime candidate = now.toLocalDate().atTime(
                config.getResetTime()
        );

        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(config.getResetIntervalDays());
        }

        nextResetAt = candidate;
    }

    public void resetAll(CommandSender sender) {
        if (resetInProgress) {
            sender.sendMessage(config.getPrefix() + "Reset already running.");
            return;
        }

        List<ResetWorldSpec> worlds = config.getResetWorlds();

        if (worlds.isEmpty()) {
            sender.sendMessage(config.getPrefix() + "No reset worlds configured.");
            return;
        }

        resetInProgress = true;

        sender.sendMessage(config.getPrefix() + "Starting arena reset...");

        if (config.isMaintenanceDuringReset()) {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    config.getMaintenanceCommandOn()
            );
        }

        resetNext(sender, worlds, 0);
    }

    private void resetNext(
            CommandSender sender,
            List<ResetWorldSpec> worlds,
            int index
    ) {
        if (index >= worlds.size()) {
            resetInProgress = false;

            if (config.isMaintenanceDuringReset()) {
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        config.getMaintenanceCommandOff()
                );
            }

            sender.sendMessage(config.getPrefix() + "Arena reset complete.");
            return;
        }

        ResetWorldSpec spec = worlds.get(index);

        resetWorldInternal(
                spec.worldName(),
                spec.biomeName(),
                sender
        );

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> resetNext(sender, worlds, index + 1),
                20L * 5L
        );
    }

    public void resetWorldByName(
            String worldName,
            CommandSender sender
    ) {
        if (resetInProgress) {
            sender.sendMessage(config.getPrefix() + "Reset already running.");
            return;
        }

        for (ResetWorldSpec spec : config.getResetWorlds()) {
            if (spec.worldName().equalsIgnoreCase(worldName)) {
                resetInProgress = true;

                resetWorldInternal(
                        spec.worldName(),
                        spec.biomeName(),
                        sender
                );

                resetInProgress = false;
                return;
            }
        }

        sender.sendMessage(
                config.getPrefix()
                        + "World is not listed in reset.worlds: "
                        + worldName
        );
    }

    public void resetWorldNow(
            String worldName,
            String biomeName
    ) {
        if (resetInProgress) {
            plugin.getLogger().warning("Reset already running.");
            return;
        }

        resetInProgress = true;

        resetWorldInternal(
                worldName,
                biomeName,
                Bukkit.getConsoleSender()
        );

        resetInProgress = false;
    }

    private void resetWorldInternal(
            String worldName,
            String biomeName,
            CommandSender sender
    ) {
        if (worldName == null || worldName.isBlank()) {
            sender.sendMessage(config.getPrefix() + "Invalid world name.");
            return;
        }

        if (worldName.equalsIgnoreCase("world")) {
            sender.sendMessage(
                    config.getPrefix()
                            + "Refusing to reset main world: "
                            + worldName
            );
            return;
        }

        sender.sendMessage(
                config.getPrefix()
                        + "Resetting world "
                        + worldName
                        + " as biome "
                        + biomeName
                        + "..."
        );

        evacuatePlayers(worldName);

        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }

        Path worldPath = Bukkit.getWorldContainer().toPath().resolve(worldName);

        if (Files.exists(worldPath)) {
            try {
                if (config.isBackupOldWorlds()) {
                    Path backupPath = Bukkit.getWorldContainer()
                            .toPath()
                            .resolve(
                                    worldName
                                            + "_old_"
                                            + System.currentTimeMillis()
                            );

                    Files.move(worldPath, backupPath);

                    if (config.isDeleteOldWorldsAfterSuccess()) {
                        deleteDirectory(backupPath);
                    }
                } else {
                    deleteDirectory(worldPath);
                }
            } catch (IOException e) {
                sender.sendMessage(
                        config.getPrefix()
                                + "Could not delete or backup world folder: "
                                + e.getMessage()
                );
                return;
            }
        }

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "mv remove " + worldName
        );

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "mv create "
                        + worldName
                        + " normal -g CPVPSingleBiome:"
                        + biomeName
        );

        if (config.isChunkyEnabled()) {
            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> {
                        World newWorld = Bukkit.getWorld(worldName);

                        if (newWorld != null) {
                            me.liass.cpvpsinglebiome.chunky.ChunkyIntegration.start(
                                    plugin,
                                    newWorld,
                                    config
                            );
                        }
                    },
                    20L * config.getChunkyDelaySeconds()
            );
        }
    }

    private void evacuatePlayers(String worldName) {
        World fallback = Bukkit.getWorld(config.getFallbackWorld());

        if (fallback == null) {
            fallback = Bukkit.getWorlds().get(0);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equalsIgnoreCase(worldName)) {
                player.teleport(fallback.getSpawnLocation());
                player.sendMessage(
                        config.getPrefix()
                                + "This arena is resetting."
                );
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        try (var stream = Files.walk(path)) {
            stream
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
