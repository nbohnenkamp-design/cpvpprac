package me.liass.cpvpsinglebiome.reset;

import java.io.IOException;
import java.lang.reflect.Method;
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
import java.util.function.Consumer;

import me.liass.cpvpsinglebiome.CPVPSingleBiomePlugin;
import me.liass.cpvpsinglebiome.chunky.ChunkyIntegration;
import me.liass.cpvpsinglebiome.config.ConfigManager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetManager {

    private final CPVPSingleBiomePlugin plugin;
    private final ConfigManager config;

    private boolean resetInProgress = false;
    private boolean waitingForChunky = false;

    private int schedulerTaskId = -1;
    private int chunkyWatchdogTaskId = -1;

    private LocalDate lastAutoResetDate = null;
    private LocalDateTime nextResetAt = null;

    private final Set<Integer> sentWarnings = new HashSet<>();

    private final Set<String> pendingChunkyWorlds = new HashSet<>();
    private final Set<String> startedChunkyWorlds = new HashSet<>();

    private boolean chunkyCompletionHookRegistered = false;

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

        registerChunkyCompletionHook();

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

        stopChunkyWatchdog();

        this.resetInProgress = false;
        this.waitingForChunky = false;
        this.pendingChunkyWorlds.clear();
        this.startedChunkyWorlds.clear();
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

        return this.resetInProgress || this.waitingForChunky;
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
        if (this.resetInProgress || this.waitingForChunky) {
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
        if (this.resetInProgress || this.waitingForChunky) {
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
            onWorldResetPhaseComplete(sender);
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
        if (this.resetInProgress || this.waitingForChunky) {
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

                onWorldResetPhaseComplete(sender);
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
        if (this.resetInProgress || this.waitingForChunky) {
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

        onWorldResetPhaseComplete(
                Bukkit.getConsoleSender()
        );
    }

    private void beginMaintenance() {
        this.resetInProgress = true;
        this.waitingForChunky = false;

        this.pendingChunkyWorlds.clear();
        this.startedChunkyWorlds.clear();

        stopChunkyWatchdog();

        if (config.isBlockJoinsDuringReset()) {
            plugin.getLogger().info(
                    "Join guard enabled: normal players are blocked during arena reset."
            );
        }
    }

    private void onWorldResetPhaseComplete(CommandSender sender) {
        this.resetInProgress = false;

        if (config.isChunkyEnabled()
                && !this.pendingChunkyWorlds.isEmpty()) {
            this.waitingForChunky = true;

            sender.sendMessage(
                    config.getPrefix()
                            + "World reset complete. Waiting for Chunky to finish..."
            );

            plugin.getLogger().info(
                    "Arena reset world phase complete. Waiting for Chunky worlds: "
                            + String.join(", ", this.pendingChunkyWorlds)
            );

            registerChunkyCompletionHook();
            startChunkyWatchdog();

            return;
        }

        finishMaintenance(sender);
    }

    private void finishMaintenance(CommandSender sender) {
        if (!this.resetInProgress
                && !this.waitingForChunky
                && this.pendingChunkyWorlds.isEmpty()) {
            return;
        }

        this.resetInProgress = false;
        this.waitingForChunky = false;

        this.pendingChunkyWorlds.clear();
        this.startedChunkyWorlds.clear();

        stopChunkyWatchdog();

        if (config.isMaintenanceDuringReset()) {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    config.getMaintenanceCommandOff()
            );
        }

        plugin.getLogger().info(
                "Arena reset and Chunky pre-generation complete. Join guard disabled."
        );

        sender.sendMessage(
                config.getPrefix()
                        + "Arena reset complete."
        );
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
            String key =
                    normalizeWorldName(worldName);

            this.pendingChunkyWorlds.add(key);

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> startChunkyForWorld(worldName),
                    20L * config.getChunkyDelaySeconds()
            );
        }
    }

    private void startChunkyForWorld(String worldName) {
        String key =
                normalizeWorldName(worldName);

        World newWorld =
                Bukkit.getWorld(worldName);

        if (newWorld == null) {
            plugin.getLogger().warning(
                    "Chunky start skipped: world is not loaded: "
                            + worldName
            );

            markChunkyWorldComplete(worldName);
            return;
        }

        this.startedChunkyWorlds.add(key);

        boolean ok =
                ChunkyIntegration.start(
                        plugin,
                        newWorld,
                        config
                );

        if (!ok) {
            plugin.getLogger().warning(
                    "Chunky could not be started for world: "
                            + worldName
            );

            markChunkyWorldComplete(worldName);
        }
    }

    private void registerChunkyCompletionHook() {
        if (this.chunkyCompletionHookRegistered) {
            return;
        }

        try {
            Class<?> apiClass =
                    Class.forName(
                            "org.popcraft.chunky.api.ChunkyAPI"
                    );

            Object api =
                    Bukkit.getServicesManager()
                            .load(apiClass);

            if (api == null) {
                plugin.getLogger().warning(
                        "Chunky API service not available yet. Completion hook not registered."
                );
                return;
            }

            Method method =
                    apiClass.getMethod(
                            "onGenerationComplete",
                            Consumer.class
                    );

            Consumer<Object> listener =
                    this::handleChunkyGenerationCompleteEvent;

            method.invoke(
                    api,
                    listener
            );

            this.chunkyCompletionHookRegistered = true;

            plugin.getLogger().info(
                    "Chunky completion hook registered."
            );

        } catch (Throwable t) {
            plugin.getLogger().warning(
                    "Could not register Chunky completion hook: "
                            + t.getClass().getSimpleName()
                            + " - "
                            + t.getMessage()
            );
        }
    }

    private void handleChunkyGenerationCompleteEvent(Object event) {
        String worldName =
                readWorldFromChunkyEvent(event);

        if (worldName == null || worldName.isBlank()) {
            return;
        }

        Bukkit.getScheduler().runTask(
                plugin,
                () -> markChunkyWorldComplete(worldName)
        );
    }

    private String readWorldFromChunkyEvent(Object event) {
        if (event == null) {
            return null;
        }

        try {
            Method worldMethod =
                    event.getClass().getMethod("world");

            Object value =
                    worldMethod.invoke(event);

            return value == null
                    ? null
                    : value.toString();

        } catch (Throwable t) {
            plugin.getLogger().warning(
                    "Could not read Chunky completion event world: "
                            + t.getClass().getSimpleName()
                            + " - "
                            + t.getMessage()
            );

            return null;
        }
    }

    private void markChunkyWorldComplete(String worldName) {
        String key =
                normalizeWorldName(worldName);

        boolean removed =
                this.pendingChunkyWorlds.remove(key);

        this.startedChunkyWorlds.remove(key);

        if (removed) {
            plugin.getLogger().info(
                    "Chunky finished for reset world: "
                            + worldName
                            + ". Remaining: "
                            + this.pendingChunkyWorlds.size()
            );
        }

        if (this.waitingForChunky
                && this.pendingChunkyWorlds.isEmpty()) {
            finishMaintenance(
                    Bukkit.getConsoleSender()
            );
        }
    }

    private void startChunkyWatchdog() {
        stopChunkyWatchdog();

        this.chunkyWatchdogTaskId =
                Bukkit.getScheduler().scheduleSyncRepeatingTask(
                        plugin,
                        this::checkChunkyRunningState,
                        20L * 30L,
                        20L * 30L
                );
    }

    private void stopChunkyWatchdog() {
        if (this.chunkyWatchdogTaskId != -1) {
            Bukkit.getScheduler().cancelTask(
                    this.chunkyWatchdogTaskId
            );

            this.chunkyWatchdogTaskId = -1;
        }
    }

    private void checkChunkyRunningState() {
        if (!this.waitingForChunky) {
            return;
        }

        if (this.pendingChunkyWorlds.isEmpty()) {
            finishMaintenance(
                    Bukkit.getConsoleSender()
            );
            return;
        }

        Set<String> copy =
                new HashSet<>(
                        this.pendingChunkyWorlds
                );

        for (String worldName : copy) {
            if (!this.startedChunkyWorlds.contains(worldName)) {
                continue;
            }

            Boolean running =
                    isChunkyRunning(worldName);

            if (Boolean.FALSE.equals(running)) {
                markChunkyWorldComplete(worldName);
            }
        }
    }

    private Boolean isChunkyRunning(String worldName) {
        try {
            Class<?> apiClass =
                    Class.forName(
                            "org.popcraft.chunky.api.ChunkyAPI"
                    );

            Object api =
                    Bukkit.getServicesManager()
                            .load(apiClass);

            if (api == null) {
                return null;
            }

            Method method =
                    apiClass.getMethod(
                            "isRunning",
                            String.class
                    );

            Object value =
                    method.invoke(
                            api,
                            worldName
                    );

            if (value instanceof Boolean b) {
                return b;
            }

            return null;

        } catch (Throwable t) {
            plugin.getLogger().warning(
                    "Could not query Chunky running state for '"
                            + worldName
                            + "': "
                            + t.getClass().getSimpleName()
                            + " - "
                            + t.getMessage()
            );

            return null;
        }
    }

    private String normalizeWorldName(String worldName) {
        return worldName == null
                ? ""
                : worldName.toLowerCase();
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
