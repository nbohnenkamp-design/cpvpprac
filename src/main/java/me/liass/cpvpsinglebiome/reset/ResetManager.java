package me.liass.cpvpsinglebiome.reset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

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

    private LocalDateTime joinBlockUntil = null;

    private final Set<Integer> sentWarnings = new HashSet<>();

    public ResetManager(
            CPVPSingleBiomePlugin plugin,
            ConfigManager config
    ) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        stop();

        this.sentWarnings.clear();

        if (!config.isResetEnabled()) {
            plugin.getLogger().info(
                    "Automatic arena reset is disabled."
            );
            return;
        }

        calculateNextReset();

        this.schedulerTaskId =
                Bukkit.getScheduler().scheduleSyncRepeatingTask(
                        plugin,
                        this::tick,
                        20L,
                        20L * 30L
                );

        plugin.getLogger().info(
                "Automatic arena reset enabled. Next reset: "
                        + nextResetAt
                        + " ("
                        + config.getResetZone()
                        + ")"
        );
    }

    public void stop() {
        if (this.schedulerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.schedulerTaskId);
            this.schedulerTaskId = -1;
        }
    }

    public void reload() {
        config.reload();
        start();
    }

    public boolean isResetInProgress() {
        return this.resetInProgress;
    }

    public boolean isMaintenanceActive() {
        if (!config.isBlockJoinsDuringReset()) {
            return false;
        }

        if (this.resetInProgress) {
            return true;
        }

        if (this.joinBlockUntil == null) {
            return false;
        }

        LocalDateTime now =
                LocalDateTime.now(
                        config.getResetZone()
                );

        return now.isBefore(this.joinBlockUntil);
    }

    public boolean canBypassMaintenance(Player player) {
        if (player == null) {
            return false;
        }

        if (player.isOp()) {
            return true;
        }

        return player.hasPermission(
                config.getMaintenanceBypassPermission()
        );
    }

    public LocalDate getLastAutoResetDate() {
        return this.lastAutoResetDate;
    }

    public LocalDateTime getNextResetAt() {
        return this.nextResetAt;
    }

    private void tick() {
        if (this.resetInProgress) {
            return;
        }

        if (this.nextResetAt == null) {
            calculateNextReset();
            return;
        }

        ZoneId zone =
                config.getResetZone();

        LocalDateTime now =
                LocalDateTime.now(zone);

        long minutesLeft =
                Duration.between(
                        now,
                        this.nextResetAt
                ).toMinutes();

        sendWarningsIfNeeded(minutesLeft);

        if (!now.isBefore(this.nextResetAt)) {
            if (config.isSkipIfAnyPlayerOnline()
                    && hasNormalPlayersOnline()) {
                postponeResetBecausePlayersAreOnline(now);
                return;
            }

            resetAll(
                    Bukkit.getConsoleSender()
            );

            this.lastAutoResetDate =
                    now.toLocalDate();

            calculateNextReset();
        }
    }

    private void sendWarningsIfNeeded(long minutesLeft) {
        if (!config.isWarningEnabled()) {
            return;
        }

        for (Integer warning : config.getWarningMinutes()) {
            if (warning == null) {
                continue;
            }

            if (minutesLeft == warning
                    && !this.sentWarnings.contains(warning)) {
                Bukkit.broadcastMessage(
                        config.getPrefix()
                                + "Arena reset in "
                                + warning
                                + " minute(s)."
                );

                this.sentWarnings.add(warning);
            }
        }
    }

    private void postponeResetBecausePlayersAreOnline(LocalDateTime now) {
        int retryMinutes =
                config.getRetryAfterMinutes();

        this.nextResetAt =
                now.plusMinutes(retryMinutes);

        this.sentWarnings.clear();

        plugin.getLogger().info(
                "Arena reset postponed by "
                        + retryMinutes
                        + " minute(s), because normal players are online: "
                        + getNormalPlayerNames()
        );
    }

    private boolean hasNormalPlayersOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canBypassMaintenance(player)) {
                return true;
            }
        }

        return false;
    }

    private String getNormalPlayerNames() {
        StringJoiner joiner =
                new StringJoiner(", ");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canBypassMaintenance(player)) {
                joiner.add(player.getName());
            }
        }

        String names =
                joiner.toString();

        return names.isBlank()
                ? "-"
                : names;
    }

    private void calculateNextReset() {
        ZoneId zone =
                config.getResetZone();

        LocalDateTime now =
                LocalDateTime.now(zone);

        LocalDateTime candidate =
                now.toLocalDate().atTime(
                        config.getResetTime()
                );

        if (!candidate.isAfter(now)) {
            candidate =
                    candidate.plusDays(
                            config.getResetIntervalDays()
                    );
        }

        this.nextResetAt =
                candidate;

        this.sentWarnings.clear();
    }

    public void resetAll(CommandSender sender) {
        if (this.resetInProgress) {
            sender.sendMessage(
                    config.getPrefix()
                            + "Reset already running."
            );
            return;
        }

        List<ResetWorldSpec> worlds =
                config.getResetWorlds();

        if (worlds.isEmpty()) {
            sender.sendMessage(
                    config.getPrefix()
                            + "No reset worlds configured."
            );
            return;
        }

        beginMaintenance();

        sender.sendMessage(
                config.getPrefix()
                        + "Starting arena reset..."
        );

        if (config.isMaintenanceDuringReset()) {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    config.getMaintenanceCommandOn()
            );
        }

        resetNext(
                sender,
                worlds,
                0
        );
    }

    private void resetNext(
            CommandSender sender,
            List<ResetWorldSpec> worlds,
            int index
    ) {
        if (index >= worlds.size()) {
            finishMaintenance(sender);
            return;
        }

        ResetWorldSpec spec =
                worlds.get(index);

        resetWorldInternal(
                spec.worldName(),
                spec.biomeName(),
                sender
        );

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> resetNext(
                        sender,
                        worlds,
                        index + 1
                ),
                20L * 5L
        );
    }

    public void resetWorldByName(
            String worldName,
            CommandSender sender
    ) {
        if (this.resetInProgress) {
            sender.sendMessage(
                    config.getPrefix()
                            + "Reset already running."
            );
            return;
        }

        for (ResetWorldSpec spec : config.getResetWorlds()) {
            if (spec.worldName().equalsIgnoreCase(worldName)) {
                beginMaintenance();

                resetWorldInternal(
                        spec.worldName(),
                        spec.biomeName(),
                        sender
                );

                finishMaintenance(sender);
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
        if (this.resetInProgress) {
            plugin.getLogger().warning(
                    "Reset already running."
            );
            return;
        }

        beginMaintenance();

        resetWorldInternal(
                worldName,
                biomeName,
                Bukkit.getConsoleSender()
        );

        finishMaintenance(
                Bukkit.getConsoleSender()
        );
    }

    private void beginMaintenance() {
        this.resetInProgress = true;
        this.joinBlockUntil = null;

        if (config.isBlockJoinsDuringReset()) {
            plugin.getLogger().info(
                    "Join guard enabled: normal players are blocked during arena reset."
            );
        }
    }

    private void finishMaintenance(CommandSender sender) {
        if (config.isMaintenanceDuringReset()) {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    config.getMaintenanceCommandOff()
            );
        }

        int extraMinutes =
                config.getJoinBlockExtraMinutesAfterReset();

        if (config.isBlockJoinsDuringReset()
                && extraMinutes > 0) {
            this.joinBlockUntil =
                    LocalDateTime.now(
                            config.getResetZone()
                    ).plusMinutes(extraMinutes);

            plugin.getLogger().info(
                    "Arena reset finished. Join guard remains active for "
                            + extraMinutes
                            + " minute(s), so Chunky can finish without players joining."
            );

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    this::clearExpiredJoinBlock,
                    20L * 60L * extraMinutes
            );

        } else {
            this.joinBlockUntil = null;
        }

        this.resetInProgress = false;

        sender.sendMessage(
                config.getPrefix()
                        + "Arena reset complete."
        );
    }

    private void clearExpiredJoinBlock() {
        if (this.joinBlockUntil == null) {
            return;
        }

        LocalDateTime now =
                LocalDateTime.now(
                        config.getResetZone()
                );

        if (!now.isBefore(this.joinBlockUntil)) {
            this.joinBlockUntil = null;

            plugin.getLogger().info(
                    "Join guard disabled. Normal players may join again."
            );
        }
    }

    private void resetWorldInternal(
            String worldName,
            String biomeName,
            CommandSender sender
    ) {
        if (worldName == null || worldName.isBlank()) {
            sender.sendMessage(
                    config.getPrefix()
                            + "Invalid world name."
            );
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

        World world =
                Bukkit.getWorld(worldName);

        if (world != null) {
            Bukkit.unloadWorld(
                    world,
                    false
            );
        }

        Path worldPath =
                Bukkit.getWorldContainer()
                        .toPath()
                        .resolve(worldName);

        if (Files.exists(worldPath)) {
            try {
                if (config.isBackupOldWorlds()) {
                    Path backupPath =
                            Bukkit.getWorldContainer()
                                    .toPath()
                                    .resolve(
                                            worldName
                                                    + "_old_"
                                                    + System.currentTimeMillis()
                                    );

                    Files.move(
                            worldPath,
                            backupPath
                    );

                    if (config.isDeleteOldWorldsAfterSuccess()) {
                        deleteDirectory(
                                backupPath
                        );
                    }

                } else {
                    deleteDirectory(
                            worldPath
                    );
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
                        World newWorld =
                                Bukkit.getWorld(worldName);

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
        World fallback =
                Bukkit.getWorld(
                        config.getFallbackWorld()
                );

        if (fallback == null) {
            fallback =
                    Bukkit.getWorlds().get(0);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld()
                    .getName()
                    .equalsIgnoreCase(worldName)) {
                player.teleport(
                        fallback.getSpawnLocation()
                );

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
