package me.liass.cpvpsinglebiome.command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.liass.cpvpsinglebiome.CPVPSingleBiomePlugin;
import me.liass.cpvpsinglebiome.chunky.ChunkyIntegration;
import me.liass.cpvpsinglebiome.config.ConfigManager;
import me.liass.cpvpsinglebiome.generator.BiomeType;
import me.liass.cpvpsinglebiome.generator.SingleBiomeChunkGenerator;
import me.liass.cpvpsinglebiome.listener.WorldInitListener;
import me.liass.cpvpsinglebiome.reset.ResetManager;
import me.liass.cpvpsinglebiome.reset.ResetWorldSpec;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

public class CPVPSBCommand implements CommandExecutor {

    private static final String HEADER =
            "§5§l━━━━━━━━━━━━━━ §dCPVPSingleBiome §5§l━━━━━━━━━━━━━━";

    private static final String FOOTER =
            "§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

    private static final DateTimeFormatter EXPORT_FILE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final CPVPSingleBiomePlugin plugin;
    private final ConfigManager config;
    private final ResetManager resetManager;

    public CPVPSBCommand(
            CPVPSingleBiomePlugin plugin,
            ConfigManager config,
            ResetManager resetManager
    ) {
        this.plugin = plugin;
        this.config = config;
        this.resetManager = resetManager;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "help":
                sendHelp(sender);
                return true;

            case "version":
                handleVersion(sender);
                return true;

            case "about":
                handleAbout(sender);
                return true;

            case "status":
                handleStatus(sender);
                return true;

            case "export":
                handleExport(sender);
                return true;

            case "reload":
                handleReload(sender);
                return true;

            case "biomes":
                handleBiomes(sender);
                return true;

            case "info":
                handleInfo(sender);
                return true;

            case "create":
                handleCreate(sender, args);
                return true;

            case "tp":
                handleTp(sender, args);
                return true;

            case "reset":
                handleReset(sender, args);
                return true;

            case "chunky":
                handleChunky(sender, args);
                return true;

            default:
                sender.sendMessage(
                        config.getPrefix()
                                + "§cUnknown subcommand. Use §f/cpvpsb help§c."
                );
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(HEADER);
        sender.sendMessage("§d  /cpvpsb help                   §f» Show this help");
        sender.sendMessage("§d  /cpvpsb version                §f» Show plugin and server version");
        sender.sendMessage("§d  /cpvpsb about                  §f» Show project information");
        sender.sendMessage("§d  /cpvpsb status                 §f» Show operational status");
        sender.sendMessage("§d  /cpvpsb export                 §f» Export diagnostic report");
        sender.sendMessage("§d  /cpvpsb reload                 §f» Reload config.yml");
        sender.sendMessage("§d  /cpvpsb biomes                 §f» List available biomes");
        sender.sendMessage("§d  /cpvpsb info                   §f» Show generator configuration");
        sender.sendMessage("§d  /cpvpsb create <world> [biome] §f» Create a single-biome world");
        sender.sendMessage("§d  /cpvpsb tp <world>             §f» Teleport to a world");
        sender.sendMessage("§d  /cpvpsb reset now              §f» Reset all configured arenas now");
        sender.sendMessage("§d  /cpvpsb reset <world>          §f» Reset one configured arena");
        sender.sendMessage("§d  /cpvpsb reset status           §f» Show detailed reset/chunky configuration");
        sender.sendMessage("§d  /cpvpsb reset reload           §f» Reload reset scheduler/config");
        sender.sendMessage("§d  /cpvpsb chunky start <world>   §f» Start Chunky for one world");
        sender.sendMessage("§d  /cpvpsb chunky start-all       §f» Start Chunky for all reset worlds");
        sender.sendMessage("");
        sender.sendMessage("§5  Available biomes: §f" + BiomeType.getNames());
        sender.sendMessage("§5  Multiverse syntax: §f/mv create <world> normal -g CPVPSingleBiome:<biome>");
        sender.sendMessage(FOOTER);
    }

