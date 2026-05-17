package me.liass.cpvpsinglebiome.command;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import me.liass.cpvpsinglebiome.CPVPSingleBiomePlugin;
import me.liass.cpvpsinglebiome.chunky.ChunkyIntegration;
import me.liass.cpvpsinglebiome.config.ConfigManager;
import me.liass.cpvpsinglebiome.generator.BiomeType;
import me.liass.cpvpsinglebiome.generator.SingleBiomeChunkGenerator;
import me.liass.cpvpsinglebiome.listener.WorldInitListener;
import me.liass.cpvpsinglebiome.reset.ResetManager;
import me.liass.cpvpsinglebiome.reset.ResetWorldSpec;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
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

        switch (args[0].toLowerCase()) {

            case "help":
                sendHelp(sender);
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
        sender.sendMessage("§d  /cpvpsb reload                 §f» Reload config.yml");
        sender.sendMessage("§d  /cpvpsb biomes                 §f» List available biomes");
        sender.sendMessage("§d  /cpvpsb info                   §f» Show plugin info");
        sender.sendMessage("§d  /cpvpsb create <world> [biome] §f» Create a single-biome world");
        sender.sendMessage("§d  /cpvpsb tp <world>             §f» Teleport to a world");
        sender.sendMessage("§d  /cpvpsb reset now              §f» Reset all configured arenas now");
        sender.sendMessage("§d  /cpvpsb reset <world>          §f» Reset one configured arena");
        sender.sendMessage("§d  /cpvpsb reset status           §f» Show reset/chunky configuration");
        sender.sendMessage("§d  /cpvpsb reset reload           §f» Reload reset scheduler/config");
        sender.sendMessage("§d  /cpvpsb chunky start <world>   §f» Start Chunky for one world");
        sender.sendMessage("§d  /cpvpsb chunky start-all       §f» Start Chunky for all reset worlds");
        sender.sendMessage("");
        sender.sendMessage("§5  Available biomes: §f" + BiomeType.getNames());
        sender.sendMessage("§5  Multiverse syntax: §f/mv create <world> normal -g CPVPSingleBiome:<biome>");
        sender.sendMessage(FOOTER);
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

        String sub = args[1].toLowerCase();

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

        String sub = args[1].toLowerCase();

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
