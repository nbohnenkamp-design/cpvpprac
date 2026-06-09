package me.liass.cpvpsinglebiome.reset;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
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

    private LocalDate lastResetDate = null;
    private LocalDateTime nextResetAt = null;

    private final Set<Integer> sentWarnings = new HashSet<>();

    private static final int MAX_WORLD_LOAD_WAIT_SECONDS = 90;
    private static final int MAX_WORLD_SETTINGS_WAIT_SECONDS = 90;
    private static final int MAX_WORLD_FOLDER_DELETE_WAIT_SECONDS = 60;

    private final Queue<String> chunkyQueue = new ArrayDeque<>();
    private final Map<String, Integer> chunkyWorldLoadRetries = new HashMap<>();
    private String activeChunkyWorld = null;

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
        this.lastResetDate = config.getLastResetDate();

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
                "Automatic arena reset enabled. Last full reset: "
                        + (
                        this.lastResetDate != null
                                ? this.lastResetDate
                                : "not recorded"
                )
                        + ". Next reset: "
                        + this.nextResetAt
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

        this.chunkyQueue.clear();
        this.chunkyWorldLoadRetries.clear();
        this.activeChunkyWorld = null;
    }

    public void reload() {
        config.reload();
        start();
    }

    public boolean isResetInProgress() {
        return this.resetInProgress;
    }

    public boolean isMaintenanceActive() {
        if (this.resetInProgress) {
            return config.isBlockJoinsDuringReset();
        }

        if (this.waitingForChunky) {
            return config.isBlockJoinsDuringChunky();
        }

        return false;
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
        return this.lastResetDate;
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
        }
    }

    private void sendWarningsIfNeeded(long minutesLeft) {
        if (!config.isWarningEnabled()) {
            return;
        }

        /*
         * If skip-if-any-player-online is enabled, warning countdowns are suppressed.
         *
         * Reason:
         * With this mode enabled, the reset will not start while normal players are online.
         * Sending "reset in 5 minutes" or "reset in 1 minute" would be misleading and
         * creates repeated chat/log spam during postponed resets.
         *
         * Countdown warnings are only useful when the reset is allowed to run even while
         * players are online.
         */
        if (config.isSkipIfAnyPlayerOnline()) {
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

        LocalDate lastDate =
                this.lastResetDate;

        LocalDateTime candidate;

        if (lastDate != null) {
            candidate =
                    lastDate
                            .plusDays(
                                    config.getResetIntervalDays()
                            )
                            .atTime(
                                    config.getResetTime()
                            );

            while (!candidate.isAfter(now)) {
                candidate =
                        candidate.plusDays(
                                config.getResetIntervalDays()
                        );
            }

        } else {
            candidate =
                    now.toLocalDate().atTime(
                            config.getResetTime()
                    );

            if (!candidate.isAfter(now)) {
                candidate =
                        candidate.plusDays(
                                config.getResetIntervalDays()
                        );
            }
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

        markFullResetDate();

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
            onWorldResetPhaseComplete(
                    sender,
                    worlds
            );
            return;
        }

        ResetWorldSpec spec =
                worlds.get(index);

        resetWorldInternal(
                spec.worldName(),
                spec.biomeName(),
                sender,
                success -> Bukkit.getScheduler().runTaskLater(
                        plugin,
                        () -> resetNext(
                                sender,
                                worlds,
                                index + 1
                        ),
                        20L * 2L
                )
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
                        sender,
                        success -> onWorldResetPhaseComplete(
                                sender,
                                List.of(spec)
                        )
                );
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
                Bukkit.getConsoleSender(),
                success -> onWorldResetPhaseComplete(
                        Bukkit.getConsoleSender(),
                        List.of(
                                new ResetWorldSpec(
                                        worldName,
                                        biomeName
                                )
                        )
                )
        );
    }

    private void beginMaintenance() {
        this.resetInProgress = true;
        this.waitingForChunky = false;

        this.chunkyQueue.clear();
        this.chunkyWorldLoadRetries.clear();
        this.activeChunkyWorld = null;

        stopChunkyWatchdog();

        if (config.isBlockJoinsDuringReset()) {
            plugin.getLogger().info(
                    "Join guard enabled: normal players are blocked during arena reset phase."
            );
        }
    }

    private void markFullResetDate() {
        LocalDate date =
                LocalDate.now(
                        config.getResetZone()
                );

        this.lastResetDate =
                date;

        config.setLastResetDate(
                date
        );

        calculateNextReset();

        plugin.getLogger().info(
                "Stored last full reset date: "
                        + date
                        + ". Next reset: "
                        + this.nextResetAt
        );
    }

    private void onWorldResetPhaseComplete(
            CommandSender sender,
            List<ResetWorldSpec> resetWorlds
    ) {
        this.resetInProgress = false;

        if (!config.isChunkyEnabled()) {
            finishMaintenance(sender);
            return;
        }

        if (!ChunkyIntegration.isAvailable()) {
            plugin.getLogger().warning(
                    "Chunky is not installed. Join guard will be disabled after world reset phase."
            );
            finishMaintenance(sender);
            return;
        }

        this.chunkyQueue.clear();

        for (ResetWorldSpec spec : resetWorlds) {
            this.chunkyQueue.add(spec.worldName());
        }

        if (this.chunkyQueue.isEmpty()) {
            finishMaintenance(sender);
            return;
        }

        this.waitingForChunky = true;

        sender.sendMessage(
                config.getPrefix()
                        + "World reset complete. Starting Chunky one world at a time..."
        );

        plugin.getLogger().info(
                "Arena reset world phase complete. Chunky queue: "
                        + String.join(", ", this.chunkyQueue)
        );

        if (config.isBlockJoinsDuringChunky()) {
            plugin.getLogger().info(
                    "Join guard remains enabled during Chunky pre-generation."
            );
        } else {
            plugin.getLogger().info(
                    "Join guard disabled during Chunky pre-generation. Normal players may join."
            );
        }

        registerChunkyCompletionHook();
        startChunkyWatchdog();

        startNextChunkyWorld();
    }

    private void startNextChunkyWorld() {
        if (!this.waitingForChunky) {
            return;
        }

        if (this.activeChunkyWorld != null) {
            return;
        }

        String worldName =
                this.chunkyQueue.peek();

        if (worldName == null) {
            finishMaintenance(
                    Bukkit.getConsoleSender()
            );
            return;
        }

        World world =
                Bukkit.getWorld(worldName);

        if (world == null) {
            int retries =
                    this.chunkyWorldLoadRetries.getOrDefault(
                            normalizeWorldName(worldName),
                            0
                    ) + 1;

            this.chunkyWorldLoadRetries.put(
                    normalizeWorldName(worldName),
                    retries
            );

            if (retries == 1 || retries % 5 == 0) {
                plugin.getLogger().info(
                        "Chunky waiting for reset world to load: "
                                + worldName
                                + " ("
                                + retries
                                + "/"
                                + MAX_WORLD_LOAD_WAIT_SECONDS
                                + "s)"
                );
            }

            if (retries >= MAX_WORLD_LOAD_WAIT_SECONDS) {
                plugin.getLogger().warning(
                        "Chunky start skipped after waiting "
                                + MAX_WORLD_LOAD_WAIT_SECONDS
                                + "s: world is still not loaded: "
                                + worldName
                );

                this.chunkyQueue.poll();
                this.chunkyWorldLoadRetries.remove(
                        normalizeWorldName(worldName)
                );

                Bukkit.getScheduler().runTaskLater(
                        plugin,
                        this::startNextChunkyWorld,
                        20L
                );
                return;
            }

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    this::startNextChunkyWorld,
                    20L
            );
            return;
        }

        applyConfiguredWorldSettings(world);

        this.chunkyQueue.poll();
        this.chunkyWorldLoadRetries.remove(
                normalizeWorldName(worldName)
        );

        this.activeChunkyWorld =
                worldName;

        plugin.getLogger().info(
                "Starting Chunky for reset world: "
                        + worldName
                        + ". Remaining queue after this: "
                        + this.chunkyQueue.size()
        );

        boolean ok =
                ChunkyIntegration.start(
                        plugin,
                        world,
                        config
                );

        if (!ok) {
            plugin.getLogger().warning(
                    "Chunky could not be started for world: "
                            + worldName
            );

            this.activeChunkyWorld = null;

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    this::startNextChunkyWorld,
                    20L
            );
        }
    }

    private void finishMaintenance(CommandSender sender) {
        boolean wasActive =
                this.resetInProgress
                        || this.waitingForChunky
                        || this.activeChunkyWorld != null
                        || !this.chunkyQueue.isEmpty();

        this.resetInProgress = false;
        this.waitingForChunky = false;

        this.chunkyQueue.clear();
        this.chunkyWorldLoadRetries.clear();
        this.activeChunkyWorld = null;

        stopChunkyWatchdog();

        if (config.isMaintenanceDuringReset()) {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    config.getMaintenanceCommandOff()
            );
        }

        if (wasActive) {
            plugin.getLogger().info(
                    "Arena reset and Chunky pre-generation complete."
            );

            sender.sendMessage(
                    config.getPrefix()
                            + "Arena reset complete."
            );
        }
    }

    private void resetWorldInternal(
            String worldName,
            String biomeName,
            CommandSender sender,
            Consumer<Boolean> completion
    ) {
        if (worldName == null || worldName.isBlank()) {
            sender.sendMessage(
                    config.getPrefix()
                            + "Invalid world name."
            );
            completion.accept(false);
            return;
        }

        if (worldName.equalsIgnoreCase("world")) {
            sender.sendMessage(
                    config.getPrefix()
                            + "Refusing to reset main world: "
                            + worldName
            );
            completion.accept(false);
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

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "mv remove " + worldName
        );

        World world =
                Bukkit.getWorld(worldName);

        if (world != null) {
            Bukkit.unloadWorld(
                    world,
                    false
            );
        }

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> prepareWorldFolderAndCreate(
                        worldName,
                        biomeName,
                        sender,
                        0,
                        completion
                ),
                20L
        );
    }

    private void prepareWorldFolderAndCreate(
            String worldName,
            String biomeName,
            CommandSender sender,
            int attempt,
            Consumer<Boolean> completion
    ) {
        World loadedWorld =
                Bukkit.getWorld(worldName);

        if (loadedWorld != null) {
            Bukkit.unloadWorld(
                    loadedWorld,
                    false
            );

            if (attempt >= MAX_WORLD_FOLDER_DELETE_WAIT_SECONDS) {
                sender.sendMessage(
                        config.getPrefix()
                                + "Could not unload world before reset: "
                                + worldName
                );
                completion.accept(false);
                return;
            }

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> prepareWorldFolderAndCreate(
                            worldName,
                            biomeName,
                            sender,
                            attempt + 1,
                            completion
                    ),
                    20L
            );
            return;
        }

        Path serverRoot =
                Bukkit.getWorldContainer()
                        .toPath();

        Path legacyWorldPath =
                serverRoot.resolve(worldName);

        Path dimensionWorldPath =
                serverRoot
                        .resolve("world")
                        .resolve("dimensions")
                        .resolve("minecraft")
                        .resolve(worldName);

        try {
            if (Files.exists(dimensionWorldPath)) {
                plugin.getLogger().info(
                        "Deleting Paper dimension world folder before reset: "
                                + dimensionWorldPath.toAbsolutePath()
                );

                deleteDirectory(dimensionWorldPath);
            }

            if (Files.exists(legacyWorldPath)) {
                if (config.isBackupOldWorlds()) {
                    Path backupPath =
                            serverRoot.resolve(
                                    worldName
                                            + "_old_"
                                            + System.currentTimeMillis()
                            );

                    Files.move(
                            legacyWorldPath,
                            backupPath
                    );

                    if (config.isDeleteOldWorldsAfterSuccess()) {
                        deleteDirectory(
                                backupPath
                        );
                    }

                } else {
                    deleteDirectory(
                            legacyWorldPath
                    );
                }
            }

        } catch (IOException | RuntimeException e) {
            if (attempt >= MAX_WORLD_FOLDER_DELETE_WAIT_SECONDS) {
                sender.sendMessage(
                        config.getPrefix()
                                + "Could not delete old world data after waiting "
                                + MAX_WORLD_FOLDER_DELETE_WAIT_SECONDS
                                + "s: "
                                + e.getMessage()
                );
                completion.accept(false);
                return;
            }

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> prepareWorldFolderAndCreate(
                            worldName,
                            biomeName,
                            sender,
                            attempt + 1,
                            completion
                    ),
                    20L
            );
            return;
        }

        if (Files.exists(legacyWorldPath) || Files.exists(dimensionWorldPath)) {
            if (attempt >= MAX_WORLD_FOLDER_DELETE_WAIT_SECONDS) {
                sender.sendMessage(
                        config.getPrefix()
                                + "World folder still exists after reset preparation: "
                                + worldName
                );
                completion.accept(false);
                return;
            }

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> prepareWorldFolderAndCreate(
                            worldName,
                            biomeName,
                            sender,
                            attempt + 1,
                            completion
                    ),
                    20L
            );
            return;
        }

        plugin.getLogger().info(
                "World folder prepared. Creating reset world '"
                        + worldName
                        + "' with generator CPVPSingleBiome:"
                        + biomeName
        );

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "mv create "
                        + worldName
                        + " normal -g CPVPSingleBiome:"
                        + biomeName
        );

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> ensureWorldSettingsApplied(
                        worldName,
                        0,
                        completion
                ),
                20L
        );
    }

    private void ensureWorldSettingsApplied(
            String worldName,
            int attempt,
            Consumer<Boolean> completion
    ) {
        World world =
                Bukkit.getWorld(worldName);

        if (world != null) {
            applyConfiguredWorldSettings(world);
            completion.accept(true);
            return;
        }

        if (attempt >= MAX_WORLD_SETTINGS_WAIT_SECONDS) {
            plugin.getLogger().warning(
                    "Could not apply world settings after reset. World is still not loaded: "
                            + worldName
            );
            completion.accept(false);
            return;
        }

        if (attempt == 0 || attempt % 5 == 0) {
            plugin.getLogger().info(
                    "Waiting to apply world settings after reset: "
                            + worldName
                            + " ("
                            + attempt
                            + "/"
                            + MAX_WORLD_SETTINGS_WAIT_SECONDS
                            + "s)"
            );
        }

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> ensureWorldSettingsApplied(
                        worldName,
                        attempt + 1,
                        completion
                ),
                20L
        );
    }

    private void applyConfiguredWorldSettings(World world) {
        if (world == null) {
            return;
        }

        world.setDifficulty(
                config.getResetDifficulty()
        );

        world.setPVP(
                config.isResetPvpEnabled()
        );

        applyConfiguredWorldBorder(world);

        applyMultiverseWorldSettings(
                world.getName()
        );

        plugin.getLogger().info(
                "Applied world settings to '"
                        + world.getName()
                        + "': difficulty="
                        + config.getResetDifficulty()
                        + ", gamemode="
                        + config.getResetGameMode()
                        + ", pvp="
                        + config.isResetPvpEnabled()
                        + ", border="
                        + config.getWorldBorderSize()
        );
    }

    private void applyConfiguredWorldBorder(World world) {
        if (world == null) {
            return;
        }

        double borderSize =
                config.getWorldBorderSize();

        if (borderSize <= 0.0D) {
            plugin.getLogger().info(
                    "World border disabled for '"
                            + world.getName()
                            + "'."
            );
            return;
        }

        world.getWorldBorder().setCenter(
                world.getSpawnLocation()
        );

        world.getWorldBorder().setSize(
                borderSize
        );

        plugin.getLogger().info(
                "Applied world border to '"
                        + world.getName()
                        + "': center=spawn, size="
                        + borderSize
        );
    }

    private void applyMultiverseWorldSettings(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return;
        }

        org.bukkit.plugin.Plugin multiverse =
                Bukkit.getPluginManager().getPlugin(
                        "Multiverse-Core"
                );

        if (multiverse == null || !multiverse.isEnabled()) {
            plugin.getLogger().warning(
                    "Multiverse-Core is not available. Bukkit world settings were applied, but Multiverse world settings were not updated for: "
                            + worldName
            );
            return;
        }

        String difficulty =
                config.getResetDifficulty()
                        .name()
                        .toLowerCase(Locale.ROOT);

        String gameMode =
                config.getResetGameMode()
                        .name()
                        .toLowerCase(Locale.ROOT);

        String pvp =
                Boolean.toString(
                        config.isResetPvpEnabled()
                );

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "mv modify " + worldName + " set difficulty " + difficulty
        );

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "mv modify " + worldName + " set gamemode " + gameMode
        );

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "mv modify " + worldName + " set pvp " + pvp
        );
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
                () -> markActiveChunkyWorldComplete(worldName)
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

    private void markActiveChunkyWorldComplete(String worldName) {
        if (!this.waitingForChunky) {
            return;
        }

        if (this.activeChunkyWorld == null) {
            return;
        }

        if (!normalizeWorldName(this.activeChunkyWorld)
                .equals(normalizeWorldName(worldName))) {
            return;
        }

        plugin.getLogger().info(
                "Chunky finished for reset world: "
                        + this.activeChunkyWorld
                        + ". Remaining queue: "
                        + this.chunkyQueue.size()
        );

        this.activeChunkyWorld = null;

        startNextChunkyWorld();
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

        if (this.activeChunkyWorld == null) {
            if (this.chunkyQueue.isEmpty()) {
                finishMaintenance(
                        Bukkit.getConsoleSender()
                );
            } else {
                startNextChunkyWorld();
            }

            return;
        }

        Boolean running =
                isChunkyRunning(this.activeChunkyWorld);

        if (Boolean.FALSE.equals(running)) {
            plugin.getLogger().info(
                    "Chunky watchdog detected completion for reset world: "
                            + this.activeChunkyWorld
            );

            String completed =
                    this.activeChunkyWorld;

            this.activeChunkyWorld = null;

            markActiveChunkyWorldComplete(completed);
            startNextChunkyWorld();
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
                : worldName.toLowerCase(Locale.ROOT);
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