    private void handleVersion(CommandSender sender) {
        if (!hasPermission(sender, "cpvpsinglebiome.version")) {
            return;
        }

        sender.sendMessage(HEADER);
        sender.sendMessage("§d  Plugin:          §fCPVPSingleBiome");
        sender.sendMessage("§d  Plugin version:  §f" + plugin.getDescription().getVersion());
        sender.sendMessage("§d  Server software: §f" + Bukkit.getName());
        sender.sendMessage("§d  Server version:  §f" + Bukkit.getVersion());
        sender.sendMessage("§d  Bukkit version:  §f" + Bukkit.getBukkitVersion());
        sender.sendMessage("§d  Java version:    §f" + System.getProperty("java.version"));
        sender.sendMessage(FOOTER);
    }

    private void handleAbout(CommandSender sender) {
        if (!hasPermission(sender, "cpvpsinglebiome.about")) {
            return;
        }

        sender.sendMessage(HEADER);
        sender.sendMessage("§d  CPVPSingleBiome");
        sender.sendMessage("§f  Crystal PvP focused single-biome terrain generator.");
        sender.sendMessage("");
        sender.sendMessage("§d  Purpose: §fOpen, readable, PvP-friendly arena worlds");
        sender.sendMessage("§d  Server:  §fcpvpprac.eu");
        sender.sendMessage("§d  Author:  §fCPVPPRAC");
        sender.sendMessage("§d  License: §fSee LICENSE file");
        sender.sendMessage("");
        sender.sendMessage("§7  This plugin is not affiliated with Mojang, Microsoft, Minecraft or PaperMC.");
        sender.sendMessage(FOOTER);
    }

    private void handleStatus(CommandSender sender) {
        if (!hasPermission(sender, "cpvpsinglebiome.status")) {
            return;
        }

        sender.sendMessage(HEADER);
        sender.sendMessage("§d  Plugin version:     §f" + plugin.getDescription().getVersion());
        sender.sendMessage("§d  Server:             §f" + Bukkit.getName() + " " + Bukkit.getBukkitVersion());
        sender.sendMessage("§d  Loaded worlds:      §f" + Bukkit.getWorlds().size());
        sender.sendMessage("§d  Configured arenas:  §f" + config.getResetWorlds().size());
        sender.sendMessage("§d  Available biomes:   §f" + BiomeType.getNames());

        sender.sendMessage(
                "§d  Default biome:      §f"
                        + config.getDefaultBiome()
        );

        sender.sendMessage(
                "§d  World border:       §f"
                        + (
                        config.getWorldBorderSize() > 0.0D
                                ? config.getWorldBorderSize() + " blocks"
                                : "disabled"
                )
        );

        sender.sendMessage(
                "§d  Decorations:        §f"
                        + (
                        config.isDecorationEnabled()
                                ? "enabled, density=" + config.getDecorationDensity()
                                : "disabled"
                )
        );

        sender.sendMessage(
                "§d  Auto-reset:         §f"
                        + (
                        config.isResetEnabled()
                                ? "enabled @ " + config.getResetTime() + " " + config.getResetZone().getId()
                                : "disabled"
                )
        );

        sender.sendMessage(
                "§d  Reset in progress:  §f"
                        + (
                        resetManager != null
                                ? resetManager.isResetInProgress()
                                : "reset manager unavailable"
                )
        );

        LocalDateTime nextReset =
                resetManager != null
                        ? resetManager.getNextResetAt()
                        : null;

        sender.sendMessage(
                "§d  Next auto reset:    §f"
                        + (
                        nextReset != null
                                ? nextReset.toString()
                                : "—"
                )
        );

        sender.sendMessage(
                "§d  Chunky:             §f"
                        + (
                        config.isChunkyEnabled()
                                ? "enabled"
                                : "disabled"
                )
                        + " §7(installed: "
                        + ChunkyIntegration.isAvailable()
                        + ")"
        );

        sender.sendMessage("");
        sender.sendMessage("§7  Use §f/cpvpsb reset status §7for detailed reset settings.");
        sender.sendMessage("§7  Use §f/cpvpsb export §7to write a diagnostic report.");
        sender.sendMessage(FOOTER);
    }

    private void handleExport(CommandSender sender) {
        if (!hasPermission(sender, "cpvpsinglebiome.export")) {
            return;
        }

        try {
            Path exportDir =
                    plugin.getDataFolder()
                            .toPath()
                            .resolve("exports");

            Files.createDirectories(exportDir);

            String timestamp =
                    LocalDateTime.now()
                            .format(EXPORT_FILE_TIME);

            Path exportFile =
                    exportDir.resolve(
                            "cpvpsinglebiome-export-" + timestamp + ".txt"
                    );

            String report =
                    buildExportReport(sender);

            Files.writeString(
                    exportFile,
                    report,
                    StandardCharsets.UTF_8
            );

            sender.sendMessage(
                    config.getPrefix()
                            + "§aExport written to:"
            );

            sender.sendMessage(
                    "§f" + exportFile.toAbsolutePath()
            );

            sender.sendMessage(
                    config.getPrefix()
                            + "§eReview this file before sharing it publicly."
            );

        } catch (IOException e) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cExport failed: §f"
                            + e.getMessage()
            );

            plugin.getLogger().warning(
                    "Export failed: " + e
            );
        }
    }

    private String buildExportReport(CommandSender sender) throws IOException {
        StringBuilder builder = new StringBuilder();

        appendLine(builder, "CPVPSingleBiome Diagnostic Export");
        appendLine(builder, "Generated at: " + ZonedDateTime.now());
        appendLine(builder, "Requested by: " + sender.getName());
        appendLine(builder, "");

        appendSection(builder, "Plugin");
        appendLine(builder, "Name: CPVPSingleBiome");
        appendLine(builder, "Version: " + plugin.getDescription().getVersion());
        appendLine(builder, "Main class: " + plugin.getDescription().getMain());
        appendLine(builder, "Authors: " + plugin.getDescription().getAuthors());
        appendLine(builder, "");

        appendSection(builder, "Server");
        appendLine(builder, "Server software: " + Bukkit.getName());
        appendLine(builder, "Server version: " + Bukkit.getVersion());
        appendLine(builder, "Bukkit version: " + Bukkit.getBukkitVersion());
        appendLine(builder, "Java version: " + System.getProperty("java.version"));
        appendLine(builder, "Online players: " + Bukkit.getOnlinePlayers().size());
        appendLine(builder, "");

        appendSection(builder, "Available Biomes");
        for (BiomeType biomeType : BiomeType.values()) {
            appendLine(builder, "- " + biomeType.getId());
        }
        appendLine(builder, "");

        appendSection(builder, "Generator Configuration");
        appendLine(builder, "Default biome: " + config.getDefaultBiome());
        appendLine(builder, "Base height: " + config.getBaseHeight());
        appendLine(builder, "Height variation: " + config.getHeightVariation());
        appendLine(builder, "Noise scale: " + config.getNoiseScale());
        appendLine(builder, "Flatness: " + config.getFlatness());
        appendLine(builder, "Decorations enabled: " + config.isDecorationEnabled());
        appendLine(builder, "Decoration density: " + config.getDecorationDensity());
        appendLine(builder, "");

        appendSection(builder, "World Border Configuration");
        appendLine(builder, "Configured world border size: " + config.getWorldBorderSize());
        appendLine(builder, "");

        appendSection(builder, "Reset Configuration");
        appendLine(builder, "Reset enabled: " + config.isResetEnabled());
        appendLine(builder, "Reset time: " + config.getResetTime());
        appendLine(builder, "Reset zone: " + config.getResetZone().getId());
        appendLine(builder, "Reset interval days: " + config.getResetIntervalDays());
        appendLine(builder, "Reset difficulty: " + config.getResetDifficulty());
        appendLine(builder, "Reset game mode: " + config.getResetGameMode());
        appendLine(builder, "Reset PvP enabled: " + config.isResetPvpEnabled());
        appendLine(builder, "Fallback world: " + config.getFallbackWorld());
        appendLine(builder, "Skip if any player online: " + config.isSkipIfAnyPlayerOnline());
        appendLine(builder, "Warnings enabled: " + config.isWarningEnabled());
        appendLine(builder, "Warning minutes: " + config.getWarningMinutes());
        appendLine(builder, "Backup old worlds: " + config.isBackupOldWorlds());
        appendLine(builder, "Delete old worlds after success: " + config.isDeleteOldWorldsAfterSuccess());

        if (resetManager != null) {
            appendLine(builder, "Reset in progress: " + resetManager.isResetInProgress());

            LocalDate lastReset =
                    resetManager.getLastAutoResetDate();

            appendLine(
                    builder,
                    "Last auto reset: "
                            + (
                            lastReset != null
                                    ? lastReset
                                    : "never"
                    )
            );

            LocalDateTime nextReset =
                    resetManager.getNextResetAt();

            appendLine(
                    builder,
                    "Next auto reset: "
                            + (
                            nextReset != null
                                    ? nextReset
                                    : "—"
                    )
            );
        } else {
            appendLine(builder, "Reset manager: unavailable");
        }

        appendLine(builder, "");

        appendSection(builder, "Configured Reset Worlds");
        List<ResetWorldSpec> specs = config.getResetWorlds();

        if (specs.isEmpty()) {
            appendLine(builder, "(none)");
        } else {
            for (ResetWorldSpec spec : specs) {
                appendLine(
                        builder,
                        "- " + spec.worldName() + " -> " + spec.biomeName()
                );
            }
        }

        appendLine(builder, "");

        appendSection(builder, "Chunky Configuration");
        appendLine(builder, "Chunky enabled: " + config.isChunkyEnabled());
        appendLine(builder, "Chunky installed: " + ChunkyIntegration.isAvailable());
        appendLine(builder, "Chunky radius: " + config.getChunkyRadius());
        appendLine(builder, "Chunky shape: " + config.getChunkyShape());
        appendLine(builder, "Chunky center spawn: " + config.isChunkyCenterSpawn());
        appendLine(builder, "Chunky silent: " + config.isChunkySilent());
        appendLine(builder, "Chunky delay seconds: " + config.getChunkyDelaySeconds());
        appendLine(builder, "");

        appendSection(builder, "Loaded Worlds");
        List<World> worlds = Bukkit.getWorlds();

        if (worlds.isEmpty()) {
            appendLine(builder, "(none)");
        } else {
            for (World world : worlds) {
                appendWorld(builder, world);
            }
        }

        appendLine(builder, "");

        appendSection(builder, "Raw config.yml");
        appendRawConfig(builder);

        return builder.toString();
    }

    private void appendWorld(
            StringBuilder builder,
            World world
    ) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();

        appendLine(builder, "- World: " + world.getName());
        appendLine(builder, "  Environment: " + world.getEnvironment());
        appendLine(builder, "  Difficulty: " + world.getDifficulty());
        appendLine(builder, "  PvP: " + world.getPVP());
        appendLine(builder, "  Players: " + world.getPlayers().size());
        appendLine(builder, "  Spawn: " + formatLocation(world.getSpawnLocation()));
        appendLine(builder, "  Border size: " + border.getSize());
        appendLine(
                builder,
                "  Border center: x="
                        + center.getBlockX()
                        + ", z="
                        + center.getBlockZ()
        );
    }

    private String formatLocation(Location location) {
        if (location == null) {
            return "null";
        }

        return "world="
                + (
                location.getWorld() != null
                        ? location.getWorld().getName()
                        : "null"
        )
                + ", x="
                + location.getBlockX()
                + ", y="
                + location.getBlockY()
                + ", z="
                + location.getBlockZ();
    }

    private void appendRawConfig(StringBuilder builder) throws IOException {
        Path configFile =
                plugin.getDataFolder()
                        .toPath()
                        .resolve("config.yml");

        if (!Files.exists(configFile)) {
            appendLine(builder, "(config.yml does not exist in plugin data folder)");
            return;
        }

        appendLine(builder, Files.readString(configFile, StandardCharsets.UTF_8));
    }

    private void appendSection(
            StringBuilder builder,
            String title
    ) {
        appendLine(builder, "============================================================");
        appendLine(builder, title);
        appendLine(builder, "============================================================");
    }

    private void appendLine(
            StringBuilder builder,
            String line
    ) {
        builder.append(line).append(System.lineSeparator());
    }

    private void handleReload(CommandSender sender) {
        if (!hasPermission(sender, "cpvpsinglebiome.reload")) {
            return;
        }

        try {
            config.reload();

            if (resetManager != null) {
                resetManager.reload();
            }

            sender.sendMessage(
                    config.getPrefix()
                            + "§aConfiguration reloaded successfully."
            );

        } catch (Throwable t) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cReload failed: §f"
                            + t.getMessage()
            );

            plugin.getLogger().warning(
                    "Reload failed: " + t
            );
        }
    }

    private void handleBiomes(CommandSender sender) {
        if (!hasPermission(sender, "cpvpsinglebiome.info")) {
            return;
        }

        sender.sendMessage(config.getPrefix() + "§fAvailable biomes:");

        for (BiomeType type : BiomeType.values()) {
            sender.sendMessage("  §5» §d" + type.getId());
        }
    }

    private void handleInfo(CommandSender sender) {
        if (!hasPermission(sender, "cpvpsinglebiome.info")) {
            return;
        }

        sender.sendMessage(HEADER);
        sender.sendMessage("§d  Version:          §f" + plugin.getDescription().getVersion());
        sender.sendMessage("§d  Default biome:    §f" + config.getDefaultBiome());
        sender.sendMessage("§d  Base height:      §f" + config.getBaseHeight());
        sender.sendMessage("§d  Height variation: §f" + config.getHeightVariation());
        sender.sendMessage("§d  Noise scale:      §f" + config.getNoiseScale());
        sender.sendMessage("§d  Flatness:         §f" + config.getFlatness());

        sender.sendMessage(
                "§d  World border:     §f"
                        + (
                        config.getWorldBorderSize() > 0.0D
                                ? config.getWorldBorderSize() + " blocks"
                                : "disabled"
                )
        );

        sender.sendMessage(
                "§d  Decorations:      §f"
                        + (
                        config.isDecorationEnabled()
                                ? "enabled (global density " + config.getDecorationDensity() + ")"
                                : "disabled"
                )
        );

        sender.sendMessage(
                "§d  Auto-reset:       §f"
                        + (
                        config.isResetEnabled()
                                ? "enabled @ " + config.getResetTime() + " " + config.getResetZone().getId()
                                : "disabled"
                )
        );

        sender.sendMessage("");
        sender.sendMessage("§5  Multiverse syntax: §f/mv create <world> normal -g CPVPSingleBiome:<biome>");
        sender.sendMessage(FOOTER);
    }

    private void handleCreate(
            CommandSender sender,
            String[] args
    ) {
        if (!hasPermission(sender, "cpvpsinglebiome.create")) {
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cUsage: §f/cpvpsb create <worldname> [biome]"
            );
            return;
        }

        String worldName = args[1];
        String biomeName =
                args.length >= 3
                        ? args[2]
                        : config.getDefaultBiome();

        BiomeType biomeType = BiomeType.fromString(biomeName);

        if (biomeType == null) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cUnknown biome: §f"
                            + biomeName
            );
            sender.sendMessage(
                    config.getPrefix()
                            + "§cAvailable biomes: §f"
                            + BiomeType.getNames()
            );
            return;
        }

        if (Bukkit.getWorld(worldName) != null) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cA loaded world named §f"
                            + worldName
                            + " §calready exists."
            );
            return;
        }

        sender.sendMessage(
                config.getPrefix()
                        + "§fCreating world §d"
                        + worldName
                        + " §fwith biome §d"
                        + biomeType.getId()
                        + "§f..."
        );

        World world;

        try {
            WorldCreator creator = new WorldCreator(worldName);

            creator.generator(
                    (ChunkGenerator) new SingleBiomeChunkGenerator(
                            config,
                            biomeType
                    )
            );

            creator.generateStructures(false);

            world = creator.createWorld();

        } catch (Throwable t) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cFailed to create world: §f"
                            + t.getMessage()
            );

            plugin.getLogger().severe(
                    "World creation failed for '"
                            + worldName
                            + "': "
                            + t
            );
            return;
        }

        if (world == null) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cFailed to create world §f"
                            + worldName
                            + "§c."
            );
            return;
        }

        WorldInitListener.applyArenaRules(world);
        applyConfiguredWorldSettings(world);

        double borderSize = config.getWorldBorderSize();

        if (borderSize > 0.0D) {
            world.getWorldBorder().setSize(borderSize);
            world.getWorldBorder().setCenter(world.getSpawnLocation());
        }

        sender.sendMessage(
                config.getPrefix()
                        + "§aWorld §f"
                        + worldName
                        + " §acreated with biome §f"
                        + biomeType.getId()
                        + "§a."
        );

        sender.sendMessage(
                config.getPrefix()
                        + "§7Applied world settings: §f"
                        + config.getResetDifficulty()
                        + "§7, §f"
                        + config.getResetGameMode()
                        + "§7, PvP=§f"
                        + config.isResetPvpEnabled()
                        + "§7."
        );

        sender.sendMessage(
                config.getPrefix()
                        + "§fUse §d/cpvpsb tp "
                        + worldName
                        + " §fto teleport there."
        );
    }

    private void handleTp(
            CommandSender sender,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cThis command can only be used by players."
            );
            return;
        }

        if (!hasPermission(player, "cpvpsinglebiome.tp")) {
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cUsage: §f/cpvpsb tp <worldname>"
            );
            return;
        }

        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cWorld §f"
                            + worldName
                            + " §cnot found or not loaded."
            );
            return;
        }

        player.teleport(world.getSpawnLocation());

        player.sendMessage(
                config.getPrefix()
                        + "§aTeleported to §f"
                        + worldName
                        + "§a."
        );
    }

    private void handleReset(
            CommandSender sender,
            String[] args
    ) {
        if (args.length < 2) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cUsage: §f/cpvpsb reset <now|status|reload|worldname>"
            );
            return;
        }

        if (resetManager == null) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cReset manager is not available."
            );
            return;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);

        switch (sub) {

            case "now":
                if (!hasPermission(sender, "cpvpsinglebiome.reset.now")) {
                    return;
                }

                if (resetManager.isResetInProgress()) {
                    sender.sendMessage(
                            config.getPrefix()
                                    + "§cA reset is already in progress."
                    );
                    return;
                }

                resetManager.resetAll(sender);
                return;

            case "status":
                if (!hasPermission(sender, "cpvpsinglebiome.reset")) {
                    return;
                }

                sendResetStatus(sender);
                return;

            case "reload":
                if (!hasPermission(sender, "cpvpsinglebiome.reload")) {
                    return;
                }

                try {
                    config.reload();
                    resetManager.reload();

                    sender.sendMessage(
                            config.getPrefix()
                                    + "§aReset configuration reloaded."
                    );

                } catch (Throwable t) {
                    sender.sendMessage(
                            config.getPrefix()
                                    + "§cReload failed: §f"
                                    + t.getMessage()
                    );
                }
                return;

            default:
                if (!hasPermission(sender, "cpvpsinglebiome.reset.now")) {
                    return;
                }

                if (resetManager.isResetInProgress()) {
                    sender.sendMessage(
                            config.getPrefix()
                                    + "§cA reset is already in progress."
                    );
                    return;
                }

                resetManager.resetWorldByName(args[1], sender);
        }
    }

    private void sendResetStatus(CommandSender sender) {
        sender.sendMessage(HEADER);

        sender.sendMessage(
                "§d  Auto-reset:      §f"
                        + (config.isResetEnabled() ? "enabled" : "disabled")
        );

        sender.sendMessage(
                "§d  Reset time:      §f"
                        + config.getResetTime()
                        + " §7("
                        + config.getResetZone().getId()
                        + ")"
        );

        sender.sendMessage(
                "§d  Interval:        §f"
                        + config.getResetIntervalDays()
                        + " day(s)"
        );

        sender.sendMessage(
                "§d  World settings:  §f"
                        + config.getResetDifficulty()
                        + "§7, §f"
                        + config.getResetGameMode()
                        + "§7, PvP=§f"
                        + config.isResetPvpEnabled()
        );

        LocalDate lastReset = resetManager.getLastAutoResetDate();

        sender.sendMessage(
                "§d  Last auto reset: §f"
                        + (
                        lastReset != null
                                ? lastReset.toString()
                                : "never"
                )
        );

        LocalDateTime next = resetManager.getNextResetAt();

        sender.sendMessage(
                "§d  Next auto reset: §f"
                        + (
                        next != null
                                ? next.toString()
                                : "—"
                )
        );

        sender.sendMessage("§d  Fallback world:  §f" + config.getFallbackWorld());
        sender.sendMessage("§d  Skip if online:  §f" + config.isSkipIfAnyPlayerOnline());

        sender.sendMessage(
                "§d  Warnings:        §f"
                        + (
                        config.isWarningEnabled()
                                ? config.getWarningMinutes() + " min"
                                : "disabled"
                )
        );

        sender.sendMessage("§d  Backup old:      §f" + config.isBackupOldWorlds());
        sender.sendMessage("§d  Delete backup:   §f" + config.isDeleteOldWorldsAfterSuccess());

        sender.sendMessage("§d  Configured worlds:");

        List<ResetWorldSpec> specs = config.getResetWorlds();

        if (specs.isEmpty()) {
            sender.sendMessage("    §7(none)");
        } else {
            for (ResetWorldSpec spec : specs) {
                sender.sendMessage(
                        "    §5» §d"
                                + spec.worldName()
                                + " §7("
                                + spec.biomeName()
                                + ")"
                );
            }
        }

        sender.sendMessage(
                "§d  Chunky:          §f"
                        + (
                        config.isChunkyEnabled()
                                ? "enabled"
                                : "disabled"
                )
                        + " §7(installed: "
                        + ChunkyIntegration.isAvailable()
                        + ")"
        );

        if (config.isChunkyEnabled()) {
            sender.sendMessage(
                    "    §7radius="
                            + config.getChunkyRadius()
                            + " shape="
                            + config.getChunkyShape()
                            + " center-spawn="
                            + config.isChunkyCenterSpawn()
                            + " silent="
                            + config.isChunkySilent()
                            + " delay="
                            + config.getChunkyDelaySeconds()
                            + "s"
            );
        }

        sender.sendMessage(FOOTER);
    }

    private void handleChunky(
            CommandSender sender,
            String[] args
    ) {
        if (!hasPermission(sender, "cpvpsinglebiome.chunky")) {
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(
                    config.getPrefix()
                            + "§cUsage: §f/cpvpsb chunky <start <world>|start-all>"
            );
            return;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);

        switch (sub) {

            case "start":
                if (args.length < 3) {
                    sender.sendMessage(
                            config.getPrefix()
                                    + "§cUsage: §f/cpvpsb chunky start <world>"
                    );
                    return;
                }

                World world = Bukkit.getWorld(args[2]);

                if (world == null) {
                    sender.sendMessage(
                            config.getPrefix()
                                    + "§cWorld §f"
                                    + args[2]
                                    + " §cnot found or not loaded."
                    );
                    return;
                }

                boolean ok = ChunkyIntegration.start(
                        (Plugin) plugin,
                        world,
                        config
                );

                sender.sendMessage(
                        config.getPrefix()
                                + (
                                ok
                                        ? "§aChunky started for §f" + world.getName() + "§a."
                                        : "§cChunky could not be started for §f" + world.getName() + "§c."
                        )
                );
                return;

            case "start-all":
                List<String> names = new ArrayList<>();

                for (ResetWorldSpec spec : config.getResetWorlds()) {
                    names.add(spec.worldName());
                }

                if (names.isEmpty()) {
                    sender.sendMessage(
                            config.getPrefix()
                                    + "§eNo reset worlds configured."
                    );
                    return;
                }

                if (!ChunkyIntegration.isAvailable()) {
                    sender.sendMessage(
                            config.getPrefix()
                                    + "§cChunky is not installed."
                    );
                    return;
                }

                ChunkyIntegration.startAll(
                        (Plugin) plugin,
                        names,
                        config
                );

                sender.sendMessage(
                        config.getPrefix()
                                + "§aChunky scheduled for §f"
                                + names.size()
                                + " §aworld(s)."
                );
                return;

            default:
                sender.sendMessage(
                        config.getPrefix()
                                + "§cUnknown chunky subcommand. Use §fstart §cor §fstart-all§c."
                );
        }
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
        );
    }

    private void applyMultiverseWorldSettings(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return;
        }

        Plugin multiverse =
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

    private boolean hasPermission(
            CommandSender sender,
            String permission
    ) {
        if (sender.hasPermission("cpvpsinglebiome.admin")
                || sender.hasPermission(permission)) {
            return true;
        }

        sender.sendMessage(
                config.getPrefix()
                        + "§cYou don't have permission to do that."
        );

        return false;
    }
}
